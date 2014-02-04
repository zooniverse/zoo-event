(ns zoo-live.web.routes
  (:use )
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
            [zoo-live.web.event :as ev]
            [clojure.core.match :refer [match]]
            [ring.middleware.json :refer [wrap-json-response]]
            [zoo-live.model.redis :as r]))

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
  [headers params]
  (match 
    [(headers "Content-Type")]
    ["application/x-stream-json"] (ev/stream-response params)
    ["application/json"] (ev/response params)))

(defn routes
  []
  (let [handler (cmpj/routes
                  (GET "/pingdom" [] (resp-ok ""))
									(GET "/countries" [] (resp-ok (r/get-countries)))
                  (GET "/events" {headers :headers params :params} (handle-request headers params))
                  (GET "/cpm" [] (resp-ok (r/save-cpm)))
                  (route/resources "/")
                  (route/not-found "Not Found"))]
    (-> (wrap-json-response handler)
        wrap-dir-index)))
