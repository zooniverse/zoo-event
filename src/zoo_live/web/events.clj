(ns zoo-live.web.events
  (:require [zoo-live.model.event :as e]
            [clj-kafka.core :refer :all]
            [clj-kafka.consumer.zk :refer :all]))

(defn- filter-from-params
  [params])

(defn stream-response
  [config {:keys [project type] :as ps}]
  (let [param-filter (filter-from-params (dissoc ps 
                                                 :project 
                                                 :type 
                                                 :from))
        kafka-config {"zookeeper.connect" (:zookeeper config)
                      "group.id" "zoo-live.1"
                      "auto.offset.reset" "smallest"
                      "auto.commit.enable" "true"}
        topic (str "events_" type "_" project)]
    (println topic)
    (with-resource [c (consumer kafka-config)]
      shutdown
      (take 2 (messages c [topic])))))

(defn response
  [config params]
  )
