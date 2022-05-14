(ns strojure-fitter-examples.how-to-mount
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

(def ^{::component/start (fn [system]
                           (println "Start" 'test-suspend)
                           {:test-suspend (:system/x system)})
       ::component/stop! (fn [inst]
                           (println "Stop" 'test-suspend inst))
       ::component/suspend! (fn [inst _old-system]
                              (println "Suspend" 'test-suspend inst)
                              (fn [system]
                                (println "Resume" 'test-suspend)
                                {:test-suspend (:system/x system) :resumed true}))}
  test-suspend
  "The value which can suspend. It is unmounted when suspended."
  {:test-suspend :default})

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
   `test-suspend (mount/component `test-suspend)
   `test-declare (mount/component `test-declare)})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- print-status
  []
  (println "Status -" (test-function) test-value test-suspend (test-deps)
           (try (test-declare) (catch Throwable e (ex-message e)))))

(comment
  (do (print-status)
      (with-open [system! (system/init)]
        (system/start! system! {:registry registry})
        (print-status)
        (system/stop! system! {:suspend true})
        (print-status)
        (system/start! system!)
        (print-status))
      (print-status))
  ;Status - {:test-function :default} {:test-value :default} {:test-suspend :default} {:test-deps :default} Attempting to call unbound fn: #'strojure-fitter-examples.how-to-mount/test-declare
  ;Start test-function
  ;Start test-deps
  ;Start test-value
  ;Start test-suspend
  ;Start test-declare
  ;Status - {:test-function :x} {:test-value :x} {:test-suspend :x} {:test-deps {:test-value :x}} {:test-declare :x}
  ;Stop test-value {:test-value :x}
  ;Suspend test-suspend {:test-suspend :x}
  ;Status - {:test-function :default} {:test-value :default} {:test-suspend :default} {:test-deps :default} Attempting to call unbound fn: #'strojure-fitter-examples.how-to-mount/test-declare
  ;Start test-function
  ;Start test-deps
  ;Start test-value
  ;Resume test-suspend
  ;Start test-declare
  ;Status - {:test-function :x} {:test-value :x} {:test-suspend :x, :resumed true} {:test-deps {:test-value :x}} {:test-declare :x}
  ;Stop test-value {:test-value :x}
  ;Stop test-suspend {:test-suspend :x, :resumed true}
  ;Status - {:test-function :default} {:test-value :default} {:test-suspend :default} {:test-deps :default} Attempting to call unbound fn: #'strojure-fitter-examples.how-to-mount/test-declare
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
