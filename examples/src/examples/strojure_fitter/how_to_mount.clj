(ns examples.strojure-fitter.how-to-mount
  "Example of using mounted components."
  (:require [strojure.fitter.component :as component]
            [strojure.fitter.mount :as mount]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn test-function
  "The function which can be used without system start."
  {::component/start (fn [system]
                       (println "Start" 'test-function)
                       (constantly {:test-function (:system/x system)}))}
  []
  {:test-function :default})

(def ^{::component/start (fn [system]
                           (println "Start" 'test-value)
                           {:test-value (:system/x system)})
       ::component/stop! (fn [inst]
                           (println "Stop" 'test-value inst))}
  test-value
  "The value which can be used without system start."
  {:test-value :default})

(defn test-deps
  "The function which depends on other mounted component."
  {::component/start (fn [system]
                       (println "Start" 'test-deps)
                       (constantly {:test-deps (system `test-value)}))}
  []
  {:test-deps :default})

(declare ^{:doc "Declared but not initialized value which cannot be used without system start."
           ::component/start (fn [system]
                               (println "Start" 'test-declare)
                               (constantly {:test-declare (:system/x system)}))}
         test-declare)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private registry
  {:system/x (constantly :x)
   `test-function (mount/component `test-function)
   `test-deps (mount/component `test-deps)
   `test-value (mount/component `test-value)
   `test-declare (mount/component `test-declare)})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- print-status
  []
  (println "Status -" (test-function) test-value (test-deps)
           (try (test-declare) (catch Throwable e (ex-message e)))))

(comment
  (do (print-status)
      (with-open [system! (system/system-atom)]
        (system/start! system! {:registry registry})
        (print-status))
      (print-status))
  ;Status - {:test-function :default} {:test-value :default} Attempting to call unbound fn: #'examples.strojure-fitter.mount/test-declare
  ;Start test-function
  ;Start test-value
  ;Start test-declare
  ;Status - {:test-function :x} {:test-value :x} {:test-declare :x}
  ;Stop test-value {:test-value :x}
  ;Status - {:test-function :default} {:test-value :default} Attempting to call unbound fn: #'examples.strojure-fitter.mount/test-declare
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
