(ns zoo-event.kafka
  (:require [clj-kafka.consumer.zk :refer :all]
            [cheshire.core :refer [parse-string]]
            [clojure.core.async :refer [chan go >!]]
            [clj-kafka.core :refer :all]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn- kafka-json-string-to-map
  [msg]
  (parse-string (->> (:value msg)
                     (map #(char (bit-and % 255))) 
                     (apply str))
                true))

(defn stream
  [{:keys [config topic threads]} & [transducer]]
  (let [config (assoc config "group.id" (uuid))
        parse-fn (map kafka-json-string-to-map)
        transducer (if transducer
                     (comp parse-fn transducer)
                     parse-fn)
        kchan (chan 1 transducer)]
    (go (with-resource [c (consumer config)]
          shutdown
          (loop [[m & msgs] (messages c topic :threads threads)]
            (when (>! kchan m)
              (recur msgs)))))
    kchan))
