(ns zoo-live.model.redis
  (:require [taoensso.carmine :as car]
            [zoo-live.model.postgres :as p]))

(def connection {})

(defn connect!
  [opts]
  (alter-var-root #'connection (constantly opts)))


(defmacro wcar* [& body] `(car/wcar connection ~@body))

(defn save
  [classification]
  (let [id (wcar* (car/incr "id"))
        location (first (p/find-ips (:user_ip classification)))
        country (:country location)
        record (dissoc (merge classification 
                              {:location location 
                               :id id}) :user_ip)]
    (wcar*
      (car/sadd "countries" country)
      (car/incr country)
      (car/lpush "classifications" record)
      (car/ltrim "classifications" 0 99))))

(defn get-all
  ([]
   (get-all 99))
  ([n]
   (wcar* (car/lrange "classifications" 0 n))))

(defn get-countries
  []
  (let [result (wcar* 
                 (car/smembers "countries")  
                 (mapv #(car/get %) (wcar* (car/smembers "countries"))))
        [countries & counts] result
        counts (map #(Integer. %) counts)]
    (sort-by #(- (last %)) (into [] (zipmap countries counts)))))

