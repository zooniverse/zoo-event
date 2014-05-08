(ns zoo-event.component.project
  (:require [clj-http.client :as http]
            [com.stuartsierra.component :as component]))

(defrecord Projects [uri ps]
  component/Lifecycle
  (start [component]
    (if ps
      component
      (assoc component :ps (map :name (http/get uri {:as :json})))))
  (stop [component]
    (if-not ps
      component
      (assoc component :ps nil))))

(def new-projects
  [uri]
  (map->Projects {:uri uri}))

