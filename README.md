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
* Fit small and large systems.
* Help developer to reason about system state.

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

* Start/stop system of components, full or partial.
* Suspend/resume components on system restart.
* Bind component instances to global vars aka mount.
* Parallel execution of components during system start/stop (option).

## Basic usage

### Component declaration

Minimal component is just a function receiving “map” of other component
instances and returns component instance. This “map” supports only `ILookup`
interface. All lookups from this map instantiate requested components and form a
dependency between components dynamically.

Complete component defines its start, stop and suspend behaviour.

```
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

```
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

;; Start/stop only specific keys.
(system/start! system! {:filter-keys #{::a}})
(system/stop! system! {:filter-keys #{::a}})

;; Update registry on start.
(system/start! system! {:registry (assoc registry ::c (constantly ::c))})

;; Suspend suspendable components on stop and resume them on start.
(doto system! (system/stop! {:suspend true})
              (system/start!))

;; Execute components in parallel
(system/init {:parallel true})
(system/start! system! {:parallel true})
(system/stop! system! {:parallel true})

;; Use `with-open` to stop system automatically.
(with-open [system! (system/init {:registry registry})]
  (let [{::keys [a b]} (system/start! system!)]
    (comment "Work with" a b)))
```

## Examples

* [app-system](examples/src/strojure_fitter_examples/app_system.clj)
* [How to log](examples/src/strojure_fitter_examples/how_to_log.clj)
* [How to mount](examples/src/strojure_fitter_examples/how_to_mount.clj)
* [How to parallel](examples/src/strojure_fitter_examples/how_to_parallel.clj)
* [How to spec](examples/src/strojure_fitter_examples/how_to_spec.clj)
* [How to suspend](examples/src/strojure_fitter_examples/how_to_suspend.clj)

