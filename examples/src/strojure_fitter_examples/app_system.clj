(ns strojure-fitter-examples.app-system
  "Example app-system based on real application with integrant-based system."
  (:require [strojure.fitter.component :as component]
            [strojure.fitter.system :as system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private app-config-component
  "Application configuration from somewhere."
  (constantly {}))

(defn- datasource-component
  [& {:keys [read-only]}]
  {::component/start (fn [{:system/keys [app-config, dev-mode]}]
                       (assert app-config)
                       (println "Create datasource from configuration")
                       {:instance/datasource {:read-only read-only :dev-mode dev-mode}})
   ::component/stop! (fn [instance]
                       (println "Close datasource" instance))})

(defn- database-migration-component
  [{:system/keys [app-config, datasource-readwrite]}]
  (assert app-config)
  (assert datasource-readwrite)
  (println "Perform database migration task")
  :status/OK)

(defn- http-conn-mgr-component
  "Shared HTTP client connection manager. Started only for dependent HTTP clients."
  [& {:as options}]
  {::component/start (fn [{:system/keys []}]
                       (println "Create HTTP connection manager" options)
                       {:instance/http-conn-mgr options})
   ::component/stop! (fn [instance]
                       (println "Shutdown HTTP connection manager" instance))})

(defn- http-client-component
  "HTTP client with specific options."
  [& {:as options}]
  {::component/start (fn [{:system/keys [http-conn-mgr]}]
                       (assert http-conn-mgr)
                       (println "Create HTTP client" options)
                       {:instance/http-client options})
   ::component/stop! (fn [instance]
                       (println "Close HTTP client" instance))})

(def ^:private rpc-connection-component
  "Shared RPC connection. Started only for dependent RPC stuff."
  {::component/start (fn [{:system/keys [app-config]}]
                       (assert app-config)
                       (println "Open RPC connection")
                       {:instance/rpc-connection {}})
   ::component/stop! (fn [instance]
                       (println "Close RPC connection" instance))})

(defn- rpc-client-component
  "RPC client to the named RPC service."
  [service-name]
  {::component/start (fn [{:system/keys [app-config, rpc-connection]}]
                       (assert app-config)
                       (println "Start RPC client" {:service service-name})
                       {:instance/rpc-client {:service service-name :connection rpc-connection}})
   ::component/stop! (fn [instance]
                       (println "Stop RPC client" instance))})

(defn- rpc-server-component
  "RPC server for the named RPC service."
  [service-name]
  {::component/start (fn [{:system/keys [app-config, rpc-connection] _ :deps/ready-to-serve}]
                       (assert app-config)
                       (println "Start RPC server" {:service service-name})
                       {:instance/rpc-server {:service service-name :connection rpc-connection}})
   ::component/stop! (fn [instance]
                       (println "Stop RPC server" instance))})

(def ^:private rpc-broadcast-component
  "RPC broadcast."
  {::component/start (fn [{:system/keys [app-config, rpc-connection] _ :deps/ready-to-serve}]
                       (assert app-config)
                       (println "Start RPC broadcast" rpc-connection)
                       {:instance/rpc-broadcast {:connection rpc-connection}})
   ::component/stop! (fn [instance]
                       (println "Stop RPC broadcast" instance))})

(defn- http-server-component
  "HTTP server for the listed handlers."
  [& handlers]
  {::component/start (fn [{:system/keys [app-config, dev-mode] _ :deps/ready-to-serve}]
                       (assert app-config)
                       (println "Start HTTP server" handlers)
                       {:instance/http-server {:handlers handlers :dev-mode dev-mode}})
   ::component/stop! (fn [instance]
                       (println "Stop HTTP server" instance))})

(defn- mount-component
  "Mount started components to ns vars."
  [keys-map]
  {::component/start (fn [system]
                       (select-keys system (keys keys-map))
                       (println "Mount" (keys keys-map))
                       (keys keys-map))
   ::component/stop! (fn [_]
                       (println "Unmount" keys-map))})

(defn- ready-to-serve
  "Helper to define dependencies to await before opening application to clients."
  [system-keys]
  (fn [system]
    (select-keys system system-keys)
    system-keys))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def app-registry
  "The registry of system components."
  {:system/dev-mode (constantly true)
   :system/app-config app-config-component
   :system/datasource-readwrite (datasource-component :read-only false)
   :system/datasource-readonly (datasource-component :read-only true)
   :task/database-migration database-migration-component
   :system/http-conn-mgr (http-conn-mgr-component)
   :system/http-client (http-client-component)
   :system/restapi-http-client (http-client-component {:no-connection-reuse true})
   :system/rpc-connection rpc-connection-component
   :system/rpc-client-1 (rpc-client-component "service-1")
   :system/rpc-client-2 (rpc-client-component "service-2")
   :system/backend-server (rpc-server-component "backend")
   :system/rpc-broadcast rpc-broadcast-component
   :system/http-server (http-server-component "homepage" "mobile" "webapi")
   :system/mount (mount-component {:system/app-config []
                                   :system/datasource-readwrite []
                                   :system/datasource-readonly []
                                   :system/http-client []
                                   :system/restapi-http-client []
                                   :system/rpc-client-1 []
                                   :system/rpc-client-2 []})
   :deps/ready-to-serve (ready-to-serve [:task/database-migration :system/mount])})

(def ^:private app-system-keys
  "Default keys to start for complete application."
  #{:task/database-migration
    :system/http-client
    :system/restapi-http-client
    :system/rpc-client-1
    :system/rpc-client-2
    :system/rpc-broadcast
    :system/backend-server
    :system/http-server
    :system/mount})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; TODO: `suspend!` in wrap-component

