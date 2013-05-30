(ns twitter-streaming-client.impl
  "an asynchronous client for the twitter streaming api, using twitter.api.streaming
   for Twitter Streaming API calls, and adding support for recovery and retry and batch
   processing of results"
  (:use clojure.core.strint
        clojure.tools.macro)
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.stacktrace :as st]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [http.async.client :as ac]
   [twitter.oauth :as oauth]
   twitter.api.streaming
   [twitter.callbacks :as callbacks]
   [twitter.callbacks.handlers :as handlers])
  (:import
   java.io.Writer
   (twitter.callbacks.protocols AsyncStreamingCallback)
   (org.joda.time Instant Duration)
   (clojure.lang IPending)))

(defn stack-trace-str
  [e]
  (with-out-str (st/print-cause-trace e)))

(defmacro with-warnings
  "evaluates forms. catches, logs and rethrows any
   exceptions generated while evaluating forms"
  [& forms]
  `(try
     ~@forms
     (catch Throwable e#
       (log/warn (stack-trace-str e#))
       (throw e#))))

(defmacro defaction
  "a defn-, wrapping the body in with-warnings"
  [name & macro-args]
  (let [[n [p & r]] (name-with-attributes name macro-args)]
    `(defn ~n ~p (with-warnings ~@r))))

