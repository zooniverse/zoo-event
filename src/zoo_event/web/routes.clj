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

(defn wrap-websockets
  [handler]
  (fn [req]
    (handler
      (if (and (= (get-in req [:headers "upgrade"]) "websocket")
               (not (or (= (get-in req [:headers "accept"]) app-mime)
                        (= (get-in req [:headers "accept"]) "application/json"))))
        (update-in req [:headers] assoc "accept" stream-mime)
        req))))

(defn wrap-cors
  [handler]
  (fn [req]
    (update-in (handler req) [:headers] assoc "Access-Control-Allow-Origin" "*")))

(defn handler
  [db kafka projects types]
  (let [handler (concat (doall (for [p projects t types] 
                                 (ev/project-event-route t p db kafka)))
                        (doall (for [t types]
                                 (ev/global-event-route t kafka))))]
    (-> (apply cmpj/routes handler)
        wrap-websockets
        wrap-to-param
        wrap-from-param 
        wrap-json-response 
        wrap-cors
        api)))
