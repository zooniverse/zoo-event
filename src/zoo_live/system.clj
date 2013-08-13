(ns zoo-live.system
  (:require [zoo-live.web.routes :as r]
            [zoo-live.web.server :as s]
            [zoo-live.model.redis-pub-sub :as rps]
            [zoo-live.model.redis :as red]))

(defn system
  "Returns a new instance of the whole application"
  []
  (let [env (System/getenv) 
        redis-pub-sub (or (get env "REDIS_PUB_SUB")
                          "redis://127.0.0.1:6379/0")
        redis (or (get env "REDISTOGO_URL")
                  (get env "REDIS")
                  "redis://127.0.0.1:6379/0")]
    {:redis-pub-sub {:pool {} 
                     :listener {} 
                     :spec {:uri redis-pub-sub}}
     :redis {:pool {} :spec {:uri redis}}
     :handler (r/routes)
     :port 8080}))

(defn start
  [system]
  (let [server (s/create (:handler system)
                         :port (:port system))
        system (update-in system 
                          [:redis-pub-sub :listener] 
                          rps/make-listener (:redis-pub-sub system))]
    (red/connect! (:redis system))
    (into system {:server server})))

(defn stop
  [system]
  (when (:server system)
    (s/stop (:server system)))
  (rps/stop (:redis-pub-sub system))
  (update-in (dissoc system :server)
             [:redis-pub-sub :listener]
             {}))

(defn -main
  [& [port]]
  (start (merge (system) {:port (Integer. port)})))
