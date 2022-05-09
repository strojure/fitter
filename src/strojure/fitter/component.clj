(ns strojure.fitter.component
  "Defines system component with lifecycle (start, stop etc.) functions."
  (:import (clojure.lang IPersistentMap AFunction Var)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol Component
  "System component lifecycle functions."
  (start [component system]
    "Returns component instance started in the system context.")
  (stop! [component instance]
    "Stops component instance."))

(extend-protocol Component
  ;;; Persistent map as component.
  IPersistentMap
  (start [this sys]
    (if-let [f (this ::start)]
      (f sys)
      (throw (ex-info "Start function not defined in the component map" {}))))
  (stop! [this inst]
    (when-let [f (this ::stop!)]
      (f inst)))
  ;;; Function as component :start, other methods in meta.
  AFunction
  (start [this sys]
    (this sys))
  (stop! [this inst]
    (when-let [f (-> this meta ::stop!)]
      (f inst)))
  ;;; Var as component :start, other methods in meta.
  Var
  (start [this sys]
    (this sys))
  (stop! [this inst]
    (when-let [f (-> this meta ::stop!)]
      (f inst))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
