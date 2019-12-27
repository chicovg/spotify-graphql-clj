(ns artists.fixtures
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-resource
  [filename extension]
  (-> (io/resource (str "fixtures/" filename extension))
      slurp))

(defn load-json-resource
  [filename]
  (load-resource filename ".json"))

(defn load-edn-resource
  [filename]
  (-> (load-resource filename ".edn")
      edn/read-string))

(def artists-search-result-json (load-json-resource "artists-search-result"))
(def artists-search-result (load-edn-resource "artists-search-result"))
(def multi-page-artists-search-result (load-edn-resource "multi-page-artists-search-result"))
(def related-artists-result-json (load-json-resource "related-artists"))
(def related-artists-result (load-edn-resource "related-artists"))
(def albums-result-json (load-json-resource "albums"))
(def albums-result (load-edn-resource "albums"))
(def top-tracks-result-json (load-json-resource "top-tracks"))
(def top-tracks-result (load-edn-resource "top-tracks"))
(def tracks-result-json (load-json-resource "tracks"))
(def tracks-result (load-edn-resource "tracks"))
