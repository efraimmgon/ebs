(ns ebs.utils.db
  (:require
   [ebs.utils.events :refer [base-interceptors query js->edn]]
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


(defn get-all-projects-by-user
  [{:keys [user-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        collRef (firestore/collection
                 @fdb "projects")
        q (firestore/query collRef (firestore/where "user_id" "==" user-id))]
    (-> q
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map (fn [doc]
                             (let [data (oops/ocall doc "data")]
                               (oops/oset! data "!id" (oops/oget doc "id")))))
                     on-success))))))


(rf/reg-event-fx
 :db/get-all-projects-by-user
 base-interceptors
 (fn [_ [{:keys [user-id on-success]}]]
   (get-all-projects-by-user {:user-id user-id
                              :on-success on-success})
   nil))


(comment
  (def fdb (rf/subscribe [:firestore/db]))

  (get-all-projects-by-user
   {:user-id "g7Ok0yCuIbYCx8Ecek1z7vSCPVt1"
    :on-success #(prn %)})

  :end)
  