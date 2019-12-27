(defproject artists "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[cheshire "5.9.0"]
                 [clj-http "3.10.0"]
                 [clj-http-fake "1.0.3"]
                 [compojure "1.6.1"]
                 [graphql-clj "0.2.5"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.5.0"]
                 [ring/ring-codec "1.1.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]
                 [slingshot "0.12.2"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler artists.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
