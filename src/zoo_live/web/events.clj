(ns zoo-live.web.events
  (:require [zoo-live.model.event :as e]
            [clj-kafka.core :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [clj-kafka.consumer.zk :refer :all]))

(defn- filter-from-params
  [{:keys [gender country city]}]
  (let [gender-fn (when (or (= "f" gender) (= "m" gender)) 
                    (fn [msg] (= gender (:gender msg))))
        country-fn (when country
                  (fn [msg] (= country (:country_code msg))))
        city-fn (when city
                  (fn [msg] (= city (:city msg))))
        filters (filter (comp not nil?) [gender-fn country-fn city ])]
    (reduce (fn [m-fn n-fn] 
              (fn [msg] (and (m-fn msg) (n-fn msg)))) 
            (fn [msg] true) 
            filters)))

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
                                                 :type))
        kafka-config {"zookeeper.connect" (:zookeeper config)
                      "group.id" "zoo-live"
                      "auto.offset.reset" "largest"
                      "auto.commit.enable" "true"}
        topic (str "events_" type "_" project)
        consumer (consumer kafka-config)
        msgs (->> (messages consumer [topic])
                  (map kafka-json-string-to-map)
                  (filter param-filter)
                  (map generate-string))]
    [consumer msgs]))

(defn response
  [config type project params]
  (let [entity (e/ent config type project)]
    (e/query-from-params entity params)))
