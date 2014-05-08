(ns zoo-event.web.routes
  (:require [compojure.core :as cmpj]
            [compojure.handler :refer [api]]
            [zoo-event.events :as ev]
            [clj-time.coerce :as c]
            [zoo-event.web.resp :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn wrap-to-param
  [handler]
  (fn [req] 
    (handler
      (if (get-in req [:params :to]) 
        (-> (update-in req [:params :to] #(Long/parseLong %))
            (update-in [:params :to] c/from-long))
        req))))

(defn wrap-from-param
  [handler]
  (fn [req]
    (handler 
      (if (get-in req [:params :from]) 
        (-> (update-in req [:params :from] #(Long/parseLong %))
            (update-in [:params :from] c/from-long))
        req))))

(defn handler
  [db kafka projects types]
  (let [handler (doall (for [p projects t types] 
                         (ev/event-route t p db kafka)))]
    (-> (apply cmpj/routes handler)
        wrap-to-param
        wrap-from-param 
        wrap-json-response 
        api)))
