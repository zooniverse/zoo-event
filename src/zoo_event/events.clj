(ns zoo-event.events
  (:require [zoo-event.kafka :as kafka]
            [cheshire.core :refer [generate-string]]
            [clojure.core.async :refer [go <! chan close! go-loop >! timeout alts!]]
            [zoo-event.web.resp :refer :all]
            [korma.core :refer :all]
            [clj-time.coerce :refer [to-sql-time]]
            [zoo-event.component.database :refer [json-transformer]]
            [org.httpkit.server :refer [send! with-channel on-close open?]]))

(defn remove-user-data
  [ev]
  (dissoc ev :user_name :user_ip :male :female :gender))

(defn- ent
  "Creates Korma Entity from event type and project"
  [{:keys [db-ents connection]} type project]
  (if project
    (-> (create-entity (str "events_" type "_" project))
        (database connection)
        (transform json-transformer)
        (table (subselect (db-ents type)
                          (where {:project project}))
               (str "events_" type "_" project)))
    (db-ents type)))

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
  [db [type & [project]] params & [mime]]
  (let [result (query (ent db type project) params)] 
    (resp-ok (mapv remove-user-data result)
             (or mime app-mime))))

(def process-event ^{:private true} 
  (comp #(str % "\n") generate-string remove-user-data :event))

(defn- message-or-heartbeat
  [stream]
  (go (or (first (alts! [stream (timeout 30000)] :priority true)) "Heartbeat\n")))

(defn- streaming-response
  [kafka req type project]
  (let [type-filter (filter #(= (:type %) type))
        project-filter (if project
                         (filter #(= (:project %) project))
                         (map identity))
        transducer (comp type-filter project-filter (map process-event))
        stream (kafka/stream kafka transducer)]
    (with-channel req channel
      (send! channel (resp-ok "Stream Start\n" stream-mime) false)
      (let [writer (go-loop [msg (<! (message-or-heartbeat stream))]
                     (send! channel (resp-ok msg stream-mime) false)
                     (when (open? channel)
                       (recur (<! (message-or-heartbeat stream)))))]
        (on-close channel (fn [status] 
                            (close! writer)
                            (close! stream)))))))

(defn handle-project-request
  [kafka db]
  (fn [req type & [project]]
    (println (get-in req [:headers "accept"]))
    (condp = (get-in req [:headers "accept"])
      stream-mime (streaming-response kafka req type project)
      app-mime (db-response db [type project] (:params req))
      "application/json" (db-response db [type project]
                                      (:params req)
                                      "application/json")
      (unsupported-media-type))))
