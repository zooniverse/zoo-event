(ns zoo-live.web.events
  (:require [zoo-live.model.event :as e]
            [clj-kafka.core :refer :all]
            [clj-kafka.consumer.zk :refer :all]))

(defn- filter-from-params
  [params])

(defn- kafka-value-to-string
  [msg]
  (str (apply str (map char (:value msg))) "\n"))

(defn stream-response
  [config {:keys [project type] :as ps}]
  (let [param-filter (filter-from-params (dissoc ps 
                                                 :project 
                                                 :type 
                                                 :from))
        kafka-config {"zookeeper.connect" (:zookeeper config)
                      "group.id" "zoo-live.2"
                      "auto.offset.reset" "smallest"
                      "auto.commit.enable" "false"}
        topic (str "events_" type "_" project)
        consumer (consumer kafka-config)]
    [consumer (map kafka-value-to-string (messages consumer [topic]))]))

(defn response
  [config params]
  )
