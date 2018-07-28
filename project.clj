(defproject web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :plugins [[s3-wagon-private "1.2.0" :exclusions [org.apache.httpcomponents/httpclient]]]
  :repositories [["releases" {:url "s3p://clojure-deps/releases/"
                              :username :env/AWS_ACCESS_KEY
                              :passphrase :env/AWS_SECRET_KEY}]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.0"]
                 [af-kafka10 "1.0.9"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [org.clojure/tools.reader "1.1.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  )
