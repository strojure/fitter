# fitter

System component management library for Clojure.

[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/fitter.svg)](https://clojars.org/com.github.strojure/fitter)

Similar purpose libraries:

* [Component](https://github.com/stuartsierra/component)
* [Integrant](https://github.com/weavejester/integrant)
* [mount](https://github.com/tolitius/mount)
* [Dependency injection](https://github.com/darkleaf/di)

## Design goals

* Describe system component dependencies in single place.
* Declare system components as easy as new function.
* Allow ad-hoc and dynamic dependencies defined by components themselves.
* Fit for systems with any amount of components.
* Maximum flexibility for any scenario.
* Help developer to reason about the whole system state.

## Terminology

* **instance**
    * something useful instantiated :-)
* **component**
    * the description how to initialize/destroy instance.
* **registry**
    * map of keys and components used in system start/stop operations.
* **system**
    * collection of dependent component instances.

## Features

* Start/stop system of components, all registered components or only part of
  them.
* Suspend/resume components on system restart.
* Bind component instances to global vars aka mount (optional).
* Parallel execution of components during system start/stop (optional).

## Basic usage

### Component declaration

Minimal component is just a function receiving system “map” of other component
instances and returning this component instance. The system “map” supports only
`ILookup` interface, see [system-test]. All lookups instantiate requested
components and form a dependency between components dynamically. (!) All
components are tightly coupled together by system key names.

[system-test]: test/strojure/fitter/system_test.clj

Complete component defines its start, stop and suspend behaviour.

```clojure
(ns user.readme.component
  (:require [strojure.fitter.component :as component]))

;; Simple function describes component start behaviour.
(def component (fn [{:keys [another-component]}]
                 (comment "Use" another-component)
                 :instance))

;; Just constant component.
(def component (constantly true))

;; Component described as hash map with required `::component/start` key.
(def component
  {::component/start (constantly :instance)
   ::component/stop! (fn stop! [instance] 
                       (comment "Destroy" instance))
   ::component/suspend! (fn suspend! [old-instance old-system]
                          (comment "Suspend" old-instance old-system)
                          (fn resume [new-system]
                            (comment "Resume" old-instance new-system)
                            :instance))})

;; Same map as above created using `component/of`.
(def component
  (component/of (constantly :instance)
                (fn stop! [instance] 
                  (comment "Destroy" instance))
                (fn suspend! [old-instance old-system]
                  (comment "Suspend" old-instance old-system)
                  (fn resume [new-system]
                    (comment "Resume" old-instance new-system)
                    :instance))))
```

### System state

System state is a variable holding instances of the running components.
The state is initialized by `init` and then altered by `start!` and `stop!`.

```clojure
(ns user.readme.system-state
  (:require [strojure.fitter.system :as system]))

(def registry
  {::a (constantly ::a)
   ::b (fn [{::keys [a]}] {::b a})})

;; Initialize system state.
(defonce system!
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
```

## Examples

* [Feature: mount](examples/src/strojure_fitter_examples/feature_mount.clj)
* [Feature: parallel](examples/src/strojure_fitter_examples/feature_parallel.clj)
* [Feature: suspend](examples/src/strojure_fitter_examples/feature_suspend.clj)
* [How-to: add logging](examples/src/strojure_fitter_examples/how_to_add_logging.clj)
* [How-to: reuse components](examples/src/strojure_fitter_examples/how_to_reuse_components.clj)
* [How-to: validate spec](examples/src/strojure_fitter_examples/how_to_validate_spec.clj)

