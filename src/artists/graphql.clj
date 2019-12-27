(ns artists.graphql
  (:require [graphql-clj.executor :as ex]
            [graphql-clj.query-validator :as qv]
            [graphql-clj.schema-validator :as sv]
            [clojure.core.match :as m]
            [clojure.java.io :as io]
            [artists.spotify :refer [get-albums
                                     get-related-artists
                                     get-top-tracks
                                     get-tracks
                                     search-artists]]))

(defn load-schema
  [filename]
  (-> (io/resource filename)
      slurp))

(def artists-schema (load-schema "schema.graphqls"))

(def federated-schema (load-schema "federated.graphqls"))

(def schema (-> (str artists-schema federated-schema)
                sv/validate-schema))

(defn process-args
  [args]
  (when args
    (into {}
          (for [[k v] args]
            [(keyword k) v]))))

(def default-market "US")
(def default-search-params {:market default-market
                            :limit 20
                            :offset 0})

(defn search-artists-resolver
  [_ _ args]
  (let [processed-args (process-args args)
        params (merge default-search-params processed-args)
        {{:keys [items limit next offset total]} :artists} (search-artists params)]
    {:artists items
     :pageInfo {:offset offset
                :limit limit
                :hasNext (not (nil? next))}
     :totalCount total}))

(defn artist-related-artists-resolver
  [_ {:keys [href]} _]
  (:artists (get-related-artists {:entity-url href})))

(defn artist-albums-resolver
  [_ {:keys [href]} args]
  (let [processed-args (process-args args)
        params (merge default-search-params processed-args)
        {:keys [items limit next offset total]} (get-albums {:entity-url href
                                                             :query-params params})]
    {:albums items
     :pageInfo {:limit limit
                :offset offset
                :hasNext (not (nil? next))}
     :totalCount total}))

(defn artist-top-tracks-resolver
  [_ {:keys [href]} {:strs [market]}]
  (:tracks (get-top-tracks {:entity-url href
                            :query-params {:market (or market
                                                       default-market)}})))

(defn album-tracks-resolver
  [_ {:keys [href]} args]
  (let [processed-args (process-args args)
        params (merge default-search-params processed-args)
        {:keys [items limit next offset total]} (get-tracks {:entity-url href
                                                             :query-params params})]
    {:tracks items
     :pageInfo {:limit limit
                :offset offset
                :hasNext (not (nil? next))}
     :totalCount total}))

(defn property-resolver
  [property]
  (fn [_ parent _] (property parent)))

(defn root-resolver [type-name field-name]
  (m/match
   [type-name field-name]
    ["Query" "_service"] (fn [] {:sdl schema})
    ["Query" "_entities"] (fn [] (prn "_entities"))

    ["Query" "searchArtists"] search-artists-resolver

    ["Artist" "related"] artist-related-artists-resolver
    ["Artist" "albumsConnection"] artist-albums-resolver
    ["Artist" "topTracks"] artist-top-tracks-resolver

    ["Album" "releaseDate"] (property-resolver :release_date)
    ["Album" "totalTracks"] (property-resolver :total_tracks)
    ["Album" "tracksConnection"] album-tracks-resolver

    ["Track" "discNumber"] (property-resolver :disc_number)
    ["Track" "durationMs"] (property-resolver :duration_ms)
    :else nil))

(defn execute
  [query variables operation-name]

  (prn "Query" query)
  (when variables (prn "Variables " variables))
  (when operation-name (prn "Operation " operation-name))

  (let [validated-query (qv/validate-query schema query)]
    (ex/execute nil schema root-resolver validated-query variables operation-name)))
