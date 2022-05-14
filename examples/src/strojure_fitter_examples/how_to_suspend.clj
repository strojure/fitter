(ns strojure-fitter-examples.how-to-suspend
  "Example of suspending components."
  (:require [strojure.fitter.component :as component]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private simple-component
  {::component/start (fn [_]
                       (println "Start" 'simple-component)
                       {:simple-component nil})

   ::component/stop! (fn [instance]
                       (println "Stop" 'simple-component instance))})

(def ^:private suspendable-component
  {::component/start (fn [{:system/keys [simple]}]
                       (println "Start" 'suspendable-component simple)
                       {:suspending-with-stop simple})

   ::component/stop! (fn [instance]
                       (println "Stop" 'suspendable-component instance))

   ::component/suspend! (fn [instance old-system]
                          (println "Suspend" 'suspendable-component)
                          (fn resume [{:system/keys [simple]}]
                            (println "Resume" 'suspendable-component simple)
                            {:suspending-with-stop simple
                             :resumed {:old-instance instance
                                       :old-simple (:simple old-system)}}))})

(def ^:private registry
  {:system/simple simple-component
   :system/suspending suspendable-component})

(def ^:private my-system!
  (system/init {:registry registry}))

(comment
  (-> (system/inspect my-system!)
      (select-keys [:suspended :deps :system]))
  (system/start! my-system!)
  (system/stop! my-system! {:suspend true})
  (system/stop! my-system! {:suspend true :filter-keys #{:system/simple}})
  (system/start! my-system! {:registry (dissoc registry :system/suspending)})
  (system/start! my-system! {:filter-keys #{:system/suspending}})
  (system/stop! my-system!)
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
