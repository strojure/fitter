(ns example.how-to-reuse-components
  "Example how to reuse components for different system keys names."
  (:require [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- reusable-component
  [param-key]
  (fn [system]
    {::reusable (get system param-key)}))

(def ^:private registry
  {::a (constantly ::a)
   ::b (constantly ::b)
   ::ra (reusable-component ::a)
   ::rb (reusable-component ::b)})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private system!
  (system/init {:registry registry}))

(comment
  (system/start! system!)
  (system/stop! system!)
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
