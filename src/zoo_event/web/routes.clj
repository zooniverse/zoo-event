(ns zoo-event.web.routes
  (:require [compojure.core :as cmpj]
            [compojure.handler :refer [api]]
            [zoo-event.events :as ev]
            [clj-time.format :as f]
            [zoo-event.web.resp :refer :all]
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

(defn handler
  [db kafka projects types]
  (let [handler (doall (for [p projects t types] 
                         (ev/event-route t p db kafka)))]
    (-> (apply cmpj/routes handler)
        api
        wrap-json-response 
        wrap-to-param
        wrap-from-param)))
