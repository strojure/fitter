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
    "Returns function `(fn stop! [inst] (stop inst))` which can stop this
    component instance. Used when caller wants to check if [[stop!]] is defined
    for the component."))

(defn stop!
  "Stops component instance. Does nothing if stop function is not defined."
  [component instance]
  (when-let [f (stop-fn component)]
    (f instance)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; Persistent map {::start start-fn, ::stop! stop-fn} as component.
(extend-type IPersistentMap Component
  (start [this sys] (if-let [f (this ::start)]
                      (f sys)
                      (throw (ex-info "Start function is not defined for the component" {}))))
  (stop-fn [this] (this ::stop!)))

;; Function as component's `start`, other methods in meta.
(extend-type AFunction Component
  (start [this sys] (this sys))
  (stop-fn [this] (-> this meta ::stop!)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
