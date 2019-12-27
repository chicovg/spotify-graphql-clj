(ns artists.spotify-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [artists.spotify :refer :all]
            [artists.fixtures :refer :all])
  (:use clj-http.fake))

(deftest test-add-expiration
  (testing "expires key is set"
    (let [start-time (System/currentTimeMillis)
          original-token {:expires_in 500}
          {:keys [expires]} (add-expiration original-token)]
      (is (> expires start-time)))))

(deftest test-get-oauth-token-response
  (testing "oauth token retrieved"
    (with-fake-routes
      {token-url (fn [request] {:status 200
                                :body "{\"access_token\": \"abcd1234\",
                                       \"token_type\": \"Bearer\",
                                       \"expires_in\": 3600}"})}
      (let [{:keys [access_token token_type expires expires_in]} (get-oauth-token-response)]
        (is (= "abcd1234" access_token))
        (is (= "Bearer" token_type))
        (is (< (System/currentTimeMillis) expires))
        (is (= 3600 expires_in)))))
  (testing "failed requests throw an exception"
    (with-fake-routes
      {token-url (fn [request] {:status 503
                                :body "Server is unavailable"})}
      (is (thrown? Exception "Failed to fetch oauth token"
                   (get-oauth-token-response))))))

(deftest test-token-expired?
  (testing "An expired token is expired"
    (is (token-expired? {:expires (- (System/currentTimeMillis) 1000)})))
  (testing "A new token is not expired"
    (is (not (token-expired? {:expires (+ (System/currentTimeMillis) 3600)})))))

(deftest test-get-access-token
  (testing "A new oauth token is retrieved and cached"
    (with-redefs [cached-token (atom {:expires -1})]
      (with-fake-routes
        {token-url (fn [request] {:status 200
                                  :body "{\"access_token\": \"xyz0123\",
                                          \"token_type\": \"Bearer\",
                                          \"expires_in\": 3600}"})}
        (let [{:keys [access_token token_type expires expires_in] :as token} (get-oauth-token)]
          (is (= "xyz0123" access_token))
          (is (= "Bearer" token_type))
          (is (< (System/currentTimeMillis) expires))
          (is (= 3600 expires_in))
          (is (= token @cached-token))))))
  (testing "cached token is returned"
    (let [token {:access_token "def4567"
                 :expires (+ (System/currentTimeMillis) 3600)}]
      (with-redefs [cached-token (atom token)]
        (with-fake-routes
          {token-url (fn [request] {:status 500
                                    :body "This request shouldn't be made"})}
          (is (= token (get-oauth-token))))))))

(deftest test-search-artists
  (let [params {:query "Andy Narell"
                :market "US"
                :limit 20
                :offset 0}]
    (testing "search results are returned"
      (with-redefs [cached-token (atom {:access_token "abcd1234"
                                        :expires (+ (System/currentTimeMillis) 5000)})]
        (with-fake-routes
          {(str search-url "?query=Andy+Narell&type=artist&market=US&limit=20&offset=0")
           (fn [request] {:status 200
                          :body artists-search-result-json})}
          (is (= artists-search-result
                 (search-artists params))))))
    (testing "failed requests throw and exception"
      (with-fake-routes
        {(str search-url "?query=Andy+Narell&type=artist&market=US&limit=20&offset=0")
         (fn [request] {:status 400
                        :body "Invalid request"})}
        (is (thrown? Exception "Failed to search artists"
                     (search-artists params)))))))

(declare entity-url)

(deftest test-get-artist-data
  (let [entity-url "https://api.spotify.com/v1/artist/123"]
    (testing "related artists are returned"
      (with-redefs [cached-token (atom {:access_token "abcd1234"
                                        :expires (+ (System/currentTimeMillis) 5000)})]
        (with-fake-routes
          {(str entity-url "/related-artists") (fn [request] {:status 200
                                                              :body related-artists-result-json})}
          (is (= related-artists-result
                 (get-related-artists {:entity-url entity-url}))))))
    (testing "albums are returned"
      (with-redefs [cached-token (atom {:access_token "abcd1234"
                                        :expires (+ (System/currentTimeMillis) 5000)})]
        (with-fake-routes
          {(str entity-url "/albums?market=US&limit=10&offset=0")
           (fn [request] {:status 200
                          :body albums-result-json})}
          (is (= albums-result
                 (get-albums {:entity-url entity-url
                              :query-params {:market "US"
                                             :limit 10
                                             :offset 0}}))))))
    (testing "top tracks are returned"
      (with-redefs [cached-token (atom {:access_token "abcd1234"
                                        :expires (+ (System/currentTimeMillis) 5000)})]
        (with-fake-routes
          {(str entity-url "/top-tracks?market=US")
           (fn [request] {:status 200
                          :body top-tracks-result-json})}
          (is (= top-tracks-result
                 (get-top-tracks {:entity-url entity-url
                                  :query-params {:market "US"}}))))))
    (testing "tracks are returned"
      (with-redefs [cached-token (atom {:access_token "abcd1234"
                                        :expires (+ (System/currentTimeMillis) 5000)})]
        (with-fake-routes
          {(str entity-url "/tracks?market=US&limit=10&offset=0")
           (fn [request] {:status 200
                          :body tracks-result-json})}
          (is (= tracks-result
                 (get-tracks {:entity-url entity-url
                              :query-params {:market "US"
                                             :limit 10
                                             :offset 0}}))))))
    (testing "a failed request throws an exception"
      (with-fake-routes
        {(str entity-url "/") (fn [request] {:status 503
                                             :body "Failure"})}
        (is (thrown? Exception "Failed to get related artists for id 123"
                     (get-entity-data "" {:entity-url entity-url})))))))
