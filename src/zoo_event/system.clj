(ns zoo-event.system
  (:gen-class :main true))

(defn system
  "Returns a new instance of the whole application"
  []
  (let [env (System/getenv) ]
    {:postgres ""
     :zookeeper ""
     :port 8080
     :types ["classifications"]}))

(defn start
  [& args])

(defn stop
  [& args])
