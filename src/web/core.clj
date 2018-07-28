(ns web.core
  (:require [org.httpkit.server :as s]
            [compojure.core :refer [routes POST GET ANY]]
            [af-kafka10.producer :as pd]
            [af-kafka10.consumer :as cn]
            [clojure.tools.cli :refer [cli]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            ;[clojure.core.async :refer [async chan go >! <! close]]
            [clojure.walk :refer [stringify-keys]]
            ))

;;;;;;;;;;;;;;;;;;;; creation of the bird dictionary;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn map-to-code-and-bird [new-bird]
  (let [bird-values (str/split new-bird #",")]
    (keywordize-keys {(first bird-values) (last bird-values)})
    )
  )

(defn read-birds-from-file []
  (with-open [rdr (clojure.java.io/reader "birds.csv")]
    (into {} (doall (map map-to-code-and-bird (line-seq rdr)))))
  )

(def birds-obj (read-birds-from-file))
;(def a-channel (chan 1))
;;;;;;;;;;;;;;;;;;;; creation of the json-config;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def program-configuration (json/read-str (slurp "resources/config.json") :key-fn keyword))

;;;;;;;;;;;;;;;;;;;; refactor kafka message - read and write ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn write-to-kafka [msg]
  (let [producer-config (stringify-keys (:kafka-config-producer program-configuration))]
    (with-open [p (pd/producer producer-config (pd/byte-array-serializer) (pd/byte-array-serializer))]
      (pd/send p (pd/record (:cluster-name program-configuration) (.getBytes (pr-str msg)))))
    )
  )

(defn create-message-with-bird-type [msg bird-type]
  (conj msg (set {:bird_type bird-type}))
  )

(defn get-bird [country]
  (get web.core/birds-obj (keyword country) "not found")
  )

(defn read-and-transform []
  (let [consumer-config (stringify-keys (:kafka-config-consumer program-configuration))]
    (with-open [c (cn/consumer consumer-config (cn/string-deserializer) (cn/string-deserializer))]
      (cn/subscribe-to-topics c (:cluster-name program-configuration))
      (let [msg (take 500 (cn/messages c))]
        (let [msg-parsed (json/read-str (:value (into {} msg)))]
          (let [new-parsed-msg (create-message-with-bird-type msg-parsed (get-bird (get (get msg-parsed "geo_info") "country_code")))]
            (write-to-kafka new-parsed-msg)
            )
          )
        )
      )
    )
  )

;;;;;;;;;;;;;;;;;;;; writing messages to kafka from file ;;;;;;;;;;;;;;;;;;;;
(defn kafka-writer [msg]
  (let [producer-config (stringify-keys (:kafka-config-producer program-configuration))]
    (with-open [p (pd/producer producer-config (pd/byte-array-serializer) (pd/byte-array-serializer))]
      (pd/send p (pd/record (:cluster-name program-configuration) (.getBytes msg))))
    )
  )


(defn write-messages-to-kafka []
  (with-open [rdr (clojure.java.io/reader "birds_input.txt")]
    (into {} (doall (map kafka-writer (line-seq rdr))))
    )
  )
