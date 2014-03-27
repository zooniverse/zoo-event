(defproject zoo-live "0.3.1-SNAPSHOT"
  :description "Live Zooniverse Classifications"
  :url "http://github.com/edpaget/zoo-live"
  :main zoo-live.system
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.2.0"]
                 [http-kit "2.1.16"]
                 [clj-kafka "0.1.2-0.8"]
                 [clj-time "0.6.0"]
                 [pg-json "0.1.0-SNAPSHOT"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [korma "0.3.0-RC5"]
                 [postgresql/postgresql "8.4-702.jdbc4"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.3"]]}}
  :min-lein-version "2.0.0")
