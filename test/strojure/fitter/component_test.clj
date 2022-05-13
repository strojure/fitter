(ns strojure.fitter.component-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.fitter.component :as component])
  (:import (clojure.lang ExceptionInfo)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- test-suspend-fn
  [new-instance]
  (fn suspend! [old-instance old-system]
    (fn resume [new-system]
      [new-instance new-system old-instance old-system])))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(deftest persistent-map-test
  (testing "A persistent map as a component."

    (testing "Component `start`."
      (test/are [expr result] (= result expr)
        (-> {::component/start identity}
            (component/start :instance)) #_=> :instance
        (try (-> {} (component/start :instance))
             (catch ExceptionInfo _ :exception)) #_=> :exception
        ))

    (testing "Component `stop-fn`"
      (test/are [expr result] (= result expr)
        (-> {::component/stop! identity}
            (component/stop-fn)
            (apply [:instance])) #_=> :instance
        (-> {} (component/stop-fn)) #_=> nil
        ))

    (testing "Component `suspend-fn`"
      (test/are [expr result] (= result expr)
        (-> {::component/suspend! (test-suspend-fn :new-instance)}
            (component/suspend-fn)
            (apply [:instance :old-system])
            (apply [:new-system])) #_=> [:new-instance :new-system :instance :old-system]
        (-> {} (component/suspend-fn)) #_=> nil
        ))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(deftest function-test
  (testing "A function as a component."

    (testing "Component `start`."
      (test/are [expr result] (= result expr)
        (-> identity
            (component/start :instance)) #_=> :instance
        ))

    (testing "Component `stop-fn`"
      (test/are [expr result] (= result expr)
        (-> identity (with-meta {::component/stop! identity})
            (component/stop-fn)
            (apply [:instance])) #_=> :instance
        (-> identity
            (component/stop-fn)) #_=> nil
        ))

    (testing "Component `suspend-fn`"
      (test/are [expr result] (= result expr)
        (-> identity (with-meta {::component/suspend! (test-suspend-fn :new-instance)})
            (component/suspend-fn)
            (apply [:instance :old-system])
            (apply [:new-system])) #_=> [:new-instance :new-system :instance :old-system]
        (-> identity
            (component/suspend-fn)) #_=> nil
        ))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
