(ns zoo-live.web.routes
  (:use ring.middleware.json
        ring.middleware.stacktrace)
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
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
(defn routes
  []
  (let [handler (cmpj/routes
                  (GET "/pingdom" [] (resp-ok ""))
                  (GET "/classifications" [] (resp-ok (r/get-all)))
                  (GET "/classifications/:n" [n] (resp-ok (r/get-all n)))
                  (route/resources "/")
                  (route/not-found "Not Found"))]
    (-> (wrap-json-response handler)
        wrap-dir-index
        wrap-json-params
        wrap-stacktrace)))
