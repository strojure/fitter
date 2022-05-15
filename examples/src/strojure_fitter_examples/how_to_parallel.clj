(ns strojure-fitter-examples.how-to-parallel
  "Example of parallel start/stop of a system.

  NOTE: Only explicitly starting/stopping components execute in parallel. But
  components can additionally work around this in their start/stop functions.
  "
  (:require [strojure.fitter.component :as component]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- print-log [s] (print s) (flush))

(defn- component
  [id millis & deps]
  (component/bundle
    (fn [system]
      (let [state (keys (select-keys system deps))]
        (print-log (str "Start " id "\n"))
        (Thread/sleep millis)
        #_(print-log (str "Start " id " - DONE\n"))
        {(keyword "inst" (name id)) state}))
    (fn [instance]
      (print-log (str "Stop " id " - " instance "\n"))
      (Thread/sleep millis)
      (print-log (str "Stop " id " - " instance " - DONE\n"))
      id)))

(def ^:private registry
  {:a (component :a 1000)
   :b (component :b 500)
   :c (component :c 100 :a :b)
   :d (component :d 100 :b :a)})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private system!
  (system/init {:registry registry :parallel true}))

(comment
  (system/inspect system!)
  (time (system/start! system!))
  (time (system/start! system! {:parallel false}))
  (time (system/start! system! {:filter-keys #{:c :d}}))
  (time (system/start! system! {:filter-keys #{:c}}))
  (time (system/stop! system!))
  (time (system/stop! system! {:parallel false}))
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
