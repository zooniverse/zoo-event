(ns zoo-live.web.server
  (:require [ring.adapter.jetty :as jetty]))

(defn create
  [handler & {:keys [port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defn stop
  [server]
  (.stop server))
