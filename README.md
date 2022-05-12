# fitter

Dependency injection library for Clojure.

## Vocabulary

* **instance** something useful instantiated :-)
* **component** describe how to initialize/destroy instance of something.
* **registry** map of keys and components
* **system** collection of dependent component instances.

## Overview

* Components are just functions or maps with `::component/start`/`::component/stop!` keys.
* Component `start` function receives `system` map with keys known in component registry.
* The `registry` is a map of components where keys are unique and known in the system context.
  These keys are referred in `component/start` function argument.
* Dependency is formed when component access system key in start function.
* System “atom” holds the state of running component instances and can be started,
  stopped, restarted etc.

## Examples

* [app-system](examples/src/examples/strojure_fitter/app_system.clj)
* [mount](examples/src/examples/strojure_fitter/mount.clj)

## TODO
* Optional parallel start/stop of system components.
* Experimental: lazy system which does not require explicit `start!` (?).
