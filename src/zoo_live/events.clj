(ns zoo-live.events
  (:require [clj-kafka.core :refer [with-resource]]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.core.async :refer [go <! <!! filter< map< pub sub chan close! go-loop >!]]
            [zoo-live.web.resp :refer :all]
            [korma.core :refer :all]
            [clojure.string :as str]
            [clj-time.coerce :refer [to-sql-date]]
            [pg-json.core :refer :all]
            [compojure.core :refer [GET]]
            [org.httpkit.server :refer [send! with-channel on-close]]
            [clj-kafka.consumer.zk :refer :all]))

(defn filter-user-data
  [ev]
  (dissoc ev :user_name :user_ip))

(defn- ent
  "Creates Korma Entity from event type and project"
  [{:keys [postgres]} type project]
  (-> (create-entity (str "events_" type "_" project))
      (database postgres)
      (transform (fn [obj] (update-in obj [:data] from-json-column)))))

(defn- date-between [w from to]
  (assoc w :created_at ['between from to]))

(def get-date (comp second :created_at))

(defn- params-to-where
  [w [k v]]
  (cond
    (and (contains? w :created_at) (= k :from)) (date-between w (get-date w) (to-sql-date v))
    (and (contains? w :created_at) (= k :to)) (date-between w (to-sql-date v) (get-date w))
    (= k :from) (assoc w :created_at ['> (to-sql-date v)])
    (= k :to) (assoc w :created_at ['< (to-sql-date v)])
    (= k :female) (assoc w :female ['> v])
    (= k :male) (assoc w :male ['> v])
    true (assoc w k v)))

(defn query-from-params
  [ent {:keys [page per_page] :as params :or {page "1" per_page "10"}}]
  (let [where-clause (reduce params-to-where {} (dissoc params :page :per_page))
        page (Integer/parseInt page)
        per_page (Integer/parseInt per_page)]
    (select ent
            (where where-clause)
            (limit per_page)
            (offset (* (- page 1) per_page)))))

(defn- kafka-config
  [zk]
  {"zookeeper.connect" zk 
   "group.id" "zoo-live"
   "auto.offset.reset" "largest"
   "auto.commit.enable" "true"})

(defn- kafka-json-string-to-map
  [msg]
  (->> (:value msg)
       (map #(char (bit-and % 255))) 
       (apply str)
       parse-string))

(defn kafka-stream
  [zk type project]
  (let [conf (kafka-config zk) 
        topic (str "events_" type "_" project)
        msgs (messages (consumer conf) [topic]) 
        channel (chan)]
    (go (doseq [m msgs] (>! channel m)))
    (pub channel (fn [_] true))))

(defn db-response
  [ent params & [mime]]
  (resp-ok (mapv filter-user-data (query-from-params ent params)) mime))

(defn- streaming-response
  [msgs {:keys [params] :as req}]
  (let [in-chan (chan)
        out-chan (->> (sub msgs true in-chan)  
                      (map< kafka-json-string-to-map)
                      (map< filter-user-data)
                      (map< (comp #(str % "\n") generate-string)))]
    (with-channel req channel
      (send! channel (resp-ok (<!! out-chan) stream-mime) false)
      (let [writer (go-loop [msg (<! out-chan)]
                            (send! channel (resp-ok msg stream-mime) false)
                            (recur (<! out-chan)))]
        (on-close channel (fn [status] 
                            (close! writer) 
                            (close! in-chan)
                            (close! out-chan)))))))

(defn handle-request
  [msgs db-ent type project]
  (fn [{:keys [headers] :as req}]
    (cond
      (= (headers "accept") stream-mime) (streaming-response msgs req)
      (= (headers "accept") app-mime) (db-response db-ent (:params req))
      (= (headers "accept") "application/json") (db-response db-ent (:params req) "application/json")
      true (resp-bad-request))))

(defn event-routes
  [config [type project]]
  (let [msgs (kafka-stream (:zookeeper config) type project)
        db-ent (ent config type project)]
    (GET (str "/events/" type "/" project) [:as req] (handle-request msgs db-ent type project))))
