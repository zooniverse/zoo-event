(ns zoo-live.model.postgres
  (:use [clojure.math.numeric-tower :only [expt]]
        [clojure.string :only [split]])
  (:require [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :as s]))

(def postgres nil)

(defn connect!
  [uri]
  (alter-var-root #'postgres (constantly uri)))

(defmacro query* [& body] `(j/query postgres ~@body))

(defn- ip-to-int
  [ip]
  (->> (split ip #"\.") 
       (mapv #(Integer. %))
       (reduce-kv (fn [m k v] (->> (nth [24 16 4 1] k)
                              (expt 2)
                              (* v)
                              (+ m))) 0)))

(defn- query-ip
  [ip]
  (first (query* [(str "SELECT l.latitude, l.longitude FROM location l JOIN blocks b ON (l.locId=b.locId) WHERE b.endIpNum >= " ip " order by b.endIpNum limit 1")])))

(defn find-ips
  [& ips]
  (map (comp query-ip ip-to-int) ips))
