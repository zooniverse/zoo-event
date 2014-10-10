(ns zoo-event.component.kafka
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))


(defn- kafka-config
  [zk]
  {"zookeeper.connect" zk 
   "auto.offset.reset" "largest"
   "auto.commit.enable" "false"})

(defrecord Kafka [zk-connect config topic threads consumer messages]
  component/Lifecycle
  (start [component]
    (if messages 
      component
      (let [conf (kafka-config zk-connect)]
        (log/info "Load Kafka config")
        (assoc component :config conf))))
  (stop [component]
    (if-not config
      component
      (do (log/info "Closing connection to Kafka")
          (assoc component :config nil)))))

(defn new-kafka
  [kafka-connect]
  (map->Kafka kafka-connect))
