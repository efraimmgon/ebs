(ns ebs.utils.datetime
  (:require
   clojure.string
   [cljs-time.core :as time]
   [cljs-time.format :as tf]))

(def iso-zoned-date (tf/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZ"))

(def datetime-local (tf/formatter "yyyy-MM-dd'T'HH:mm"))

(defn to-datetime-local-string [datetime]
  (tf/unparse datetime-local datetime))

(defn to-datetime-local [string]
  (->> string
       (tf/parse-local datetime-local)))

(defn datetime-out
  "Converts a goog.date.DateTime obj to a format that can be parsed by 
   the server"
  [datetime-obj]
  (->> datetime-obj
       time/to-utc-time-zone
       (tf/unparse iso-zoned-date)))

(defn datetime-in
  "Converts a string to a format that can be parsed by the client"
  [string]
  (->> string
       (tf/parse iso-zoned-date)
       time/to-default-time-zone))

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
  "Converts a string to a user friendly format"
  [string]
  (.toUTCString (js/Date. (str string "Z"))))

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

(defn unparse [date]
  (tf/unparse (tf/formatters :date-time-no-ms) date))


(comment
  (tf/show-formatters)
  (->> "2023-01-22T22:09:08.455Z"
       (tf/parse iso-zoned-date)
       time/to-default-time-zone
       time/to-utc-time-zone
       .toUTCRfc3339String)
  (tf/parse (tf/formatters :date-time
                           "2023-01-22T22:09:08.455Z"))

  (unparse (time/to-default-time-zone (time/now)))
  (time/from-default-time-zone (time/now))
  (time/default-time-zone))

