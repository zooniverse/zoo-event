(ns zoo-event.component.app
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))

(defrecord App [database kafka projects types handler port server]
  component/Life
  (start [component]
    (if server
      component
      (run-server (handler database kafka projects types) {:port port :json? false})))
  (stop [component]
    (if-not server
      component 
      (server :timeout 100))))

