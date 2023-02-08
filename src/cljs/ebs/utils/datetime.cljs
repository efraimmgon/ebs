(ns ebs.utils.datetime
  (:require
   clojure.string))

(defn datetime-out
  "Converts a string to a format that can be parsed by the server"
  [string]
  (when (seq string)
    (let [[_ time] (clojure.string/split string #"T")
          time-coll (clojure.string/split time #":")]
      (if (= 3 (count time-coll))
        (str string "Z")
        (str string ":00Z")))))

(defn datetime-in
  "Converts a string to a format that can be parsed by the client"
  [string]
  (if (clojure.string/includes? string ".")
    (-> string (clojure.string/split #"\.") first)
    (->> string drop-last (apply str))))

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