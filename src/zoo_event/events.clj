(ns zoo-event.events
  (:require [clj-kafka.core :refer [with-resource]]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.core.async :refer [go <! <!! map< sub chan close! go-loop >! timeout alts!]]
            [zoo-event.web.resp :refer :all]
            [korma.core :refer :all]
            [clojure.string :as str]
            [clj-time.coerce :refer [to-sql-time]]
            [pg-json.core :refer :all]
            [compojure.core :refer [GET]]
            [org.httpkit.server :refer [send! with-channel on-close open?]]))

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
  [ent {:keys [from to per_page page]}]
  (let [per_page (if per_page (Integer/parseInt per_page) 100)
        page (if page (Integer/parseInt page) 0)
        q (select* ent) 
        q (cond
            (and from to) (where q (between :created_at [(to-sql-time from) (to-sql-time to)]))
            from (where q (> :created_at (to-sql-time from)))
            to (where  q (< :created_at (to-sql-time to)))
            true q)]
    (select q
            (order :created_at :DESC)
            (limit per_page)
            (offset (* page per_page)))))

(defn- db-response
  [ent params & [mime]]
  (resp-ok (mapv filter-user-data (query ent params)) 
           (or mime app-mime)))

(def process-event ^{:private true} 
  (comp #(str % "\n") generate-string filter-user-data :event))

(defn- message-or-heartbeat
  [stream]
  (go (or (first (alts! [stream (timeout 30000)] :priority true)) "Heartbeat\n")))

(defn- streaming-response
  [msgs type project req]
  (let [inchan (sub msgs (str type "/" project) (chan)) 
        stream (map< process-event inchan)]
    (with-channel req channel
      (send! channel (resp-ok "Stream Start\n" stream-mime) false)
      (let [writer (go-loop [msg (<! (message-or-heartbeat stream))]
                            (send! channel (resp-ok msg stream-mime) false)
                            (when (open? channel)
                              (recur (<! (message-or-heartbeat stream)))))]
        (on-close channel (fn [status] 
                            (close! writer)
                            (close! stream)
                            (close! inchan)))))))

(defn- handle-request
  [msgs db-ent type project]
  (fn [{:keys [headers] :as req}]
    (cond
      (= (headers "accept") stream-mime) (streaming-response msgs type project req)
      (= (headers "accept") app-mime) (db-response db-ent (:params req))
      (= (headers "accept") "application/json") (db-response db-ent (:params req) "application/json")
      true (resp-bad-request))))

(defn event-route
  [type project db kafka]
  (let [msgs (:messages kafka) 
        db-ent (ent db type project)]
    (GET (str "/" type "/" project) [:as req] (handle-request msgs db-ent type project))))
