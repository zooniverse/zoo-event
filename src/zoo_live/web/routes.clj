(ns zoo-live.web.routes
  (:require [compojure.core :as cmpj :refer [OPTIONS GET POST context]]
            [compojure.route :as route]
            [zoo-live.web.events :as ev]
            [clojure.core.match :refer [match]]
            [clj-kafka.consumer.zk :refer [shutdown]]
            [org.httpkit.server :refer [send! with-channel on-close]]
            [ring.middleware.json :refer [wrap-json-response]]))

(def stream-mime "application/vnd.zooevents.stream.v1+json")
(def app-mime "application/vnd.zooevents.v1+json")

(defn resp-ok
  [body & [content-type]]
  {:status 200
   :headers {"Content-Type" (or content-type app-mime)}
   :body body})

(defn resp-bad-request
  []
  {:status 401
   :headers {"Content-Type" app-mime}
   :body {"status" "Bad Request"}})

(defn wrap-dir-index
  [handler]
  (fn [req]
    (handler (update-in req [:uri]
                        #(if (= "/" %) "/index.html" %)))))

(defn- streaming-response
  [config type project params req]
  (let [[c msgs] (ev/stream-response config params)] 
    (with-channel req channel
      (send! channel (resp-ok "" stream-mime) false)
      (on-close channel (fn [status] (shutdown c)))
      (doseq [m msgs]
        (send! channel (resp-ok m stream-mime) false)))))

(defn handle-request
  [config type project {:keys [params headers] :as req}]
  (match 
    [(headers "Accepts")]
    [mime] (streaming-response config type project params req)
    [app-mime] (resp-ok (ev/response config params))
    ["application/json"] (resp-ok (ev/response config params) "application/json")
    [_] (resp-bad-request)))

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
