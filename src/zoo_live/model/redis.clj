(ns zoo-live.model.redis
  (:require [taoensso.carmine :as car]
            [clj-http.client :as client]))

(def connection {})

(defn connect!
  [opts]
  (alter-var-root #'connection (constantly opts)))


(defmacro wcar* [& body] `(car/wcar connection ~@body))

(defn get-location
  [ip]
  (first (:body (client/get (str "http://zoogeo.herokuapp.com/geocode/" ip) {:as :json}))))

(defn save
  [classification]
  (let [id (wcar* (car/incr "id"))
        location (get-location (:user_ip classification))]
    (wcar*
      (car/lpush "classifications"  (merge classification 
                                           {:location location 
                                            :id id}))
      (car/ltrim "classifications" 0 99))))

(defn get-all
  ([]
   (get-all 99))
  ([n]
   (wcar* (car/lrange "classifications" 0 n))))
