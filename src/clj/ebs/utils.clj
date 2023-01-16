(ns ebs.utils
  (:require
   [java-time :as jt]))

(defn string->date [s]
  (if (string? s)
    (let [[date time] (clojure.string/split s #"T")
          [y mon d] (clojure.string/split date #"-")
          [h min s] (clojure.string/split time #":")]
      (apply jt/local-date-time
             (map #(Integer/parseInt %) (list y mon d h min))))
    s))
#_(str (string->date "2020-04-15T20:08:37.752274"))
