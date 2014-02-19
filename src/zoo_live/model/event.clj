(ns zoo-live.model.event
  (require [korma.db :as kdb]
           [korma.core :refer :all]
           [clojure.string :as str]))

(defn uri-to-db-map
  [uri]
  (let [uri (java.net.URI. uri)
        [username password] (str/split (.getUserInfo uri) #":")]
    {:db (apply str (drop 1 (.getPath uri)))
     :user username
     :password password
     :host (.getHost uri)
     :port (.getPort uri)}))

(defn ent
  "Creates Korma Entity from event type and project"
  [{:keys [postgres]} type project]
  (let [postgres (kdb/postgres 
                   (if (string? postgres) (uri-to-db-map postgres) postgres))]
    (-> (create-entity (keyword (str "events_" type "_" project)))
        (database postgres))))

(defn- date-between
  [w from to]
  (assoc w :created_at [between from to]))

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
  (let [where-clause (reduce params-to-where {} params)] )
  (select ent
          (where where-clause)))
