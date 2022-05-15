(ns strojure-fitter-examples.how-to-log
  "Example how to add custom behaviour around components in the registry like
  logging or exception handling."
  (:require [strojure.fitter.component :as component]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private log-info println)
(def ^:private log-error println)

(defn- wrap-component
  [[k c]]
  [k (component/bundle
       (fn wrapped-start [system]
         (log-info "Start" k)
         (try (component/start c system)
              (catch Throwable e
                (log-error "Start" k "-> Exception:" (ex-message e))
                (throw e))))

       (when-let [stop-fn (component/stop-fn c)]
         (fn wrapped-stop! [instance]
           (log-info "Stop" k instance)
           (try (stop-fn instance)
                (catch Throwable e
                  (println "Stopped" k "-> Exception:" (ex-message e))))))

       (when-let [suspend-fn (component/suspend-fn c)]
         (fn wrapped-suspend! [instance old-system]
           (println "Suspend" k instance)
           (when-let [resume-fn (suspend-fn instance old-system)]
             (fn wrapped-resume [new-system]
               (println "Resume" k)
               (resume-fn new-system))))))])

(def ^:private registry
  (into {} (map wrap-component)
        {:a (component/bundle (fn [{:keys [b c]}] {:a/inst [b c]})
                              (fn [inst] {:stopped inst}))

         :b (component/bundle (fn [_] {:b/inst :_})
                              (fn [inst] {:stopped inst})
                              (fn [instance _old-system]
                                (fn resume [_new-system]
                                  (assoc instance :b/resume true))))

         :c (component/bundle (fn [system] {:c/inst (system :b)} #_(throw (Exception. "OOPS")))
                              (fn [inst] {:stopped inst} #_(throw (Exception. "Failure"))))}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private system!
  (system/init {:registry registry}))

(comment
  (system/start! system!)
  (system/stop! system! {:suspend true})
  (system/stop! system!)
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