(defn- wrap-component
  [c k]
  {::component/start (fn [system]
                       (try (doto (component/start c system) (->> (println "Started" k "->")))
                            (catch Throwable e
                              (println ":: Starting" k "-> Exception:" (ex-message e))
                              (throw e))))
   ::component/stop! (when-let [stop-fn (component/stop-fn c)]
                       (fn [inst]
                         (try (doto (stop-fn inst) (->> (println "Stopped" k "->")))
                              (catch Throwable e
                                (println ":: Stopped" k "-> Exception:" (ex-message e))))))})

(defn- print-system-status
  [system]
  (println "\n[OK]" (str (count system) "/" (count app-registry)) "system keys started.\n"))

(defn- run-example
  ([system-keys] (run-example system-keys app-registry))
  ([system-keys registry]
   (with-open [system! (system/init {:wrap-component wrap-component})]
     (-> (system/start! system! {:registry registry :filter-keys system-keys})
         (print-system-status))
     (-> (system/inspect system!)
         (select-keys [:deps :system])))))

(comment
  (run-example app-system-keys)
  ; Started :system/app-config -> {}
  ; Started :system/dev-mode -> true
  ; Create datasource from configuration
  ; Started :system/datasource-readwrite -> #:instance{:datasource {:read-only false, :dev-mode true}}
  ; Perform database migration task
  ; Started :task/database-migration -> :status/OK
  ; Create datasource from configuration
  ; Started :system/datasource-readonly -> #:instance{:datasource {:read-only true, :dev-mode true}}
  ; Create HTTP connection manager nil
  ; Started :system/http-conn-mgr -> #:instance{:http-conn-mgr nil}
  ; Create HTTP client nil
  ; Started :system/http-client -> #:instance{:http-client nil}
  ; Create HTTP client {:no-connection-reuse true}
  ; Started :system/restapi-http-client -> #:instance{:http-client {:no-connection-reuse true}}
  ; Open RPC connection
  ; Started :system/rpc-connection -> #:instance{:rpc-connection {}}
  ; Start RPC client {:service service-1}
  ; Started :system/rpc-client-1 -> #:instance{:rpc-client {:service service-1, :connection #:instance{:rpc-connection {}}}}
  ; Start RPC client {:service service-2}
  ; Started :system/rpc-client-2 -> #:instance{:rpc-client {:service service-2, :connection #:instance{:rpc-connection {}}}}
  ; Mount (:system/app-config :system/datasource-readwrite :system/datasource-readonly :system/http-client :system/restapi-http-client :system/rpc-client-1 :system/rpc-client-2)
  ; Started :system/mount -> (:system/app-config :system/datasource-readwrite :system/datasource-readonly :system/http-client :system/restapi-http-client :system/rpc-client-1 :system/rpc-client-2)
  ; Started :deps/ready-to-serve -> [:task/database-migration :system/mount]
  ; Start HTTP server (homepage mobile webapi)
  ; Started :system/http-server -> #:instance{:http-server {:handlers (homepage mobile webapi), :dev-mode true}}
  ; Start RPC broadcast #:instance{:rpc-connection {}}
  ; Started :system/rpc-broadcast -> #:instance{:rpc-broadcast {:connection #:instance{:rpc-connection {}}}}
  ; Start RPC server {:service backend}
  ; Started :system/backend-server -> #:instance{:rpc-server {:service backend, :connection #:instance{:rpc-connection {}}}}
  ;
  ; [OK] 16/16 system keys started.
  ;
  ; Stop HTTP server #:instance{:http-server {:handlers (homepage mobile webapi), :dev-mode true}}
  ; Stopped :system/http-server -> nil
  ; Stop RPC broadcast #:instance{:rpc-broadcast {:connection #:instance{:rpc-connection {}}}}
  ; Stopped :system/rpc-broadcast -> nil
  ; Stop RPC server #:instance{:rpc-server {:service backend, :connection #:instance{:rpc-connection {}}}}
  ; Stopped :system/backend-server -> nil
  ; Unmount #:system{:app-config [], :datasource-readwrite [], :datasource-readonly [], :http-client [], :restapi-http-client [], :rpc-client-1 [], :rpc-client-2 []}
  ; Stopped :system/mount -> nil
  ; Close datasource #:instance{:datasource {:read-only false, :dev-mode true}}
  ; Stopped :system/datasource-readwrite -> nil
  ; Stop RPC client #:instance{:rpc-client {:service service-2, :connection #:instance{:rpc-connection {}}}}
  ; Stopped :system/rpc-client-2 -> nil
  ; Stop RPC client #:instance{:rpc-client {:service service-1, :connection #:instance{:rpc-connection {}}}}
  ; Stopped :system/rpc-client-1 -> nil
  ; Close RPC connection #:instance{:rpc-connection {}}
  ; Stopped :system/rpc-connection -> nil
  ; Close datasource #:instance{:datasource {:read-only true, :dev-mode true}}
  ; Stopped :system/datasource-readonly -> nil
  ; Close HTTP client #:instance{:http-client {:no-connection-reuse true}}
  ; Stopped :system/restapi-http-client -> nil
  ; Close HTTP client #:instance{:http-client nil}
  ; Stopped :system/http-client -> nil
  ; Shutdown HTTP connection manager #:instance{:http-conn-mgr nil}
  ; Stopped :system/http-conn-mgr -> nil
  #_=>
  #_{:deps {:system/datasource-readwrite #{:system/dev-mode :system/app-config},
            :deps/ready-to-serve #{:system/datasource-readwrite
                                   :system/mount
                                   :system/dev-mode
                                   :task/database-migration
                                   :system/app-config},
            :system/http-server #{:system/datasource-readwrite
                                  :deps/ready-to-serve
                                  :system/rpc-client-2
                                  :system/rpc-connection
                                  :system/mount
                                  :system/dev-mode
                                  :system/restapi-http-client
                                  :system/rpc-client-1
                                  :system/datasource-readonly
                                  :system/http-client
                                  :system/http-conn-mgr
                                  :task/database-migration
                                  :system/app-config},
            :system/rpc-client-2 #{:system/rpc-connection :system/app-config},
            :system/rpc-connection #{:system/app-config},
            :system/mount #{:system/datasource-readwrite
                            :system/rpc-client-2
                            :system/rpc-connection
                            :system/dev-mode
                            :system/restapi-http-client
                            :system/rpc-client-1
                            :system/datasource-readonly
                            :system/http-client
                            :system/http-conn-mgr
                            :system/app-config},
            :system/rpc-broadcast #{:system/datasource-readwrite
                                    :deps/ready-to-serve
                                    :system/rpc-client-2
                                    :system/rpc-connection
                                    :system/mount
                                    :system/dev-mode
                                    :system/restapi-http-client
                                    :system/rpc-client-1
                                    :system/datasource-readonly
                                    :system/http-client
                                    :system/http-conn-mgr
                                    :task/database-migration
                                    :system/app-config},
            :system/restapi-http-client #{:system/http-conn-mgr},
            :system/rpc-client-1 #{:system/rpc-connection :system/app-config},
            :system/datasource-readonly #{:system/dev-mode :system/app-config},
            :system/backend-server #{:system/datasource-readwrite
                                     :deps/ready-to-serve
                                     :system/rpc-client-2
                                     :system/rpc-connection
                                     :system/mount
                                     :system/dev-mode
                                     :system/restapi-http-client
                                     :system/rpc-client-1
                                     :system/datasource-readonly
                                     :system/http-client
                                     :system/http-conn-mgr
                                     :task/database-migration
                                     :system/app-config},
            :system/http-client #{:system/http-conn-mgr},
            :task/database-migration #{:system/datasource-readwrite :system/app-config}},
     :system {:system/datasource-readwrite #:instance{:datasource {:read-only false, :dev-mode true}},
              :deps/ready-to-serve [:task/database-migration :system/mount],
              :system/http-server #:instance{:http-server {:handlers ("homepage" "mobile" "webapi"),
                                                           :dev-mode true}},
              :system/rpc-client-2 #:instance{:rpc-client {:service "service-2",
                                                           :connection #:instance{:rpc-connection {}}}},
              :system/rpc-connection #:instance{:rpc-connection {}},
              :system/mount (:system/app-config
                              :system/datasource-readwrite
                              :system/datasource-readonly
                              :system/http-client
                              :system/restapi-http-client
                              :system/rpc-client-1
                              :system/rpc-client-2),
              :system/dev-mode true,
              :system/rpc-broadcast #:instance{:rpc-broadcast {:connection #:instance{:rpc-connection {}}}},
              :system/restapi-http-client #:instance{:http-client {:no-connection-reuse true}},
              :system/rpc-client-1 #:instance{:rpc-client {:service "service-1",
                                                           :connection #:instance{:rpc-connection {}}}},
              :system/datasource-readonly #:instance{:datasource {:read-only true, :dev-mode true}},
              :system/backend-server #:instance{:rpc-server {:service "backend",
                                                             :connection #:instance{:rpc-connection {}}}},
              :system/http-client #:instance{:http-client nil},
              :system/http-conn-mgr #:instance{:http-conn-mgr nil},
              :task/database-migration :status/OK,
              :system/app-config {}}}

  (run-example nil)
  (run-example #{:task/database-migration})
  (run-example #{:system/http-client})
  (run-example #{:system/http-server})
  (run-example #{:system/http-server} (dissoc app-registry :system/mount))
  (run-example #{:system/backend-server})
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
