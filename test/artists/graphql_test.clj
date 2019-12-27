(ns artists.graphql-test
  (:require [clojure.test :refer :all]
            [artists.graphql :refer :all]
            [artists.spotify :refer :all]
            [artists.fixtures :refer :all]))

(deftest test-process-args
  (testing "it converts string keys to keywords"
    (is (= (process-args {"key1" 1
                          "key2" 2})
           {:key1 1
            :key2 2})))
  (testing "it supports empty maps"
    (is (= (process-args {})
           {})))
  (testing "it supports nil maps"
    (is (nil? (process-args nil)))))

(declare params)

(deftest test-search-artists-resolver
  (testing "it calls search-artists with default params"
    (with-redefs [params (atom nil)
                  search-artists #(do
                                    (reset! params %)
                                    artists-search-result)]
      (let [result (search-artists-resolver nil nil {"query" "Test"})]
        (is (= {:artists (get-in artists-search-result [:artists :items])
                :pageInfo {:limit 20
                           :offset 0
                           :hasNext false}
                :totalCount 2}
               result))
        (is (= {:query "Test"
                :market "US"
                :limit 20
                :offset 0}
               @params)))))
  (testing "is calls search artists with specified params"
    (with-redefs [params (atom nil)
                  search-artists #(do
                                     (reset! params %)
                                     multi-page-artists-search-result)]
      (let [result (search-artists-resolver nil nil {"query" "Test"
                                                     "market" "CA"
                                                     "limit" 10
                                                     "offset" 5})]
        (is (= {:artists (get-in multi-page-artists-search-result [:artists :items])
                :pageInfo {:limit 10
                           :offset 5
                           :hasNext true}
                :totalCount 252}
               result))
        (is (= {:query "Test"
                :market "CA"
                :limit 10
                :offset 5}
               @params))))))

(deftest test-artist-related-artists-resolver
  (testing "it calls get-related-artists with the artist href"
    (with-redefs [params (atom nil)
                  get-related-artists #(do
                                         (reset! params %)
                                         related-artists-result)]
      (let [href "https://api.spotify.com/v1/artist/123"
            result (artist-related-artists-resolver nil {:href href} nil)]
        (is (= (:artists related-artists-result)
               result))
        (is (= {:entity-url href}
               @params))))))

(deftest test-artist-albums-resolver
  (testing "it calls get-albums with the default params"
    (with-redefs [params (atom nil)
                  get-albums #(do
                                (reset! params %)
                                albums-result)]
      (let [href "https://api.spotify.com/v1/artist/456"
            result (artist-albums-resolver nil {:href href} nil)]
        (is (= {:albums (:items albums-result)
                :pageInfo {:limit (:limit albums-result)
                           :offset (:offset albums-result)
                           :hasNext true}
                :totalCount (:total albums-result)}
               result))
        (is (= {:entity-url href
                :query-params default-search-params}
               @params)))))
  (testing "it calls get-albums with the provided params"
    (with-redefs [params (atom nil)
                  get-albums #(do
                                (reset! params %)
                                albums-result)]
      (let [href "https://api.spotify.com/v1/artists/675"
            result (artist-albums-resolver nil
                                           {:href href}
                                           {"market" "ES"
                                            "limit" 10
                                            "offset" 5})]
        (is (= {:albums (:items albums-result)
                :pageInfo {:limit (:limit albums-result)
                           :offset (:offset albums-result)
                           :hasNext true}
                :totalCount (:total albums-result)}
               result))
        (is (= {:entity-url href
                :query-params {:market "ES"
                               :limit 10
                               :offset 5}}
               @params))))))

(deftest test-artist-top-tracks-resolver
  (testing "it calls get-top-tracks with the default params"
    (with-redefs [params (atom nil)
                  get-top-tracks #(do
                                    (reset! params %)
                                    top-tracks-result)]
      (let [href "https://api.spotify.com/v1/artist/456"
            result (artist-top-tracks-resolver nil {:href href} nil)]
        (is (= (:tracks top-tracks-result)
               result))
        (is (= {:entity-url href
                :query-params {:market default-market}}
               @params)))))
  (testing "it calls get-top-tracks with the provided params"
    (with-redefs [params (atom nil)
                  get-top-tracks #(do
                                (reset! params %)
                                top-tracks-result)]
      (let [href "https://api.spotify.com/v1/artists/675"
            result (artist-top-tracks-resolver nil
                                               {:href href}
                                               {"market" "ES"})]
        (is (= (:tracks top-tracks-result)
               result))
        (is (= {:entity-url href
                :query-params {:market "ES"}}
               @params))))))

(deftest test-album-tracks-resolver
  (testing "it calls get-tracks with the default params"
    (with-redefs [params (atom nil)
                  get-tracks #(do
                                    (reset! params %)
                                    tracks-result)]
      (let [href "https://api.spotify.com/v1/artist/456"
            result (album-tracks-resolver nil {:href href} nil)]
        (is (= {:tracks (:items tracks-result)
                :pageInfo {:limit 20
                           :offset 0
                           :hasNext false}
                :totalCount 6}
               result))
        (is (= {:entity-url href
                :query-params default-search-params}
               @params)))))
  (testing "it calls get-tracks with the provided params"
    (with-redefs [params (atom nil)
                  get-tracks #(do
                                (reset! params %)
                                tracks-result)]
      (let [href "https://api.spotify.com/v1/artists/675"
            result (album-tracks-resolver nil
                                          {:href href}
                                          {"market" "ES"
                                           "limit" 50
                                           "offset" 1})]
        (is (= {:tracks (:items tracks-result)
                :pageInfo {:limit 20
                           :offset 0
                           :hasNext false}
                :totalCount 6}
               result))
        (is (= {:entity-url href
                :query-params {:market "ES"
                               :limit 50
                               :offset 1}}
               @params))))))

(deftest test-property-resolver
  (testing "it resolves the parent property with the specified key"
    (is (= "value"
           ((property-resolver :foo) nil {:foo "value"} nil)))))

(deftest test-root-resolver
  (testing "it returns the correct resolver for Query searchArtists"
    (is (= (root-resolver "Query" "searchArtists")
           search-artists-resolver)))
  (testing "it returns the correct resolver for Artist related"
    (is (= (root-resolver "Artist" "related")
           artist-related-artists-resolver)))
  (testing "it returns the correct resolver for Artist albumsConnection"
    (is (= (root-resolver "Artist" "albumsConnection")
           artist-albums-resolver)))
  (testing "it returns the correct resolver for Artist topTracks"
    (is (= (root-resolver "Artist" "albumsConnection")
           artist-albums-resolver)))
  (testing "it returns the correct resolver for Album tracksConnection"
    (is (= (root-resolver "Album" "tracksConnection")
           album-tracks-resolver))))
