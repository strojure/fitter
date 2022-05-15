# fitter

System component management library for Clojure.

[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/fitter.svg)](https://clojars.org/com.github.strojure/fitter)
(beta)

## Status

* [x] Start/stop system of components.
* [x] Mount running instances.
* [x] Suspend/resume components.
* [x] Parallel start/stop.

## Terminology

* **instance**
    * something useful instantiated :-)
* **component**
    * describe how to initialize/destroy instance of something.
* **registry**
    * map of keys and components
* **system**
    * collection of dependent component instances.

## Overview

* Components are just functions or maps with `::component/start`/`::component/stop!` keys.
* Component `start` function receives `system` map with keys known in component registry.
* The `registry` is a map of components where keys are unique and known in the system context.
  These keys are referred in `component/start` function argument.
* Dependency is formed when component access system key in start function.
* System “atom” holds the state of running component instances and can be started,
  stopped, restarted etc.

## Examples

* [app-system](examples/src/strojure_fitter_examples/app_system.clj)
* [How to log](examples/src/strojure_fitter_examples/how_to_log.clj)
* [How to mount](examples/src/strojure_fitter_examples/how_to_mount.clj)
* [How to parallel](examples/src/strojure_fitter_examples/how_to_parallel.clj)
* [How to suspend](examples/src/strojure_fitter_examples/how_to_suspend.clj)

## TODO

* Optional parallel start/stop of system components.
* Experimental: lazy system which does not require explicit `start!` (?).
