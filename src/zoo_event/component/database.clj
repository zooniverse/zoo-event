(ns zoo-event.component.databse
  (:require [korma.db :as db]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

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

(defrecord Database [host port db-name user pass connection]
  component/Lifecycle
  (start [component]
    (if connection 
      component 
      (do (log/info (str "Connecting to " (db-log-name)))
          (assoc component :connection (db-connection {:host host
                                                       :port port
                                                       :db db-name
                                                       :user user
                                                       :password password})))))
  (stop [component]
    (if-not connection
      component
      (do (log/info (str "Closing connection to " (db-log-name)))
          (.close connection)
          (dissoc component :connection)))))

(defn new-database
  [jdbc-uri]
  (map->database (uri-to-db-map jdbc-uri)))
