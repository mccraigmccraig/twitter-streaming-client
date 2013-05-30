## twitter-streaming-client ##

[![Build Status](https://travis-ci.org/mccraigmccraig/twitter-streaming-client.png?branch=master)](https://travis-ci.org/mccraigmccraig/twitter-streaming-client)

twitter-streaming-client builds an asynchronous [Clojure](http://clojure.org) client for the Twitter Streaming API on
top of [twitter-api](https://github.com/adamwynne/twitter-api)

it manages the connection and deals with problems as specified in the [Twitter Streaming API documentation](https://dev.twitter.com/docs/streaming-api/concepts#connecting). In particular it manages :

* protocol errors with exponential backoff before retry
* network errors with linear backoff before retry
* reconnection after errors
* sorting different message types which may occur on the stream
* error logging

All that is left is to create a connection and do something with the tweets that are returned

## Usage ##


    (ns foo
      (:require [twitter-streaming-client.core :as client]
                [twitter.oauth :as oauth]))

    (def consumer-key "<insert-key-here>")
    (def consumer-secret "<insert-secret-here>")
    (def user-access-token "<insert-token-here>")
    (def user-access-token-secret "<insert-more-secrets-here>")

    (def creds (oauth/make-oauth-creds consumer-key consumer-secret
                                       user-access-token user-access-token-secret))

    ;; create the client with a twitter.api streaming method and params of your choice
    (def stream  (client/create-twitter-stream twitter.api.streaming/statuses-filter
                                               :oauth-creds creds :params {:track "kittens"}))

    ;; fire up the client and start collecting tweets and other messages
    (client/start-twitter-stream stream)

    ;; returns a hash of all collected message queues. empties the queues
    ;; keys are message-types: :tweet, :delete, :scrub_geo, :limit, :unkown
    ;; values are vectors of JSON decoded messages
    (def q (client/retrieve-queues stream))

    ;; cancel the client : stop collecting tweets
    (client/cancel-twitter-stream stream)


## License ##

Copyright (C) 2011 Trampoline Systems Ltd

Distributed under the Eclipse Public License, the same as Clojure.
