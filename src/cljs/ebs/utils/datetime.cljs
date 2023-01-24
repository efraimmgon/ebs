(ns ebs.utils.datetime
  (:require
   clojure.string))

(defn datetime-out [string]
  (when (seq string)
    (let [[_ time] (clojure.string/split string #"T")
          time-coll (clojure.string/split time #":")]
      (if (= 3 (count time-coll))
        (str string "Z")
        (str string ":00Z")))))

(defn datetime-in [string]
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