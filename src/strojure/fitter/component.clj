(ns strojure.fitter.component
  "Defines system component with lifecycle (start, stop etc.) functions."
  (:import (clojure.lang AFunction IPersistentMap)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol Component
  "System component lifecycle functions."

  (start
    [component system]
    "Returns component instance started in the system context. The `system` is
    a map of other component instances which this component depends on.")

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; Persistent map  as component:
;; {::start start-fn, ::stop! stop-fn, ::suspend! suspend-fn}
(extend-type IPersistentMap Component
  (start [this system]
    (if-let [f (this ::start)]
      (f system)
      (throw (ex-info "Start function is not defined for the component" {}))))
  (stop-fn [this]
    (this ::stop!))
  (suspend-fn [this]
    (this ::suspend!)))

;; Function as component's `start`, other methods in meta.
(extend-type AFunction Component
  (start [this system]
    (this system))
  (stop-fn [this]
    (-> this meta ::stop!))
  (suspend-fn [this]
    (-> this meta ::suspend!)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn bundle
  "Returns component as persistent map from provided functions."
  ([start-fn, stop!]
   {::start start-fn, ::stop! stop!})
  ([start-fn, stop!, suspend!]
   {::start start-fn, ::stop! stop!, ::suspend! suspend!}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
