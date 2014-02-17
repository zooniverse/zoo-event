(ns zoo-live.web.routes
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
            [zoo-live.web.events :as ev]
            [clojure.core.match :refer [match]]
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
  [config headers params]
  (match 
    [(headers "Content-Type")]
    ["application/x-stream-json"] (ev/stream-response config params)
    ["application/json"] (ev/response config params)))

(defn routes
  [config]
  (let [handler (cmpj/routes
                  (GET "/pingdom" [] (resp-ok ""))
                  (GET "/events/:type/:project" 
                       {headers :headers params :params} 
                       ((partial handle-request config) headers params))
                  (route/resources "/")
                  (route/not-found "Not Found"))]
    (-> (wrap-json-response handler)
        wrap-dir-index)))
