(ns strojure.fitter.component
  "Defines system component with lifecycle (start, stop etc.) functions."
  (:import (clojure.lang AFunction IPersistentMap)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol Component
  "System component lifecycle functions."

  (start-fn
    [component]
    "Returns function to start component instance. The returned function
    receives a map of other component instances which this component depends on,
    and returns started instance.

        (fn start [system] instance)

    ")

  (stop-fn
    [component]
    "Returns function to stop component instance, or `nil` if component does not
    need to be stopped:

        (fn stop! [instance] (.stop instance))

    ")

  (suspend-fn
    [component]
    "Returns function to suspend component instance, or `nil` if component is
    not suspendable:

        (fn suspend! [instance old-system]
          ;; Return function to resume component instance with new system.
          (fn resume [new-system]
            ;; Return resumed or restarted instance using old and new systems.
            resumed-instance))

    "))

(defn start
  "Returns component instance started in the system context. The `system` is a
  map of other component instances which this component depends on."
  [component system]
  (if-let [f (start-fn component)]
    (f system)
    (throw (ex-info "Start function is not defined for the component" {::component component}))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; Persistent map  as component:
;; {::start start-fn, ::stop! stop-fn, ::suspend! suspend-fn}
(extend-type IPersistentMap Component
  (start-fn [this] (this ::start))
  (stop-fn [this] (this ::stop!))
  (suspend-fn [this] (this ::suspend!)))

;; Function as component's `start`, other methods in meta.
(extend-type AFunction Component
  (start-fn [this] this)
  (stop-fn [this] (-> this meta ::stop!))
  (suspend-fn [this] (-> this meta ::suspend!)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

#_{:clj-kondo/ignore [:shadowed-var]}

(defn of
  "Returns component as persistent map assembled from provided functions."
  ([start, stop!]
   {::start start, ::stop! stop!})
  ([start, stop!, suspend!]
   {::start start, ::stop! stop!, ::suspend! suspend!}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
