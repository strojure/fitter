(ns example.how-to-validate-spec
  "Example how to spec component arguments on system start."
  (:require [clojure.spec.alpha :as s]
            [strojure.fitter.component :as component]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(s/def ::a int?)
(s/def ::b string?)

(defmulti ^:private component-spec identity)
(defmethod component-spec :default [_] nil)
(defmethod component-spec ::a [_] (s/keys :req [::b]))

(defn- wrap-spec
  [[k c]]
  [k (component/of (fn spec-start [system]
                         (some-> (component-spec k) (s/assert* system))
                         (component/start c system))
                   (component/stop-fn c)
                   (when-let [suspend-fn (component/suspend-fn c)]
                     (fn suspend! [instance old-system]
                       (when-let [resume-fn (suspend-fn instance old-system)]
                         (fn spec-resume [new-system]
                           (some-> (component-spec k) (s/assert* new-system))
                           (resume-fn new-system))))))])

(def ^:private registry
  (->> {::a (comp parse-long ::b)
        ::b (constantly "1")}
       (into {} (map wrap-spec))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private system!
  (system/init {:registry registry}))

(comment
  (system/start! system!)
  (system/start! system! {:registry (dissoc registry ::b)})
  (system/start! system! {:registry (assoc registry ::b (constantly :k))})
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
