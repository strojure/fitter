(ns readme.component
  (:require [strojure.fitter.component :as component]))

(def function-component
  "Simple function describes component start behaviour."
  (fn [{:keys [another-component]}]
    (comment "Use" another-component)
    :instance))

(def constant-component
  "Just constant component."
  (constantly true))

(def map-component
  "Component described as hash map with required `::component/start` key."
  {::component/start (constantly :instance)
   ::component/stop! (fn stop! [instance]
                       (comment "Destroy" instance))
   ::component/suspend! (fn suspend! [old-instance old-system]
                          (comment "Suspend" old-instance old-system)
                          (fn resume [new-system]
                            (comment "Resume" old-instance new-system)
                            :instance))})

(def assembled-component
  "Same map as above created using `component/of`."
  (component/of (constantly :instance)
                (fn stop! [instance]
                  (comment "Destroy" instance))
                (fn suspend! [old-instance old-system]
                  (comment "Suspend" old-instance old-system)
                  (fn resume [new-system]
                    (comment "Resume" old-instance new-system)
                    :instance))))
