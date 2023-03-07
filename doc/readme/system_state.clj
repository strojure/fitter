(ns readme.system-state
  (:require [strojure.fitter.system :as system]))

(def ^:private registry
  {::a (constantly ::a)
   ::b (fn [{::keys [a]}] {::b a})})

;; Initialize system state.
(defonce ^:private system!
  (system/init {:registry registry}))

;; Start all system keys.
(system/start! system!)

;; Stop all running keys.
(system/stop! system!)

;; The `start!`, `stop!` and `deref` return the actual system map.
(let [{::keys [a b]} (system/start! system!)]
  (comment "Work with" a b))
(let [_ (system/start! system!)
      {::keys [a b]} (deref system!)]
  (comment "Work with" a b))

;; Start/stop only specific keys.
(system/start! system! {:filter-keys #{::a}})
(system/stop! system! {:filter-keys #{::a}})

;; Start/stop system incrementally.
(doto system! (system/start! {:filter-keys #{:a}})
              (system/start! {:filter-keys (complement #{:a})}))
(doto system! (system/stop! {:filter-keys (complement #{:a})})
              (system/stop! {:filter-keys #{:a}}))

;; Update registry on start.
(system/start! system! {:registry (assoc registry ::c (constantly ::c))})

;; Suspend suspendable components on stop and resume them on start.
(doto system! (system/stop! {:suspend true})
              (system/start!))

;; Execute components in parallel
(doto (system/init {:parallel true}) (system/start!)
                                     (system/stop!))
(system/start! system! {:parallel true})
(system/stop! system! {:parallel true})

;; Use `with-open` to stop system automatically.
(with-open [s! (system/init {:registry registry})]
  (let [{::keys [a b]} (system/start! s!)]
    (comment "Work with" a b)))
