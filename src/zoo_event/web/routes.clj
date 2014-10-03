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

(defn wrap-cors
  [handler]
  (fn [req]
    (update-in (handler req) [:headers] assoc "Access-Control-Allow-Origin" "*")))

(defn handler
  [db kafka]
  (let [handler (cmpj/routes
                 (cmpj/GET "/pingdom" [] (resp-ok "OK" "text/plain"))
                 (cmpj/GET "/:type" [type :as req]
                           (ev/handle-global-request (:messages kafka) type))
                 (cmpj/GET "/:type/:project" [type project :as req]
                           ((ev/handle-project-request kafka db) type project req)))]
    (-> (wrap-to-param handler)
        wrap-from-param 
        wrap-json-response 
        wrap-cors
        api)))
