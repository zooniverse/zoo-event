(ns zoo-live.web.events
  (:require [zoo-live.model.event :as e]
            [clj-kafka.core :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [clj-kafka.consumer.zk :refer :all]))

(defn- filter-from-params
  [keys])

(defn- kafka-json-string-to-map
  [msg]
  (->> (:value msg)
       (map char)
       (apply str)
       parse-string))

(defn stream-response
  [config {:keys [project type] :as ps}]
  (let [param-filter (filter-from-params (dissoc ps 
                                                 :project 
                                                 :type 
                                                 :from))
        kafka-config {"zookeeper.connect" (:zookeeper config)
                      "group.id" "zoo-live.2"
                      "auto.offset.reset" "smallest"
                      "auto.commit.enable" "true"}
        topic (str "events_" type "_" project)
        consumer (consumer kafka-config)
        msgs (->> (messages consumer [topic])
                  (map kafka-json-string-to-map)
                  (filter param-filter)
                  (map generate-string))]
    [consumer msgs]))

(defn response
  [config params]
  )
