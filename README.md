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

## Overview

* Components are just functions or maps with `::component/start`
  /`::component/stop!` keys.
* Component `start` function receives `system` map with keys known in component
  registry. This map supports get operations only.
* The `registry` is a map of components where keys are unique and known in the
  system context. These keys are referred in `component/start` function
  argument.
* Dependencies are dynamic and built when component access system key in
  its `start` function.
* System state instance holds running component instances and used to start,
  stop and restart registered components.

## Examples

* [app-system](examples/src/strojure_fitter_examples/app_system.clj)
* [How to log](examples/src/strojure_fitter_examples/how_to_log.clj)
* [How to mount](examples/src/strojure_fitter_examples/how_to_mount.clj)
* [How to parallel](examples/src/strojure_fitter_examples/how_to_parallel.clj)
* [How to spec](examples/src/strojure_fitter_examples/how_to_spec.clj)
* [How to suspend](examples/src/strojure_fitter_examples/how_to_suspend.clj)

