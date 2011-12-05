(defproject twitter-streaming-client "0.1.3-SNAPSHOT"
  :description "a clojure based client for Twitter's streaming API"
  :url "https://github.com/trampoline/twitter-streaming-client"
  :dependencies [
                 ;; java libs 
                 [joda-time "1.6.2"]
                 [log4j "1.2.16"]
                 [org.slf4j/slf4j-api "1.6.2"]
                 [org.slf4j/slf4j-log4j12 "1.6.2"]

                 ;;clojure
                 [org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.macro "0.1.1"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojars.mccraigmccraig/core.incubator "0.1.1-SNAPSHOT"]

                 ;; twitter async client
                 [twitter-api "0.6.3"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"] 
                     [clojure-source "1.3.0"]
                     [midje "1.3.0-SNAPSHOT"]
                     [lein-midje "1.0.5"]
                     [lein-clojars "0.7.0"]])
 