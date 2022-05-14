(ns strojure.fitter.mount
  "“Mounting” components like in <https://github.com/tolitius/mount>."
  (:require [strojure.fitter.component :as component]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- resolve-sym
  [sym]
  (or (resolve sym)
      (throw (ex-info (str "Unresolved var for symbol: " sym) {:type ::error}))))

(defn- mount-instance
  "Mounts instance to var, returns nil."
  [instance, v]
  ;; TODO: mount over mounted - should we care?
  (let [original (or (::original (meta v))
                     (deref v))]
    (doto v
      (alter-var-root (constantly instance))
      (alter-meta! assoc ::original original)))
  nil)

(defn- restore-var
  [v]
  (when-let [original (-> v meta ::original)]
    (doto v
      (alter-var-root (constantly original))
      (alter-meta! dissoc ::original)))
  nil)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn component
  "Returns component mounting started instance in existing var using var’s meta
  as component."
  [sym]
  (reify component/Component
    (start [_ system]
      (let [v (resolve-sym sym)
            start-fn (::component/start (meta v))]
        (when-not start-fn
          (throw (ex-info (str "Symbol is not a component: " sym)
                          {:type ::error :meta (meta v)})))
        (doto (start-fn system)
          (mount-instance v))))

    (stop-fn [_]
      (let [v (resolve-sym sym)
            stop-fn (::component/stop! (meta v))]
        (fn [instance]
          (restore-var v)
          (when stop-fn (stop-fn instance)))))

    (suspend-fn [_]
      (let [v (resolve-sym sym)]
        (when-let [suspend-fn (::component/suspend! (meta v))]
          (fn suspend! [instance old-system]
            (when-let [resume-fn (suspend-fn instance old-system)]
              (restore-var v)
              (fn resume [new-system]
                (doto (resume-fn new-system)
                  (mount-instance v))))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
