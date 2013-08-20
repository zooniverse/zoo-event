(ns zoo-live.model.redis
  (:use [clj-time.core :only [day month now minus weeks]])
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
        today (now)
        week-ago (minus today (weeks 1))
        date (str (month today) "-" (day today))
        prev-date (str (month week-ago) "-" (day week-ago))
        record (dissoc (merge classification 
                              {:location location 
                               :id id}) :user_ip)]
    (wcar*
      (car/sadd "countries" country)
      (car/incr country)
      (car/lpush "classifications" record)
      (car/ltrim "classifications" 0 99)
      (car/lpush (str "classifications-" date) record)
      (if (= 1 (wcar* (car/exists (str "classifications-" week-ago))))
        (car/del (str "classifications-" week-ago))))))

(defn get-all
  ([]
   (get-all 99))
  ([n]
   (wcar* (car/lrange "classifications" 0 n))))

(defn get-date
  [date]
  (let [key (str "classifications-" date)] 
    (wcar* (car/lrange key 0 (wcar* (car/llen key))))))

(defn get-countries
  []
  (let [result (wcar* 
                 (car/smembers "countries")  
                 (mapv #(car/get %) (wcar* (car/smembers "countries"))))
        [countries & counts] result
        counts (map #(Integer. %) counts)]
    (sort-by #(- (last %)) (into [] (zipmap countries counts)))))

