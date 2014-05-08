(ns zoo-event.component.app
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [org.httpkit.server :refer [run-server]]))

(defrecord App [db kafka projects types handler port server]
  component/Lifecycle
  (start [component]
    (if server
      component
      (let [s (run-server (handler db kafka (:ps projects) types) 
                          {:port port :json? false})] 
        (log/info (str "Starting Server on port " port))
        (assoc component :server s))))
  (stop [component]
    (if-not server
      component 
      (do (server :timeout 100)
          (log/info (str "Stopping Server on port " port))
          (assoc component server nil)))))

(defn new-app
  [port types handler]
  (map->App {:port port :types types :handler handler}))
