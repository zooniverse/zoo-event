(defproject zoo-event "2.0.4"
  :description "Live Zooniverse Classifications"
  :url "http://github.com/zooniverse/zoo-events"
  :license {:name "Apache Public License v2"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.3.1"]
                 [http-kit "2.1.16"]
                 [clj-kafka "0.2.6-0.8"]
                 [clj-time "0.6.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [pg-json "0.2.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [korma "0.3.1"]
                 [postgresql/postgresql "9.3-1101.jdbc4"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.3"]]}
   :uberjar {:main zoo-event.main
             :aot [zoo-event.main]}}
  :min-lein-version "2.0.0")
