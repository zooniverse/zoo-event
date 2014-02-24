(ns zoo-live.events
  (:require [clj-kafka.core :refer [with-resource]]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.core.match :refer [match]]
            [clojure.core.async :refer [go <! filter< map< dropping-buffer chan close! go-loop >!]]
            [zoo-live.web.resp :refer :all]
            [korma.core :refer :all]
            [clojure.string :as str]
            [compojure.core :refer [GET]]
            [org.httpkit.server :refer [send! with-channel on-close]]
            [clj-kafka.consumer.zk :refer :all]))

(defn- ent
  "Creates Korma Entity from event type and project"
  [{:keys [postgres]} type project]
  (-> (create-entity (keyword (str "events_" type "_" project)))
      (database postgres)))

(defn- date-between [w from to]
  (assoc w :created_at ['between from to]))

(defn- params-to-where
  [w [k v]]
  (cond
    (and (contains? w :created_at) (= k :from)) (date-between w v (:created_at w))
    (and (contains? w :created_at) (= k :to)) (date-between w v (:created_at w))
    (= k :from) (assoc w :created_at [> v])
    (= k :to) (assoc w :create [< v])
    true (assoc w k v)))

(defn query-from-params
  [ent params]
  (let [where-clause (reduce params-to-where {} params)] (select ent
            (where where-clause))))

(defn- kafka-config
  [zk]
  {"zookeeper.connect" zk 
   "group.id" "zoo-live"
   "auto.offset.reset" "largest"
   "auto.commit.enable" "true"})

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
       (map char) (apply str)
       parse-string))

(defn kafka-stream
  [zk type project]
  (let [conf (kafka-config zk) 
        topic (str "events_" type "_" project)]
    (messages (consumer conf) [topic])))

(defn db-response
  [ent params]
  (query-from-params ent params))

(defn- streaming-response
  [msgs {:keys [params] :as req}]
  (let [in-chan (chan (dropping-buffer 100))
        out-chan (map< generate-string (map< kafka-json-string-to-map in-chan))]
    (future (doseq [m msgs] (println m) (>! in-chan m)))
    (with-channel req channel
      (send! channel (resp-ok "" stream-mime) false)
      (on-close channel (fn [status] (close! in-chan) (close! out-chan)))
      (go-loop []
               (println "Here")
               (if-let [m (<! out-chan)] 
                 
                 (do (println m)
                     (send! channel (resp-ok m stream-mime) false)
                     ))
               
                     (recur)      
               ))))

(defn handle-request
  [msgs db-ent type project]
  (fn [{:keys [headers] :as req}]
    (match 
      [(headers "accept")]
      [mime] (streaming-response msgs req)
      [app-mime] (resp-ok (db-response db-ent (:params req)))
      ["application/json"] (resp-ok (db-response config (:params req)) "application/json")
      [_] (resp-bad-request))))

(defn event-routes
  [config [type project]]
  (println type project)
  (let [msgs (kafka-stream (:zookeeper config) type project)
        db-ent (ent config type project)]
    (GET (str "/events/" type "/" project) [:as req] (handle-request msgs db-ent type project))))
