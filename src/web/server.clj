(ns web.server

  (:require [org.httpkit.server :as s]
            [compojure.core :refer [routes POST GET ANY]]
            [af-kafka10.producer :as pd]
            [af-kafka10.consumer :as cn]
            [clojure.tools.cli :refer [cli]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [web.core :refer [read-and-transform write-messages-to-kafka]]
            ))
(defonce ^:private server (atom nil))

(defn remove-trailing-slashes [handler]
  (fn [req]
    (let [uri (:uri req)
          not-root? (not= uri "/")
          ends-with-slash? (.endsWith ^String uri "/")
          fixed-uri (if (and not-root?
                             ends-with-slash?)
                      (subs uri 0 (dec (count uri)))
                      uri)
          fixed-req (assoc req :uri fixed-uri)]
      (handler fixed-req))))


(defn print-msg
  ([msg] (format "<h1> Hello world from the %s!</h1>" msg))
  ([first-msg second-msg] (format "<h1> Hello world from  %s and also %s!</h1>" first-msg second-msg))
  )

(defn read-from-file []
  (def string1 (slurp "Text.txt"))
  (format string1))

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

(defn write-to-file [msg]
  (spit "Text.txt" msg)
  (def string1 (slurp "Text.txt"))
  (format string1))

(defn write-back-message [msg bird-type]
  (let [bird-type-object (set {:bird_type bird-type})]
    (println (conj msg bird-type-object))
    (write-to-kafka (conj msg bird-type-object))
    )
  )


(defn get-bird [msg country]
  (let [bird-type (get birds-obj (keyword country) "not found")]
    (write-back-message msg bird-type)
    )
  )

(defn print-msg
  ([msg] (format "<h1> Hello world from the %s!</h1>" msg))
  ([first-msg second-msg] (format "<h1> Hello world from  %s and also %s!</h1>" first-msg second-msg))
  )


(defn transform-msg []
  (read-and-transform)
  (format "transformed")
  )

(defn write-messages []
  (write-messages-to-kafka)
  (format "writing to kafka...")
  )


(defn app []
  (routes
    (GET "/" [:as req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    "<h1>Hi from the root!!!</h1>"}
      )
    (GET "/readfile" []
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (read-from-file)}
      )
    (GET "/readbirds" []
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (read-birds-from-file)}
      )
    (GET "/write/:msg" [msg :as req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (write-to-kafka msg)}
      )
    (GET "/writefile/:stuff" [stuff :as req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (write-to-file stuff)}
      )
    (GET "/transform" []
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (transform-msg)}
      )
    (GET "/write-messages" []
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (write-messages)}
      )
    )
  )

(defn create-server []
  (s/run-server (remove-trailing-slashes (app)) {:port 8080}))

(defn stop-server [server]
  (server :timeout 100))


(defn run-server []
  (reset! server (create-server)))

(defn main [args&]
  (app))
