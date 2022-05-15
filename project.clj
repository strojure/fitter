(defproject com.github.strojure/fitter "0.2.23-SNAPSHOT"
  :description "Dependency injection for Clojure"
  :url "https://github.com/strojure/fitter"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.11.1" :scope "provided"]]

  :profiles {:dev {:source-paths ["examples/src"]
                   :dependencies []}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
