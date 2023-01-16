(ns ebs.utils.events
  (:require
   [re-frame.core :as rf]))

(def base-interceptors
  [(when ^boolean js/goog.DEBUG rf/debug)
   rf/trim-v])

(defn query [db [event]]
  (get db event))

(defn <sub [query-v]
  (deref (rf/subscribe query-v)))

(defn dispatch-n [& events]
  (doseq [evt events]
    (rf/dispatch evt)))