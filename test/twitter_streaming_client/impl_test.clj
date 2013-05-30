(ns twitter-streaming-client.impl-test
  (:use twitter-streaming-client.impl
        midje.sweet)
  (:require [clojure.data.json :as json]
            [clojure.string :as str])
  (:import (org.joda.time Instant Duration)))


;; clear-failure
(fact (clear-failure {:failure-count 1 :last-backoff-ms 100 :next-connection-time (Instant.)}) =>
  {:failure-count nil :last-backoff-ms nil :next-connection-time nil})

;; message-type parsing
(fact (message-type {:text "foo"}) => :tweet)
(fact (message-type {:delete {:status {:id 1234}}}) => :delete)
(fact (message-type {:limit {:track 1234}}) => :limit)
(fact (message-type {:scrub_geo {:user_id 1234}}) => :scrub_geo)
(fact (message-type {:blah "blah"}) => :unknown)

(def some-messages [{:text "foo"}
                   {:delete {:status {:id 1234}}}
                   {:text "bar"}
                   {:limit {:track 1234}}
                   {:text "boo"}])

(def some-messages-by-type {:tweet [{:text "foo"} {:text "bar"} {:text "boo"}]
                            :delete [{:delete {:status {:id 1234}}}]
                            :limit [{:limit {:track 1234}}]})

;; gathering messages by type
(fact (messages-by-type some-messages) => some-messages-by-type)


;; parse-twitter-stream-bodyparts
(fact (parse-twitter-stream-bodyparts (str/join "\n" (map json/json-str some-messages))) => some-messages-by-type)
