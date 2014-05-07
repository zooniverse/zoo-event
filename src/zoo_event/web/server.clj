(ns zoo-live.web.server
  (:require [org.httpkit.server :refer [run-server]]))

(defn create
  [handler & {:keys [port]}]
  (run-server handler {:port port :join? false}))

(defn stop
  [server]
  (server))
