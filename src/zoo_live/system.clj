(ns zoo-live.system
  (:require [zoo-live.web.routes :as r]
            [zoo-live.web.server :as s]
            [zoo-live.model.postgres :as post]))

(defn system
  "Returns a new instance of the whole application"
  []
  (let [env (System/getenv) ]
    {:postgres (or (get env "DATABASE_URL") "postgres://localhost:5433/events")
     :handler r/routes
     :zookeeper (or (get env "ZOOKEEPER_CLUSTER") "33.33.33.10:2181") 
     :port 8080}))

(defn start
  [system]
  (let [system (update-in system [:handler] apply [system])
        server (s/create (:handler system)
                         :port (:port system))]
    (post/connect! (:postgres system))
    (into system {:server server})))

(defn stop
  [system]
  (when (:server system)
    (s/stop (:server system)))
  (dissoc system :server))

(defn -main
  [& [port]]
  (start (merge (system) {:port (Integer. port)})))
