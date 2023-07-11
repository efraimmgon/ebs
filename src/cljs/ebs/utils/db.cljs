(ns ebs.utils.db
  (:require
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

(defn now []
  (firestore/serverTimestamp))


(defn add-timestamps [params]
  (let [now (now)]
    (-> params
        (assoc :created_at now)
        (assoc :updated_at now))))


(defn update-timestamp [params]
  (assoc params :updated_at (now)))


(defn prepare-input [docRef params]
  (-> params
      (assoc :id (oops/oget docRef "id"))
      add-timestamps))

