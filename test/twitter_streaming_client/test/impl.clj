(ns twitter-streaming-client.test.impl
  (:use twitter-streaming-client.impl
        midje.sweet)
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

;; gathering messages by type
(fact (messages-by-type [{:text "foo"} 
                         {:delete {:status {:id 1234}}}
                         {:text "bar"}
                         {:limit {:track 1234}}
                         {:text "boo"}]) => {:tweet [{:text "foo"} {:text "bar"} {:text "boo"}]
                                             :delete [{:delete {:status {:id 1234}}}]
                                             :limit [{:limit {:track 1234}}]})


;; parse-twitter-stream-bodyparts