(ns zoo-event.main)

(defn -main
  [& [conf]]
  (let [sys (if conf (merge (system) (read-string (slurp conf))) (system))] 
    (start sys)))
