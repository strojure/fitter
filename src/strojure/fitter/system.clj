(ns strojure.fitter.system
  "Defines system of dependent components to start/stop them."
  (:require [strojure.fitter.component :as component])
  (:import (clojure.lang IDeref IFn ILookup IPersistentMap MapEntry)
           (java.io Closeable)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol SystemAtom
  "An “atom” with map of running system components."

  (start!
    [system-atom]
    [system-atom, {:keys [filter-keys] :as opts}]
    "Starts not running system components, all registered or selected by
    optional predicate function `filter-keys`. Returns `system-atom`.")

  (stop!
    [system-atom]
    [system-atom, {:keys [filter-keys] :as opts}]
    "Stops started system components, all keys in the registry or selected by optional
    predicate function `filter-keys`. Returns `system-atom`.")

  (inspect
    [system-atom]
    "Returns arbitrary data about system map internals."))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- expand-deps
  [deps k]
  (if-let [k-deps (deps k)]
    (loop [k-deps k-deps]
      (let [prev-c (count k-deps)
            k-deps (into k-deps (comp (keep (fn [e] (when (k-deps (key e)) (val e))))
                                      cat)
                         deps)]
        (if (= (count k-deps) prev-c)
          (assoc deps k k-deps)
          (recur k-deps))))
    deps))

(defn- update-deps
  [deps k dk]
  (cond-> deps
    (not (contains? (deps k) dk)) (-> (update k (fnil conj #{}) dk)
                                      (expand-deps k))))

(defn- component-system
  "Particular map-like view provided as argument in `::component/start`. Tracks
  dependencies and evaluates required components on demand."
  [component-key delays! deps!]
  (let [force-inst (fn [inst k]
                     (when (-> (swap! deps! update-deps component-key k)
                               (get component-key)
                               (contains? component-key))
                       (throw (ex-info (str "Cyclic dependencies: " (pr-str component-key)
                                            " -> " (@deps! component-key))
                                       {:type ::cyclic-dependencies
                                        ::key component-key
                                        ::deps @deps!})))
                     (force inst))
        lookup-fn (fn
                    ([k] (some-> (@delays! k) (force-inst k)))
                    ([k not-found]
                     (if-let [inst (@delays! k)]
                       (force-inst inst k)
                       not-found)))]
    (reify
      ILookup
      (valAt [_ k] (lookup-fn k))
      (valAt [_ k not-found] (lookup-fn k not-found))
      IFn
      (invoke [_ k] (lookup-fn k))
      (invoke [_ k not-found] (lookup-fn k not-found))
      IPersistentMap
      (seq [_] (keep (fn [[k inst]] (when (realized? inst)
                                      [k (force-inst inst k)]))
                     @delays!))
      (containsKey [_ k] (if-let [inst (@delays! k)]
                           (force-inst inst k)
                           false))
      (entryAt [_ k] (when-let [inst (@delays! k)]
                       (MapEntry. k (force-inst inst k)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init-system
  "Returns [[SystemAtom]] implementation for the `registry` of components.
  Optional function `(fn wrap-component [component key] wrapped-component)`
  allows to add additional functionality around component methods for logging,
  exception handling etc. The returned instance is `with-open`-friendly."
  ^Closeable
  [registry {:keys [wrap-component] :as opts}]
  (let [deps! (atom {})
        delays! (atom nil)
        snapshot! (atom nil)
        wrapped (fn wrapped-component [k]
                  (cond-> (registry k) wrap-component (wrap-component k)))
        start-delay
        (fn start-delay [k]
          (delay (try
                   (swap! deps! dissoc k)
                   (component/start (wrapped k)
                                    (component-system k delays! deps!))
                   (catch Throwable e
                     (swap! deps! dissoc k)
                     (swap! delays! assoc k (start-delay k))
                     (throw (->> e (ex-info (str "Component start failure: " k)
                                            {:type ::start-component-failure
                                             ::key k}))))
                   (finally
                     (reset! snapshot! nil)))))]
    (reset! delays! (->> (keys registry)
                         (into {} (map (fn [k] [k (start-delay k)])))))
    (reify
      SystemAtom
      (start! [this] (start! this nil))
      (start! [this {:keys [filter-keys]}]
        (try
          (->> (cond->> (keys registry) filter-keys (filter filter-keys))
               (run! (fn start-key [k]
                       (force (@delays! k)))))
          (catch Throwable e
            (throw (->> e (ex-info "System start failure" {:type ::system-start-failure
                                                           ::system this})))))
        this)

      (stop! [this] (stop! this nil))
      (stop! [this {:keys [filter-keys]}]
        (->> (cond->> (keys @delays!) filter-keys (filter filter-keys))
             (run! (fn stop-key [k]
                     (let [inst (@delays! k)]
                       (when (realized? inst)
                         (->> @deps! (keep (fn [[dk deps]] (when (deps k) dk)))
                              (run! stop-key))
                         (when-let [stop-fn (component/stop-fn (wrapped k))]
                           (try (stop-fn @inst)
                                (catch Throwable _)))
                         (swap! deps! dissoc k)
                         (swap! delays! assoc k (start-delay k)))))))
        (reset! snapshot! nil)
        this)

      (inspect [this]
        {:registry registry :opts opts :deps @deps! :delays @delays! :system (deref this)})

      IDeref
      (deref [_]
        (or (.deref ^IDeref snapshot!)
            (reset! snapshot! (into {} (keep (fn [[k inst]] (when (realized? inst)
                                                              (MapEntry. k (deref inst)))))
                                    @delays!))))
      Closeable
      (close [this] (stop! this)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
