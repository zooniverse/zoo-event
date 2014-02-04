(defproject zoo-live "0.1.0-SNAPSHOT"
  :description "Live Zooniverse Classifications"
  :url "http://github.com/edpaget/zoo-live"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/url "0.0.8"]
                 [org.clojure/data.json "0.2.2"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.2.0"]
                 [http-kit "2.1.16"]
                 [clj-time "0.6.0"]
                 [clj-http "0.7.7"]
                 [clj-kafka "0.1.2-0.8"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.3"]]}}
  :min-lein-version "2.0.0")