(defmacro ignore-exceptions
  "evaluates forms. catches and logs, but otherwise ignores,
   any Exceptions generated while evaluating forms"
  [& forms]
  `(try
     ~@forms
     (catch Exception e#
       (log/debug "ignoring Exception" (stack-trace-str e#)))))

;;; response - the current http.async.client response
;;; queues - hash of message-type keyword keyed vectors of decoded JSON tweet stream objects
;;; failure-count - time of last failure
;;; last-backoff-ms - last backoff period in ms
;;; next-connection-time - org.joda.time.Instant after which to attempt re-connection
(defrecord TwitterStream
    [state ;; one of : [:runnable, :stopped]
     queues
     response
     failure-count
     last-backoff-ms
     next-connection-time])

(defn inspect
  "print an object to a string with print-method"
  [o]
  (with-out-str (print-method o *out*)))

(defn print-response
  "print an http.async.client response to a Writer.
   necessary because http.async.client responses have promises in
   and default printing of undelivered promises hangs the repl under clojure 1.2"
  [r, ^java.io.Writer w]
  (.write w (str "http.async.client/Response{"))
  (->> [:id :status :headers :done :error]
       (map (fn [f] (<< "~(name f): ~(inspect (f r))")))
       (str/join ", ")
       (.write w))
  (.write w "}"))

(defn print-response-str
  "print an http.async.client response to a String"
  [r]
  (with-out-str (print-response r *out*)))

(defmethod print-method TwitterStream [o, ^java.io.Writer w]
  (.write w (str (.getName TwitterStream) "{"))
  (->> [:state :queues :failure-count :last-backoff :next-connection]
       (map (fn [f] (<< "~(name f): ~(inspect (f o))")))
       (str/join ", ")
       (.write w))
  (.write w ", response: ")
  (if (:response o)
    (print-response (:response o) w)
    (.write w "nil"))
  (.write w "}"))

(defn http-async-response-done?
  "returns true if the http.async.client response is finished"
  [response]
  (or (not response) ;; there is no response
      (realized? (:done response)) ;; done promise delivered
      ((:cancelled? (meta response))))) ;; cancelled

(defn cancel-http-async-client-response
  "cancel an http.async.client response if it's not already cancelled"
  [response]
  (if (not (http-async-response-done? response))
      (do (log/info "cancelling http.async.client request")
          (ignore-exceptions ((:cancel (meta response)))))
      (log/info "already cancelled/finished")))

(defn clear-failure
  "clear failures from a TwitterStream"
  [twitter-stream]
  (assoc twitter-stream
    :failure-count nil
    :last-backoff-ms nil
    :next-connection-time nil))

(defn message-type
  "given a message from the twitter stream returns the type of message :
   one of :tweet, :delete, :scrub_geo, :limit or :unknown"
  [message]
  (cond
   (contains? message :text) :tweet
   (contains? message :delete) :delete
   (contains? message :scrub_geo) :scrub_geo
   (contains? message :limit) :limit
   true :unknown))

(defn messages-by-type
  "given a sequence of messages from the twitter stream, splits them into a hash of
   sequences of tweets, deletes, scrub_geo, and limit messages"
  [messages]
  (->> messages
       (map (fn [m] [(message-type m) m]))
       (reduce (fn [h [t m]] (assoc h t
                                    (conj (or (get h t) [])
                                          m)))
               {})))

(defn parse-twitter-stream-bodyparts
  "given a body string of multiple lines, with each line containing zero or
   one JSON encoded tweets or other twitter streaming objects,
   return a hash containing a sequence for each type of object, keyed by the type"
  [body]
  (->> body
       (str/split-lines)
       (filter (comp (fn [i] (> i 0)) count str/trim))
       ((fn [lines]
          (log/info (<< "received ~(count lines) message lines"))
          lines))
       (map json/read-json)
       (messages-by-type)))

(defaction enqueue-bodypart-action
  "if the status is delivered? and 200, then
   clear failures, and append received messages to the queues"
  [twitter-stream response body]
  (if (and (realized? (:status response))
           (= (:code @(:status response)) 200))
    (-> twitter-stream
        (clear-failure)
        (assoc :queues (merge-with into
                                   (:queues twitter-stream)
                                   (parse-twitter-stream-bodyparts body))))
    (do
      (log/warn "ignoring body from incomplete or failed request")
      twitter-stream)))

(defn create-enqueue-on-bodypart-handler
  "twitter-api on-bodypart handler : sends enqueue-bodypart-action"
  [twitter-stream-agent]
  (fn [response baos]
    (let [body (str baos)]
      (send-off twitter-stream-agent enqueue-bodypart-action response body))))

(defaction empty-queues-action
  "call f with the queues,
   returns an updated twitter-stream with empty queues"
  [twitter-stream f]
  (f (:queues twitter-stream))
  (assoc twitter-stream :queues {}))

(declare start-twitter-stream-action)

(defaction record-failure-action
  "record an http failure.
   - doubles the backoff time, maxing at 240s
     as per https://dev.twitter.com/docs/streaming-api/concepts#connecting
   - logs the failed response, and clears the response field
   - kicks off a re-start of the client"
  [twitter-stream twitter-stream-agent response]
  (let [last-backoff-ms  (or (:last-backoff-ms twitter-stream) 10000)
        next-backoff-ms (min 240000 (* 2 last-backoff-ms))
        now (Instant.)
        next-connection-time (.plus now (long next-backoff-ms))
        next-failure-count (inc (or (:failure-count twitter-stream) 0))]
    (log/warn (<< "protocol failure. failure-count ~{next-failure-count}. backing off ~{next-backoff-ms}ms until ~(.toString next-connection-time)"))
    (log/warn (print-response-str response))
    (send-off twitter-stream-agent start-twitter-stream-action (:request-fn (meta twitter-stream-agent)) :force? false)
    (assoc twitter-stream
      :response nil
      :failure-count next-failure-count
      :last-backoff-ms next-backoff-ms
      :next-connection-time next-connection-time)))

(defn create-record-failure-on-failure-handler
  "twitter-api on-failure handler : sends record-failure-action to the agent"
  [twitter-stream-agent]
  (fn [response]
    (send-off twitter-stream-agent record-failure-action twitter-stream-agent response)))

(defaction record-exception-action
  "record some non-http failure.
   - increases the backoff time in increments of 250ms, capped at 16s
     as per https://dev.twitter.com/docs/streaming-api/concepts#connecting
   - kicks off a re-start of the client"
  [twitter-stream twitter-stream-agent response throwable]
  (let [state (:state twitter-stream)
        last-backoff-ms  (or (:last-backoff-ms twitter-stream) 250)
        next-backoff-ms (min 16000 (+ 250 last-backoff-ms))
        now (Instant.)
        next-connection-time (.plus now (long next-backoff-ms))
        next-failure-count (inc (or (:failure-count twitter-stream) 0))]
    (if (= state :runnable)
      (do
        (log/warn (<< "network failure. failure-count ~{next-failure-count}. backing off ~{next-backoff-ms}ms until ~(.toString next-connection-time)"))
        (log/warn (print-response-str response))
        (log/warn (stack-trace-str throwable))
        (send-off twitter-stream-agent start-twitter-stream-action (:request-fn (meta twitter-stream-agent)) :force? false)
        (assoc twitter-stream
          :response nil
          :failure-count next-failure-count
          :last-backoff-ms next-backoff-ms
          :next-connection-time next-connection-time))
      (do
        (log/debug "exception ignored : stream cancelled")
        twitter-stream))))

(defn create-record-exception-on-exception-handler
  "twitter-api on-exception handler : sends record-exception-action to the agent"
  [twitter-stream-agent]
  (fn [response throwable]
    (send-off twitter-stream-agent record-exception-action twitter-stream-agent response throwable)))

(defn async-streaming-callbacks
  "the twitter-api/http.async.client callbacks"
  [twitter-stream-agent]
  (AsyncStreamingCallback. (create-enqueue-on-bodypart-handler twitter-stream-agent)
                           (create-record-failure-on-failure-handler twitter-stream-agent)
                           (create-record-exception-on-exception-handler twitter-stream-agent)))

(defn twitter-stream-agent-error-handler
  "log the exception, cancel any request in progress,
   restart the agent, clearing actions, and start the
   stream processing again"
  [twitter-stream-agent throwable]
  (with-warnings
    (let [state @twitter-stream-agent
          response (:response @state)
          new-state (assoc state :response nil)]
      (log/warn "error on twitter-stream-agent. clearing actions and restarting")
      (log/warn (stack-trace-str throwable))
      (if response (cancel-http-async-client-response response))
      (restart-agent twitter-stream-agent new-state :clear-actions true)
      (send-off twitter-stream-agent start-twitter-stream-action (:request-fn (meta twitter-stream-agent)) :force? false))))

(defn create-request-fn
  "creates a function of 0 params which executes a twitter streaming request"
  [twitter-method callbacks & other-params]
  (if (some #{:callbacks} other-params)
    (throw (RuntimeException. "you don't get to specify callbacks")))
  (fn []
    (log/info (<< "starting request: ~{twitter-method} with params: ~{other-params}"))
    (apply twitter-method
           :callbacks callbacks
           other-params)))


(defaction cancel-twitter-stream-action
  "cancel the stream: cancel the twitter-api http.async.client request, and set :state to :stopped"
  [twitter-stream]
  (cancel-http-async-client-response (:response twitter-stream))
  (assoc twitter-stream
    :state :stopped))


(defaction start-twitter-stream-action
  "wait as required and then create a new async streaming client.
   if state is :stopped then force? must be true, or does nothing"
  [twitter-stream request-fn & {:keys [force?] :or {force? false}}]
  (let [current-response (:response twitter-stream)
        state (:state twitter-stream)
        next-state (if force? :runnable state)
        next-connection-time (:next-connection-time twitter-stream)
        sleep-delay (if next-connection-time (.getMillis (Duration. (Instant.) next-connection-time)))]

    (if (= next-state :runnable)
      (do

        (if (http-async-response-done? current-response)

          (do ;; we're going to start a request

            (if (and sleep-delay (> sleep-delay 0))
              (do (log/debug (<< "sleeping for ~{sleep-delay}ms before starting request"))
                  (Thread/sleep sleep-delay)))
            (assoc twitter-stream
              :state next-state
              :response (request-fn)))

          (do ;; it's already running
            (log/warn "stream is still active. cancel before re-starting")
            twitter-stream)))
      (do
        (log/info "cancelled: use start-twitter-stream to restart")
        twitter-stream))))
