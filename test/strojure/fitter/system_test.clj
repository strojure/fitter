(ns strojure.fitter.system-test
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.test :as test :refer [deftest testing]]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(declare thrown? thrown-with-msg?)

(s/def ::a int?)

(defn- test-system
  []
  (let [delays! (atom {:a (delay 1) :b (delay 2) :c (delay [:c0 :c1])})
        deps! (atom {})]
    (#'system/component-system :x delays! deps!)))

(deftest component-system-test
  (testing "Component system - supported map operations."
    (test/are [expr] expr
      ;; get existing key
      (= 1 ((test-system) :a))
      (= 1 (:a (test-system)))
      (= 1 (get (test-system) :a))
      (= 1 ((test-system) :a :not-found))
      (= 1 (:a (test-system) :not-found))
      (= 1 (get (test-system) :a :not-found))
      ;; get not existing key
      (= nil ((test-system) :x))
      (= nil (:x (test-system)))
      (= nil (get (test-system) :x))
      (= :not-found ((test-system) :x :not-found))
      (= :not-found (:x (test-system) :not-found))
      (= :not-found (get (test-system) :x :not-found))
      ;; get-in
      (= :c0 (get-in (test-system) [:c 0]))
      (= nil (get-in (test-system) [:c 3]))
      (= :not-found (get-in (test-system) [:c 3] :not-found))
      ;; destructure
      (let [{:keys [a b x]} (test-system)]
        (= [1 2 nil] [a b x]))
      (let [{:keys [a b x] :or {a :not-found b :not-found x :not-found}} (test-system)]
        (= [1 2 :not-found] [a b x]))
      ;; select keys
      (= {:a 1 :b 2} (select-keys (test-system) [:a :b]))
      ;; find
      (= [:a 1] (find (test-system) :a))
      (= nil (find (test-system) :x))
      ;; s/keys
      (s/valid? (s/keys :req-un [::a ::b]) (test-system))
      ))

  (testing "Component system - unsupported map operations."
    (test/are [expr] expr
      (thrown? AbstractMethodError (assoc (test-system) :a 0))
      (thrown? AbstractMethodError (dissoc (test-system) :a))
      (thrown? AbstractMethodError (into {} (test-system)))
      (thrown? AbstractMethodError (set/rename-keys (test-system) {:a :aa}))
      ))

  (testing "Component system - undefined behavior."
    (test/are [expr] expr
      (nil? (keys (test-system)))
      (nil? (vals (test-system)))
      )))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
