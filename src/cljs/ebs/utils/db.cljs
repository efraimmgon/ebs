(ns ebs.utils.db
  (:require
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

;;; ---------------------------------------------------------------------------
;;; Utilities
;;; ---------------------------------------------------------------------------


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

;;; ---------------------------------------------------------------------------
;;; Firestore DB
;;; ---------------------------------------------------------------------------

;;; ---------------------------------------------------------------------------
;;; Projects

(defn get-all-projects-by-user
  [{:keys [user_id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/collection @fdb "projects")
        (firestore/query (firestore/where "user_id" "==" user_id))
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map (fn [doc]
                             (oops/ocall doc "data")))
                     on-success))))))


(defn get-project-by-id
  [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/doc @fdb "projects" project-id)
        firestore/getDoc
        (.then (fn [^js docSnapshot]
                 (let [data (oops/ocall docSnapshot "data")]
                   (on-success data)))))))


(defn create-project [{:keys [params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        docRef (-> (firestore/collection @fdb "projects") firestore/doc)
        params (prepare-input docRef params)]
    (-> (firestore/setDoc docRef (clj->js params))
        (.then #(on-success params)))))


(defn update-project [{:keys [project-id params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        params (update-timestamp params)]
    (-> (firestore/doc @fdb "projects" project-id)
        (firestore/updateDoc (clj->js params))
        (.then #(on-success params)))))


(defn delete-project [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/doc @fdb "projects" project-id)
        firestore/deleteDoc
        (.then #(on-success project-id)))))


;;; ---------------------------------------------------------------------------
;;; Stories

(defn get-all-stories-by-project
  [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/collection @fdb "projects" project-id "stories")
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map (fn [doc]
                             (oops/ocall doc "data")))
                     on-success))))))

(defn create-story
  [{:keys [params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        docRef (-> (firestore/collection @fdb "projects" (:project_id params) "stories")
                   firestore/doc)
        params (prepare-input docRef params)]
    (-> docRef
        (firestore/setDoc (clj->js params))
        (.then #(on-success params)))))
