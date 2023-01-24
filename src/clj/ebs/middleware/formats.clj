(ns ebs.middleware.formats
  (:require
   [java-time.api :as jt]
   [luminus-transit.time :as time]
   [muuntaja.core :as m]))

(def instance
  (m/create
   (-> m/default-options
       (update-in
        [:formats "application/transit+json" :decoder-opts]
        (partial merge time/time-deserialization-handlers))
       (update-in
        [:formats "application/transit+json" :encoder-opts]
        (partial merge time/time-serialization-handlers)))))

(defn read-instant-timestamp
  "Reads a timestamp in ISO-8601 format into a java.time.Instant."
  [string]
  (java.time.Instant/parse string))