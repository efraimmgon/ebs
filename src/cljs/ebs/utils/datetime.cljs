(ns ebs.utils.datetime
  (:require
   clojure.string
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [oops.core :as oops]
   ["firebase/firestore" :as firestore]))


(def iso-zoned-date (tf/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZ"))

(def datetime-local (tf/formatter "yyyy-MM-dd'T'HH:mm"))

(def user-friendly-fmt (tf/formatter "yyyy-MM-dd HH:mm"))

(defn to-datetime-local-string [datetime]
  (tf/unparse datetime-local datetime))

(defn to-datetime-local [string]
  (->> string
       (tf/parse-local datetime-local)))


(defn string->firestore-timestamp
  [string]
  (let [d (js/Date. string)]
    (firestore/Timestamp.fromDate d)))


(defn datetime-out
  "Converts a goog.date.DateTime obj to a format that can be parsed by 
   the server"
  [datetime-obj]
  (->> datetime-obj
       time/to-utc-time-zone
       (tf/unparse iso-zoned-date)))

(defn truncate-ms
  "Truncates the milliseconds from a string"
  [string]
  (when (and string
             (not (clojure.string/blank? string)))
    (let [[left ms] (clojure.string/split string #"\.")]
      (cond (empty? ms)
            (str (apply str (drop-last left))
                 ".000Z")

            :else (str left ".000Z")))))

(defn datetime-in
  "Converts a string to a format that can be parsed by the client"
  [string]
  (->> string
       truncate-ms
       (tf/parse iso-zoned-date)
       time/to-default-time-zone))


(defn firestore->datetime-input-fmt
  [firestore-timestamp]
  (when firestore-timestamp
    (-> firestore-timestamp
        (oops/ocall "toDate")
        .toJSON
        datetime-in
        to-datetime-local-string)))


(defn update-datetime-in [m k]
  (let [v (get m k)]
    (cond (not (contains? m k)) m
          (or (clojure.string/blank? v) (nil? v)) (dissoc m k)
          :else (update m k datetime-in))))

(defn update-datetime-out [m k]
  (let [v (get m k)]
    (cond (not (contains? m k)) m
          (or (clojure.string/blank? v) (nil? v)) (dissoc m k)
          :else (update m k datetime-out))))

(defn datetime-ui
  "Converts a goog.date.DateTime obj to a user friendly format"
  [datetime-obj]
  (tf/unparse user-friendly-fmt datetime-obj))

(defn min->ms
  "Convert minutes to milliseconds"
  [min]
  (* min 60 1000))

(defn min->sec
  "Convert minutes to seconds"
  [min]
  (* min 60))

(defn sec->ms
  "Convert seconds to milliseconds"
  [sec]
  (* sec 1000))

(defn ms->sec
  "Convert milliseconds to seconds"
  [ms]
  (/ ms 1000))

(defn ms->hours-mins-sec
  "Returns a map with the hours, minutes and seconds of a number of milliseconds."
  [ms]
  (let [s (quot ms 1000)
        m (quot s 60)
        h (quot m 60)]
    {:hours (mod h 24)
     :minutes (mod m 60)
     :seconds (mod s 60)}))

(defn unparse-utc-datetime [date]
  (tf/unparse iso-zoned-date date))


(comment
  (tf/show-formatters)
  (->> "2023-01-22T22:09:08.455Z"
       (tf/parse iso-zoned-date)
       time/to-default-time-zone
       time/to-utc-time-zone
       .toUTCIsoString)
  (tf/parse (tf/formatters :date-time
                           "2023-01-22T22:09:08.455Z"))

  (unparse-utc-datetime (time/to-default-time-zone (time/now)))
  (time/from-default-time-zone (time/now))
  (time/default-time-zone))

