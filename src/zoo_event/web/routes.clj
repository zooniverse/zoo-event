(ns zoo-event.web.routes
  (:require [compojure.core :as cmpj]
            [compojure.handler :refer [api]]
            [zoo-event.events :as ev]
            [clj-time.format :as f]
            [zoo-event.web.resp :refer :all]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn wrap-to-param
  [handler]
  (fn [req] 
    (handler
      (if (get-in req [:params :to]) 
        (update-in req [:params :to] #(f/parse (f/formatters :date-time) %))
        req))))

(defn wrap-from-param
  [handler]
  (fn [req]
    (handler 
      (if (get-in req [:params :from]) 
        (update-in req [:params :from] #(f/parse (f/formatters :date-time) %))
        req))))

(defn routes
  [config]
  (let [handler (doall (map (partial ev/event-routes config)  
                            (cartesian-product (:types config) (:projects config))))]
    (-> (apply cmpj/routes handler)
        api
        wrap-json-response 
        wrap-to-param
        wrap-from-param)))
