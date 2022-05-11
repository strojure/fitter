(ns strojure.fitter.component-test
  (:require [clojure.test :as test]
            [strojure.fitter.component :as component]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest persistent-map-test
  (test/testing "A persistent map as system component."
    (test/are [expr result] (= result expr)
      (-> #::component{:start identity} (component/start :instance)) #_=> :instance
      (-> #::component{:stop! identity} (component/stop! :instance)) #_=> :instance
      (-> #::component{:start identity} (component/stop! :instance)) #_=> nil
      (-> {} (component/stop! :instance)) #_=> nil)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest function-test
  (test/testing "A function as system component."
    (test/are [expr result] (= result expr)
      (-> identity (component/start :instance)) #_=> :instance
      (-> identity (with-meta #::component{:stop! identity}) (component/stop! :instance)) #_=> :instance
      (-> identity (component/stop! :instance)) #_=> nil)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(comment

  (defn- var-component
    {::component/stop! (fn [inst] inst)}
    [system] system)

  (defn- var-component-no-meta
    [system] system)

  (test/deftest var-test
    (test/testing "A var as system component."
      (test/are [expr result] (= result expr)
        (-> #'var-component (component/start :instance)) #_=> :instance
        (-> #'var-component (component/stop! :instance)) #_=> :instance
        (-> #'var-component-no-meta (component/stop! :instance)) #_=> nil))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
