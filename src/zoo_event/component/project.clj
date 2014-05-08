(ns zoo-event.component.project
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defrecord Projects [uri ps]
  component/Lifecycle
  (start [component]
    (if ps
      component
      (let [ps (map :name (:body (http/get uri {:as :json})))]
        (log/info (str "Fetching list of projects from " uri))
        (assoc component :ps ps))))
  (stop [component]
    (if-not ps
      component
      (assoc component :ps nil))))

(defn new-projects
  [uri]
  (map->Projects {:uri uri}))

