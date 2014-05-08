(ns zoo-event.main
  (:require [zoo-event.system :refer [system]]
            [com.stuartsierra.component :refer [start stop]]
            [clojure.edn :refer [read-string]]))

(defn -main
  [& [conf]]
  (let [conf (read-string (slurp conf))
        sys (system conf)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop sys))))  
    (start sys)))

