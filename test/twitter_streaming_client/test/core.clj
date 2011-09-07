(ns twitter-streaming-client.test.core
  (:use [twitter-streaming-client.core])
  (:use midje.sweet))



(fact (@#'twitter-streaming-client.core/message-type {:text "foo"}) => :tweet)
(fact (@#'twitter-streaming-client.core/message-type {:delete {:status {:id 1234}}}) => :delete)
(fact (@#'twitter-streaming-client.core/message-type {:limit {:track 1234}}) => :limit)
(fact (@#'twitter-streaming-client.core/message-type {:scrub_geo {:user_id 1234}}) => :scrub_geo)
(fact (@#'twitter-streaming-client.core/message-type {:blah "blah"}) => :unknown)


