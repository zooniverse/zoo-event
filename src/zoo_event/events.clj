(ns zoo-event.events
  (:require [clj-kafka.core :refer [with-resource]]
            [cheshire.core :refer [parse-string generate-string]]
            [clojure.core.async :refer [go <! <!! map< sub chan close! go-loop >! timeout alts!]]
            [zoo-event.web.resp :refer :all]
            [korma.core :refer :all]
            [clojure.string :as str]
            [clj-time.coerce :refer [to-sql-time]]
            [compojure.core :refer [GET]]
            [zoo-event.component.database :refer [json-transformer]]
            [org.httpkit.server :refer [send! with-channel on-close open?]]))

(defn filter-user-data
  [ev]
  (dissoc ev :user_name :user_ip :male :female :gender))

(defn- ent
  "Creates Korma Entity from event type and project"
  [{:keys [db-ents connection]} type project]
  (-> (create-entity (str "events_" type "_" project))
      (database connection)
      (transform json-transformer)
      (table (subselect (db-ents type)
                        (where {:project project}))
             (str "events_" type "_" project))))

(defn- date-query
  [query from to]
  (cond
    (and from to) (where query (between :created_at [(to-sql-time from) (to-sql-time to)]))
    from (where query (> :created_at (to-sql-time from)))
    to (where  query (< :created_at (to-sql-time to)))
    true query))

(defn- str-to-int
  [string & [default]]
  (if string
    (Integer/parseInt string)
    (or default 0)))

(defn- query
  [ent {:keys [from to per_page page]}]
  (let [per_page (str-to-int per_page 100)
        page (str-to-int page)
        q (-> (select* ent)
              (date-query from to))]
    (select q
            (order :created_at :DESC)
            (limit per_page)
            (offset (* page per_page)))))

(defn- db-response
  [ent params & [mime]]
  (let [result (query ent params)] 
    (resp-ok (mapv filter-user-data result)
             (or mime app-mime))))

(def process-event ^{:private true} 
  (comp #(str % "\n") generate-string filter-user-data :event))

(defn- message-or-heartbeat
  [stream]
  (go (or (first (alts! [stream (timeout 30000)] :priority true)) "Heartbeat\n")))

(defn- streaming-response
  [msgs msg-key req]
  (let [inchan (sub msgs msg-key (chan)) 
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

(defn- handle-project-request
  [msgs db-ent type project]
  (fn [{:keys [headers] :as req}]
    (cond
      (= (headers "accept") stream-mime) (streaming-response msgs (str type "/" project) req)
      (= (headers "accept") app-mime) (db-response db-ent (:params req))
      (= (headers "accept") "application/json") (db-response db-ent (:params req) "application/json")
      true (resp-bad-request))))

(defn- handle-global-request
  [msgs type]
  (fn [{:keys [headers] :as req}]
    (cond
      (= (headers "accept") stream-mime) (streaming-response msgs type req)
      true (resp-bad-request))))

(defn project-event-route
  [type project db kafka]
  (let [msgs (:project-messages kafka) 
        db-ent (ent db type project)]
    (GET (str "/" type "/" project) [:as req] (handle-project-request msgs db-ent type project))))

(defn global-event-route
  [type kafka]
  (let [msgs (:messages kafka)]
    (GET (str "/" type) [:as req] (handle-global-request msgs type))))
