(defproject twitter-streaming-client "0.1.2-SNAPSHOT"
  :description "a clojure based client for Twitter's streaming API"
  :url "https://github.com/trampoline/twitter-streaming-client"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-stacktrace "0.2.3"]
                 [joda-time "1.6.2"]
                 [org.slf4j/slf4j-api "1.6.1"]
                 [org.slf4j/slf4j-jdk14 "1.6.1"]
                 [twitter-api "0.5.0"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"] 
                     [clojure-source "1.2.0"]
                     [midje "1.2.0"]
                     [lein-midje "1.0.3"]
                     [lein-clojars "0.6.0"]])
