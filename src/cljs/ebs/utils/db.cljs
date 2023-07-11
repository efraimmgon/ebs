(ns ebs.utils.db
  (:require
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

(defn now []
  (firestore/serverTimestamp))

(defn add-id [docRef params]
  (oops/oset! params "!id" (oops/oget docRef "id")))

(defn add-timestamps [params]
  (let [now (now)]
    (oops/oset! params "!created_at" now)
    (oops/oset! params "!updated_at" now)))

(defn update-timestamp [params]
  (oops/oset! params "!updated_at" (now)))

(defn prepare-input [docRef params]
  (-> (add-id docRef params)
      add-timestamps))

(comment
  (def fdb (rf/subscribe [:firestore/db]))



  :end)
  