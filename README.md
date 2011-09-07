## twitter-streaming-client ##

twitter-streaming-client builds an asynchronous [Clojure](http://clojure.org) client for the Twitter Streaming API built on 
top  [twitter-api](https://github.com/adamwynne/twitter-api)

it manages the connection and deals with problems as specified in the [Twitter Streaming API documentation](https://dev.twitter.com/docs/streaming-api/concepts#connecting). In particular it manages :

* protocol errors with exponential backoff before retry
* network errors with linear backoff before retry
* reconnection after errors
* sorting different message types which may occur on the stream
* error logging

All that is left is to create a connection and do something with the tweets that are returned

## Usage ##




## License ##

Copyright (C) 2011 Trampoline Systems Ltd

Distributed under the Eclipse Public License, the same as Clojure.
