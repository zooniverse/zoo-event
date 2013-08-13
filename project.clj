(defproject zoo-live "0.1.0-SNAPSHOT"
  :description "Live Zooniverse Classifications"
  :url "http://github.com/edpaget/zoo-live"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/url "0.0.8"]
                 [org.clojure/data.json "0.2.2"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.2.0"]
                 [clj-http "0.7.6"]
                 [com.taoensso/carmine "2.2.0"]
                 [ring/ring-devel "1.2.0-RC1"]
                 [ring/ring-jetty-adapter "1.2.0-RC1"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.3"]]}}
  :min-lein-version "2.0.0")
