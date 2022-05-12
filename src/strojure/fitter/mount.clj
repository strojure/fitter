(ns strojure.fitter.mount
  "“Mounting” components like in <https://github.com/tolitius/mount>."
  (:require [strojure.fitter.component :as component]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- resolve-sym
  [sym]
  (or (resolve sym)
      (throw (ex-info (str "Unresolved var for symbol: " sym) {:type ::error}))))

(defn- mount-var
  [v, new-val]
  ;; TODO: mount over mounted - should we care?
  (let [original (or (::original (meta v))
                     (deref v))]
    (doto v
      (alter-var-root (constantly new-val))
      (alter-meta! assoc ::original original))))

(defn- restore-var
  [v]
  (when-let [original (-> v meta ::original)]
    (doto v
      (alter-var-root (constantly original))
      (alter-meta! dissoc ::original))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn component
  "Returns component mounting started instance in existing var using var’s meta
  as component."
  [sym]
  {::component/start (fn start [system]
                       (let [v (resolve-sym sym)
                             start (::component/start (meta v))
                             _ (when-not start
                                 (throw (ex-info (str "Symbol is not a component: " sym)
                                                 {:type ::error :meta (meta v)})))
                             instance (start system)]
                         (mount-var v instance)
                         instance))
   ::component/stop! (fn stop! [instance]
                       (let [v (resolve-sym sym)]
                         (try
                           (when-let [stop (::component/stop! (meta v))]
                             (stop instance))
                           (finally
                             (restore-var v)))))})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
