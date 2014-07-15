(ns zoo-event.component.database
  (:require [korma.db :as db]
            [korma.core :refer :all]
            [clojure.tools.logging :as log]
            [pg-json.core :refer :all]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]))

(defn json-transformer
  [obj] 
  (update-in obj [:data] from-json-column))

(defn- create-event-entity
  [db-conn event]
  [event (-> (create-entity (str "events_" event))
             (database db-conn)
             (transform json-transformer))])

(defn- uri-to-db-map
  [uri]
  (let [uri (java.net.URI. uri)
        [username password] (str/split (.getUserInfo uri) #":")]
    {:db-name (apply str (drop 1 (.getPath uri)))
     :user username
     :password password
     :host (.getHost uri)
     :port (.getPort uri)}))

(defn- db-connection
  [conn-map]
  (db/create-db (db/postgres conn-map)))

(defn- db-log-name 
  [{:keys [db-name host port]}]
  (str db-name " at " host ":" port))

(defrecord Database [events host port db-name user password connection db-ents]
  component/Lifecycle
  (start [component]
    (if connection 
      component 
      (let [connection (db-connection {:host host
                                       :port port
                                       :db db-name
                                       :user user
                                       :password password})
            db-ents (into {} (map (partial create-event-entity connection) events))]
        (do (log/info (str "Connecting to " (db-log-name component)))
            (-> (assoc component :connection connection)
                (assoc :db-ents db-ents))))))
  (stop [component]
    (if-not connection
      component
      (do (log/info (str "Closing connection to " (db-log-name component)))
          (dissoc component :connection)))))

(defn new-database
  [events jdbc-uri]
  (map->Database (merge {:events events} (uri-to-db-map jdbc-uri))))
