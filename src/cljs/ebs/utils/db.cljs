(ns ebs.utils.db
  (:require
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

(defn create
  [{:keys [collection params]}]
  (let [fdb (rf/subscribe [:firestore/db])
        docRef (firestore/addDoc
                (firestore/collection
                 @fdb collection)
                (clj->js params))]
    (oops/oget docRef "id")))


(defn get-by-id
  [{:keys [collection id]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (firestore/getDoc
     (firestore/doc
      (str collection "/" id)))))



(comment
  (def fdb (rf/subscribe [:firestore/db]))



  :end)
  