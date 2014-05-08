(defproject zoo-event "1.0.0-SNAPSHOT"
  :description "Live Zooniverse Classifications"
  :url "http://github.com/zooniverse/zoo-events"
  :license {:name "Apache Public License v2"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.3.1"]
                 [http-kit "2.1.16"]
                 [clj-http "0.9.1"]
                 [clj-kafka "0.2.4-0.8"]
                 [clj-time "0.6.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [pg-json "0.2.1"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [korma "0.3.1"]
                 [postgresql/postgresql "9.3-1101.jdbc4"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.3"]]}}
  :min-lein-version "2.0.0")
