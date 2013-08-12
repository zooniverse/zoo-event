(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [zoo-live.system :as system]))

(def system nil)

(defn init
  "Constructs Dev System"
  []
  (alter-var-root #'system (constantly (system/system))))

(defn start
  "Starts Dev System"
  []
  (alter-var-root #'system system/start))

(defn stop
  "Stops Dev System"
  []
  (alter-var-root #'system (fn [s] (when s (system/stop s)))))

(defn go
  "Inits and Starts"
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
