(ns artists.handler
  (:require [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer :all]
            [ring.util.response :as r]
            [artists.graphql :refer [execute]]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/graphql" [query variables :as request]
    (r/response (execute query variables nil)))
  (POST "/graphql" [schema query variables operationName :as request]
    (r/response (execute query (json/parse-string variables) operationName)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-json-response
      (wrap-defaults api-defaults)
      (wrap-json-params)))
