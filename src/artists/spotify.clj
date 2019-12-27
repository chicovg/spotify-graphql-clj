(ns artists.spotify
  (:require [clj-http.client :as client]
            [clj-http.client :as client]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def client-id (System/getenv "SPOTIFY_CLIENT_ID"))
(def client-secret (System/getenv "SPOTIFY_CLIENT_SECRET"))

(def token-url "https://accounts.spotify.com/api/token")
(def base-api-url "https://api.spotify.com/v1")
(def search-url (str base-api-url "/search"))

(defn add-expiration
  [token-response]
  (assoc token-response :expires (+ (System/currentTimeMillis) (:expires_in token-response))))

(defn get-oauth-token-response
  []
  (try+
   (-> (client/post token-url
                    {:basic-auth [client-id client-secret]
                     :form-params {:grant_type "client_credentials"}
                     :as :json-strict})
       :body
       add-expiration)
   (catch Object {:keys [status body]}
     (log/error "Failed to fetch oauth token" status body)
     (throw+))))

(def cached-token (atom {:expires -1}))

(defn token-expired?
  [token]
  (< (:expires token) (System/currentTimeMillis)))

(defn get-oauth-token
  []
  (if (token-expired? @cached-token)
    (reset! cached-token (get-oauth-token-response))
    @cached-token))

(defn search-artists
  [{:keys [query market limit offset] :as params}]
  (try+
   (let [{:keys [access_token]} (get-oauth-token)]
     (->> (client/get search-url {:oauth-token access_token
                                  :query-params {:query query
                                                 :type "artist"
                                                 :market market
                                                 :limit limit
                                                 :offset offset}
                                  :as :json-strict})
          :body))
   (catch Object {:keys [status body]}
     (log/error "Failed to search artists" status body)
     (throw+))))

(defn get-entity-data
  [path {:keys [entity-url query-params]}]
  (let [{:keys [access_token]} (get-oauth-token)
        request-url (str entity-url "/" path)]
    (try+
     (->> (client/get request-url {:oauth-token access_token
                                   :query-params query-params
                                   :as :json-strict})
          :body)
     (catch Object {:keys [status body]}
       (log/error "Failed to get artist data from" request-url status body)
       (throw+)))))

(def get-related-artists (partial get-entity-data "related-artists"))

(def get-albums (partial get-entity-data "albums"))

(def get-top-tracks (partial get-entity-data "top-tracks"))

(def get-tracks (partial get-entity-data "tracks"))
