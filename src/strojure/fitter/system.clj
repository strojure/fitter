(ns strojure.fitter.system
  "Defines system of dependent components to start/stop them."
  (:require [clojure.set :as set]
            [strojure.fitter.component :as component])
  (:import (clojure.lang IDeref IFn ILookup IPersistentMap MapEntry)
           (java.io Closeable)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol SystemState
  "Stateful container of running system components."

  (start!
    [state]
    [state {:keys [registry, filter-keys, parallel]}]
    "Starts not running system components and returns map of running instances.

    **`registry`** The optional new registry of system components to start and
    to use in subsequent start/stop calls. If registry differs from the previous
    one then removed keys stop.

    **`filter-keys`** The optional predicate function to filter starting keys.
    If not specified then all keys from the registry are starting.

    **`parallel`** If `true` then components starts (possibly) in parallel.
    ")

  (stop!
    [state]
    [state {:keys [filter-keys, suspend, parallel]}]
    "Stops started system components, all keys in the registry or selected by optional
    predicate function `filter-keys`. Suspends suspendable components if
    `suspend` is true. Returns result map of running instances.

    **`filter-keys`** The optional predicate function to filter stopping keys.
    If not specified then all keys from the registry are stopping.

    **`suspend`** If true then suspendable components not stopped but suspended.
    Suspended instances resume in next [[start!]] when they are required.

    **`parallel`** If `true` then components stops in parallel.
    ")

  (inspect
    [state]
    "Returns arbitrary data about system state internals."))

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
  (let [track-deps (partial swap! deps! update-deps component-key)
        force-inst (fn [inst _k]
                     (when (-> (@deps! component-key)
                               (contains? component-key))
                       (throw (ex-info (str "Cyclic dependencies: " (pr-str component-key)
                                            " -> " (@deps! component-key))
                                       {:type ::cyclic-dependencies
                                        ::key component-key, ::deps @deps!})))
                     (force inst))
        lookup-fn (fn
                    ([k]
                     (track-deps k)
                     (some-> (@delays! k) (force-inst k)))
                    ([k not-found]
                     (track-deps k)
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
      ;; Specify IPersistentMap below to satisfy `(map? system)` in `spec/keys`.
      IPersistentMap
      (seq [_] (keep (fn [[k inst]] (when (realized? inst)
                                      (track-deps k)
                                      [k (force-inst inst k)]))
                     @delays!))
      (containsKey [_ k]
        (track-deps k)
        (if-let [inst (@delays! k)]
          (force-inst inst k)
          false))
      (entryAt [_ k]
        (track-deps k)
        (when-let [inst (@delays! k)]
          (MapEntry. k (force-inst inst k)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init
  "Returns [[SystemState]] implementation. The state is instance of
  `java.io.Closeable` and can be used in `with-open` to stop system
  automatically.

  **`registry`** The optional initial registry of components to use in
  subsequent start/stop calls.

  **`parallel`** If `true` then subsequent start/stop calls execute components
  by default in parallel.

  **`wrap-component`** Deprecated, to be removed soon.
  "
  {:arglists '([]
               [{:keys [registry, wrap-component, parallel] :as options}])
   :tag Closeable}
  ([] (init {}))
  ([{:keys [registry, wrap-component] :as opts}]
   (let [registry! (atom (or registry {}))
         deps! (atom {})
         delays! (atom {})
         snapshot! (atom nil)
         suspended! (atom {})
         wrapped (fn wrapped-component [k]
                   (cond-> (@registry! k) wrap-component (wrap-component k)))
         start-delay
         (fn start-delay [k]
           (delay (try
                    (swap! deps! dissoc k)
                    (let [system (component-system k delays! deps!)]
                      (if-let [{:keys [resume-fn]} (@suspended! k)]
                        (do (swap! suspended! dissoc k)
                            (resume-fn system))
                        (component/start (wrapped k) system)))
                    (catch Throwable e
                      (swap! deps! dissoc k)
                      (swap! delays! assoc k (start-delay k))
                      (throw (->> e (ex-info (str "Component start failure: " k)
                                             {:type ::component-start-failure, ::key k}))))
                    (finally
                      (reset! snapshot! nil)))))]
     (reset! delays!
             (->> (keys @registry!)
                  (into {} (map (fn [k] [k (start-delay k)])))))
     (reify
       SystemState
       (start! [this] (start! this nil))
       (start! [this {:keys [registry, filter-keys, parallel] :or {parallel (:parallel opts)}}]
         (when registry
           (let [old-ks (set (keys @registry!))
                 new-ks (set (keys registry))]
             (when-let [removed (not-empty (set/difference old-ks new-ks))]
               (stop! this {:filter-keys removed})
               (swap! delays! #(apply dissoc % removed)))
             (reset! registry! registry)
             (when-let [added (not-empty (set/difference new-ks old-ks))]
               ;; Ensure that all dependent components stop.
               ;; This will also init delays for added keys.
               (stop! this {:filter-keys added}))))
         (try
           (->> (keys @registry!)
                (into [] (apply comp (cond-> []
                                       filter-keys (conj (filter filter-keys))
                                       :instances, (conj (map @delays!))
                                       parallel,,, (conj (map (fn [inst] (future @inst)))))))
                (run! deref))
           (catch Throwable e
             (throw (->> e (ex-info "System start failure" {:type ::system-start-failure, ::state this})))))
         (deref this))

       (stop! [this] (stop! this nil))
       (stop! [this {:keys [filter-keys suspend parallel] :or {parallel (:parallel opts)}}]
         (let [old-system (deref this)
               future-stops! (atom [])]
           (->> (cond->> (keys @registry!) filter-keys (filter filter-keys))
                (run! (fn stop-key [k]
                        ;; Run over dependent keys even if they are not started
                        ;; because keys can emerge on registry changes.
                        (->> @deps! (keep (fn [[dk deps]] (when (deps k) dk)))
                             (run! stop-key))
                        (let [inst (@delays! k)
                              inst (or (when (some-> inst realized?) inst)
                                       (and (not suspend) (:inst (@suspended! k))))
                              stop-fn (and inst (component/stop-fn (wrapped k)))
                              resume-fn (when-let [suspend-fn (and inst suspend (component/suspend-fn (wrapped k)))]
                                          (try (suspend-fn @inst old-system)
                                               (catch Throwable _)))]
                          (cond
                            ;; TODO: Should we keep current deps in suspended?
                            resume-fn (swap! suspended! assoc k {:inst inst
                                                                 :resume-fn resume-fn})
                            stop-fn (do (swap! suspended! dissoc k)
                                        (if parallel
                                          (swap! future-stops! conj
                                                 (future (try (stop-fn @inst) (catch Throwable _))))
                                          (try (stop-fn @inst) (catch Throwable _)))))
                          (swap! deps! dissoc k)
                          (swap! delays! assoc k (start-delay k))))))
           (run! deref @future-stops!))
         (reset! snapshot! nil)
         (deref this))

       (inspect [this]
         {:opts (dissoc opts :registry) :registry @registry! :delays @delays!
          :suspended @suspended! :deps @deps! :system (deref this)})

       IDeref
       (deref [_]
         (or (.deref ^IDeref snapshot!)
             (reset! snapshot! (into {} (keep (fn [[k inst]] (when (realized? inst)
                                                               (MapEntry. k (deref inst)))))
                                     @delays!))))
       Closeable
       (close [this] (stop! this) nil)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
