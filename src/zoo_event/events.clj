(ns zoo-event.events
  (:require [clj-kafka.core :refer [with-resource]]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.core.async :refer [go <! <!! filter< map< sub chan close! go-loop >!]]
            [zoo-event.web.resp :refer :all]
            [korma.core :refer :all]
            [clojure.string :as str]
            [clj-time.coerce :refer [to-sql-date]]
            [pg-json.core :refer :all]
            [compojure.core :refer [GET]]
            [org.httpkit.server :refer [send! with-channel on-close]]))

(defn filter-user-data
  [ev]
  (dissoc ev :user_name :user_ip :male :female :gender))

(defn- ent
  "Creates Korma Entity from event type and project"
  [db type project]
  (-> (create-entity (str "events_" type "_" project))
      (database (:connection db))
      (transform (fn [obj] (update-in obj [:data] from-json-column)))))

(defn- query
  [ent params]
  (select ent
          (limit 10)))

(defn- db-response
  [ent params & [mime]]
  (resp-ok (mapv filter-user-data (query ent params)) 
           (or mime app-mime)))

(def process-event ^{:private true} 
  (comp #(str % "\n") generate-string filter-user-data :event))

(defn- streaming-response
  [msgs type project req]
  (let [stream (map process-event msgs)]
    (with-channel req channel
      (send! channel (resp-ok "Stream Start" stream-mime) false)
      (doseq [m stream] 
        (send! channel (resp-ok m stream-mime) false)))))

(defn- handle-request
  [msgs db-ent type project]
  (fn [{:keys [headers] :as req}]
    (cond
      (= (headers "accept") stream-mime) (streaming-response msgs 
                                                             type 
                                                             project 
                                                             req)
      (= (headers "accept") app-mime) (db-response db-ent (:params req))
      (= (headers "accept") "application/json") (db-response db-ent (:params req) "application/json")
      true (resp-bad-request))))

(defn- by-type-project
  [t p]
  (fn [{:keys [type project]}]
    (and (= type t) (= project p))))

(defn event-route
  [type project db kafka]
  (let [msgs (filter (by-type-project type project) ((:messages kafka))) 
        db-ent (ent db type project)]
    (GET (str "/" type "/" project) [:as req] (handle-request msgs db-ent type project))))
