(ns zoo-event.model.postgres
  (:require [clojure.java.jdbc :as j]
            [clojure.string :refer [split]] 
            [clojure.java.jdbc.sql :as s]))

(def postgres nil)

(defn connect!
  [uri]
  (alter-var-root #'postgres (constantly uri)))
