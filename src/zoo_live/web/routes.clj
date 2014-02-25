(ns zoo-live.web.routes
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
            [compojure.handler :refer [api]]
            [zoo-live.events :as ev]
            [zoo-live.web.resp :refer :all]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn wrap-dir-index
  [handler]
  (fn [req]
    (handler (update-in req [:uri]
                        #(if (= "/" %) "/index.html" %)))))

(defn routes
  [config]
  (let [handler (doall (map (partial ev/event-routes config)  
                            (cartesian-product (:types config) (:projects config))))]
    (-> (apply cmpj/routes handler)
        api
        wrap-json-response 
        wrap-dir-index)))
