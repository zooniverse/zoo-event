(ns zoo-event.component.kafka
  (:require [clj-kafka.consumer.zk :as kafka]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! chan pub go]]
            [com.stuartsierra.component :as component]  
            [cheshire.core :refer [parse-string]]))

(defn- kafka-config
  [zk group-id]
  {"zookeeper.connect" zk 
   "group.id" group-id 
   "auto.offset.reset" "largest"
   "auto.commit.enable" "true"})

(defn- kafka-json-string-to-map
  [msg]
  (parse-string (->> (:value msg)
                     (map #(char (bit-and % 255))) 
                     (apply str))
                true))

(defn- kafka-stream
  [consumer topic threads]
  (let [msgs (kafka/messages consumer topic :threads threads)
        kchan (chan)]
    (go (doseq [m msgs] (>! kchan (kafka-json-string-to-map m))))
    (pub kchan (fn [{:keys [project type]}] (str type "/" project)))))

(defrecord Kafka [zk-connect group-id topic threads consumer messages]
  component/Lifecycle
  (start [component]
    (if messages 
      component
      (let [conf (kafka-config zk-connect group-id)
            c (kafka/consumer conf)
            ms (kafka-stream c topic threads)]
        (log/info "Connecting to Kafka topic: " topic)
        (assoc component :consumer c :messages ms))))
  (stop [component]
    (if-not messages 
      component
      (do (log/info "Closing connection to Kafka topic: " topic)
          (kafka/shutdown consumer)
          (assoc component :messages nil :consumer nil)))))

(defn new-kafka
  [kafka-connect]
  (map->Kafka kafka-connect))