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

(def real-tweet "{\"created_at\":\"Tue May 27 00:39:38 +0000 2014\",\"id\":1000000000000,\"id_str\":\"1000000000000\",\"text\":\"RT @SportsCenter: Jeff Samardzija FINALLY wins a game as Cubs beat Giants, 8-4. Samardzija is now 1-4 on season with a 1.68 ERA. http:\\/\\/t.c\\u2026\",\"source\":\"\\u003ca href=\\\"http:\\/\\/twitter.com\\/download\\/iphone\\\" rel=\\\"nofollow\\\"\\u003eTwitter for iPhone\\u003c\\/a\\u003e\",\"truncated\":false,\"in_reply_to_status_id\":null,\"in_reply_to_status_id_str\":null,\"in_reply_to_user_id\":null,\"in_reply_to_user_id_str\":null,\"in_reply_to_screen_name\":null,\"user\":{\"id\":10000001,\"id_str\":\"10000001\",\"name\":\"Joe\",\"screen_name\":\"joe_blow\",\"location\":\"\",\"url\":null,\"description\":null,\"protected\":false,\"followers_count\":214,\"friends_count\":160,\"listed_count\":0,\"created_at\":\"Fri Jun 29 22:39:09 +0000 2012\",\"favourites_count\":1893,\"utc_offset\":-14400,\"time_zone\":\"Eastern Time (US & Canada)\",\"geo_enabled\":false,\"verified\":false,\"statuses_count\":3872,\"lang\":\"en\",\"contributors_enabled\":false,\"is_translator\":false,\"is_translation_enabled\":false,\"profile_background_color\":\"C0DEED\",\"profile_background_image_url\":\"http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"profile_background_image_url_https\":\"https:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"profile_background_tile\":false,\"profile_image_url\":\"http:\\/\\/pbs.twimg.com\\/profile_images\\/40000000\\/YLnI_b48_normal.jpeg\",\"profile_image_url_https\":\"https:\\/\\/pbs.twimg.com\\/profile_images\\/40000000\\/YLnI_b48_normal.jpeg\",\"profile_banner_url\":\"https:\\/\\/pbs.twimg.com\\/profile_banners\\/622378302\\/40000000\",\"profile_link_color\":\"0084B4\",\"profile_sidebar_border_color\":\"C0DEED\",\"profile_sidebar_fill_color\":\"DDEEF6\",\"profile_text_color\":\"333333\",\"profile_use_background_image\":true,\"default_profile\":true,\"default_profile_image\":false,\"following\":null,\"follow_request_sent\":null,\"notifications\":null},\"geo\":null,\"coordinates\":null,\"place\":null,\"contributors\":null,\"retweeted_status\":{\"created_at\":\"Mon May 26 23:19:03 +0000 2014\",\"id\":1000000000000,\"id_str\":\"1000000000000\",\"text\":\"Jeff Samardzija FINALLY wins a game as Cubs beat Giants, 8-4. Samardzija is now 1-4 on season with a 1.68 ERA. http:\\/\\/t.co\\/amlaI0uHGr\",\"source\":\"web\",\"truncated\":false,\"in_reply_to_status_id\":null,\"in_reply_to_status_id_str\":null,\"in_reply_to_user_id\":null,\"in_reply_to_user_id_str\":null,\"in_reply_to_screen_name\":null,\"user\":{\"id\":26257166,\"id_str\":\"26257166\",\"name\":\"SportsCenter\",\"screen_name\":\"SportsCenter\",\"location\":\"Rockin' Bristol, CT since 1979\",\"url\":\"http:\\/\\/www.SportsCenter.com\\/\",\"description\":\"All things sports, all the time. Nominate top plays using #SCtop10. *If you send us a tweet, you consent to letting ESPN use and showcase it in any media*\",\"protected\":false,\"followers_count\":7498846,\"friends_count\":1401,\"listed_count\":34158,\"created_at\":\"Tue Mar 24 15:28:02 +0000 2009\",\"favourites_count\":285,\"utc_offset\":-14400,\"time_zone\":\"Eastern Time (US & Canada)\",\"geo_enabled\":false,\"verified\":true,\"statuses_count\":41212,\"lang\":\"en\",\"contributors_enabled\":false,\"is_translator\":false,\"is_translation_enabled\":true,\"profile_background_color\":\"131516\",\"profile_background_image_url\":\"http:\\/\\/pbs.twimg.com\\/profile_background_images\\/421871393\\/set01.jpg\",\"profile_background_image_url_https\":\"https:\\/\\/pbs.twimg.com\\/profile_background_images\\/421871393\\/set01.jpg\",\"profile_background_tile\":true,\"profile_image_url\":\"http:\\/\\/pbs.twimg.com\\/profile_images\\/464580047228002304\\/lAoaTGAR_normal.jpeg\",\"profile_image_url_https\":\"https:\\/\\/pbs.twimg.com\\/profile_images\\/464580047228002304\\/lAoaTGAR_normal.jpeg\",\"profile_banner_url\":\"https:\\/\\/pbs.twimg.com\\/profile_banners\\/26257166\\/1350220608\",\"profile_link_color\":\"BF0B14\",\"profile_sidebar_border_color\":\"EEEEEE\",\"profile_sidebar_fill_color\":\"EFEFEF\",\"profile_text_color\":\"333333\",\"profile_use_background_image\":true,\"default_profile\":false,\"default_profile_image\":false,\"following\":null,\"follow_request_sent\":null,\"notifications\":null},\"geo\":null,\"coordinates\":null,\"place\":null,\"contributors\":null,\"retweet_count\":2133,\"favorite_count\":2383,\"entities\":{\"hashtags\":[],\"symbols\":[],\"urls\":[],\"user_mentions\":[],\"media\":[{\"id\":471068021323100160,\"id_str\":\"471068021323100160\",\"indices\":[111,133],\"media_url\":\"http:\\/\\/pbs.twimg.com\\/media\\/BomR3pwCcAAl_LR.jpg\",\"media_url_https\":\"https:\\/\\/pbs.twimg.com\\/media\\/BomR3pwCcAAl_LR.jpg\",\"url\":\"http:\\/\\/t.co\\/amlaI0uHGr\",\"display_url\":\"pic.twitter.com\\/amlaI0uHGr\",\"expanded_url\":\"http:\\/\\/twitter.com\\/SportsCenter\\/status\\/471068022208475136\\/photo\\/1\",\"type\":\"photo\",\"sizes\":{\"small\":{\"w\":340,\"h\":236,\"resize\":\"fit\"},\"thumb\":{\"w\":150,\"h\":150,\"resize\":\"crop\"},\"medium\":{\"w\":600,\"h\":416,\"resize\":\"fit\"},\"large\":{\"w\":1024,\"h\":711,\"resize\":\"fit\"}}}]},\"favorited\":false,\"retweeted\":false,\"possibly_sensitive\":false,\"lang\":\"en\"},\"retweet_count\":0,\"favorite_count\":0,\"entities\":{\"hashtags\":[],\"symbols\":[],\"urls\":[],\"user_mentions\":[{\"screen_name\":\"SportsCenter\",\"name\":\"SportsCenter\",\"id\":26257166,\"id_str\":\"26257166\",\"indices\":[3,16]}],\"media\":[{\"id\":471068021323100160,\"id_str\":\"471068021323100160\",\"indices\":[139,140],\"media_url\":\"http:\\/\\/pbs.twimg.com\\/media\\/BomR3pwCcAAl_LR.jpg\",\"media_url_https\":\"https:\\/\\/pbs.twimg.com\\/media\\/BomR3pwCcAAl_LR.jpg\",\"url\":\"http:\\/\\/t.co\\/amlaI0uHGr\",\"display_url\":\"pic.twitter.com\\/amlaI0uHGr\",\"expanded_url\":\"http:\\/\\/twitter.com\\/SportsCenter\\/status\\/471068022208475136\\/photo\\/1\",\"type\":\"photo\",\"sizes\":{\"small\":{\"w\":340,\"h\":236,\"resize\":\"fit\"},\"thumb\":{\"w\":150,\"h\":150,\"resize\":\"crop\"},\"medium\":{\"w\":600,\"h\":416,\"resize\":\"fit\"},\"large\":{\"w\":1024,\"h\":711,\"resize\":\"fit\"}},\"source_status_id\":471068022208475136,\"source_status_id_str\":\"471068022208475136\"}]},\"favorited\":false,\"retweeted\":false,\"possibly_sensitive\":false,\"filter_level\":\"medium\",\"lang\":\"en\"}\r\n")

;;parse a complete tweet chunk
(fact
           (map :text
                    (-> (process-chunk (twitter_streaming_client.impl.TwitterStream. :runnable {} "" nil nil nil nil)
                                       real-tweet)
                        :queues :tweet))
      => (list "RT @SportsCenter: Jeff Samardzija FINALLY wins a game as Cubs beat Giants, 8-4. Samardzija is now 1-4 on season with a 1.68 ERA. http://t.c…"))

;;parse an unevenly split tweet
(fact
  (map :text
       (:tweet (:queues (process-chunk (process-chunk (twitter_streaming_client.impl.TwitterStream. :runnable {} "" nil nil nil nil)
                          (subs real-tweet 0 200)) (subs real-tweet 200)))))
      => (list "RT @SportsCenter: Jeff Samardzija FINALLY wins a game as Cubs beat Giants, 8-4. Samardzija is now 1-4 on season with a 1.68 ERA. http://t.c…"))