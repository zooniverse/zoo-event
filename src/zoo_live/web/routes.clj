(ns zoo-live.web.routes
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
            [zoo-live.web.events :as ev]
            [clojure.core.match :refer [match]]
            [clj-kafka.consumer.zk :refer [shutdown]]
            [org.httpkit.server :refer [send! with-channel on-close]]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn resp-ok
  [body]
  {:status 200
   :body body})

(defn wrap-dir-index
  [handler]
  (fn [req]
    (handler (update-in req [:uri]
                        #(if (= "/" %) "/index.html" %)))))
(defn handle-request
  [config type project {:keys [params headers] :as req}]
  (let [[c msgs] (ev/stream-response config params)] 
    (with-channel req channel
      (on-close channel (fn [status] (println c) (shutdown c)))
      (doseq [m msgs]
        (println m)
        (send! channel m false)))) 

  (comment (match 
             [(headers "Content-Type")]
             ["application/x-stream-json"] (with-channel req channel
                                             (on-close channel (fn [status] (println "todo: cleanup")))
                                             (doseq [m (ev/stream-response config params)]
                                               (send! channel m false))) 
             ["application/json"] (resp-ok (ev/response config params)))))

(defn routes
  [config]
  (let [handler (cmpj/routes
                  (GET "/pingdom" [] (resp-ok ""))
                  (GET "/events/:type/:project" 
                       [type project :as r]
                       ((partial handle-request config) type project r))
                  (route/resources "/")
                  (route/not-found "Not Found"))]
    (-> (wrap-json-response handler)
        wrap-dir-index)))
