(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [zoo-event.system :as app]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (app/system {:jdbc "postgres://storm:4Hq6rSuqGSWj@eventdb.chrs9iawmdss.us-east-1.rds.amazonaws.com:5432/events"
                                           :kafka-config {:zk-connect "zk1.zooniverse.org:2181,zk2.zooniverse.org:2182,zk3.zooniverse.org:2183"
                                                          :group-id "local-zoo-event2"
                                                          :topic "events"
                                                          :threads 4}
                                           :project-uri "https://api.zooniverse.org/projects/list"
                                           :types ["classifications"]}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
