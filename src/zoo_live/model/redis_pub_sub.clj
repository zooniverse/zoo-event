(ns zoo-live.model.redis-pub-sub
  (:require [taoensso.carmine :as car]
            [zoo-live.model.redis :as r]
            [clojure.data.json :as j]))

(defn update
  [[msg-type key-name classification]]
  (if (and (= msg-type "message")
           (= key-name "classifications"))
    (r/save (j/read-str classification :key-fn keyword))))

(defn make-listener
  [_ connection]
  (car/with-new-pubsub-listener (:spec connection)
    {"classifications" update}
    (car/subscribe "classifications")))

(defn stop
  [connection]
  (car/wcar (dissoc connection :listener)
        (car/close-listener (:listener connection))))
