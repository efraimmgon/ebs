(ns ebs.app.project.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.db :as udb]
   [ebs.utils.events :refer [query base-interceptors js->edn]]
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))


(defn project->out
  "Coerce project data for the API."
  [project]
  (cond-> project
    :created_at (datetime/update-datetime-out :created_at)
    :updated_at (datetime/update-datetime-out :updated_at)))

(defn add-id [docRef params]
  (assoc params
         :id (oops/oget docRef "id")))

(defn add-timestamps [params]
  (let [now (firestore/serverTimestamp)]
    (assoc params
           :created_at now
           :updated_at now)))

(defn update-timestamp [params]
  (assoc params
         :updated_at (firestore/serverTimestamp)))

(defn prepare-input [docRef params]
  (-> (add-id docRef params)
      add-timestamps
      clj->js))

;;; ---------------------------------------------------------------------------
;;; DB

(defn get-all-projects-by-user
  [{:keys [user_id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/collection @fdb "projects")
        (firestore/query (firestore/where "user_id" "==" user_id))
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map (fn [doc]
                             (let [data (oops/ocall doc "data")]
                               (oops/oset! data "!id" (oops/oget doc "id")))))
                     on-success))))))


(defn get-project-by-id
  [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/doc @fdb "projects" project-id)
        firestore/getDoc
        (.then (fn [^js docSnapshot]
                 (let [data (oops/ocall docSnapshot "data")]
                   (when on-success
                     (on-success data))))))))


(defn create-project [{:keys [params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])

        docRef (-> (firestore/collection @fdb "projects") firestore/doc)
        jsdata (prepare-input docRef params)]
    (-> (firestore/setDoc docRef jsdata)
        (.then (fn [^js doc]
                 (when on-success
                   (on-success (oops/oget jsdata "id"))))))))


(defn update-project [{:keys [project-id params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        params (assoc params "updated_at" (firestore/serverTimestamp))]
    (-> (firestore/doc @fdb "projects" project-id)
        (firestore/updateDoc (-> params update-timestamp clj->js))
        (.then (fn [^js docRef]
                 (when on-success
                   (on-success (oops/oget docRef "id"))))))))

(defn delete-project [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/doc @fdb "projects" project-id)
        firestore/deleteDoc
        (.then (fn [^js docRef]
                 (when on-success
                   (on-success (oops/oget docRef "id"))))))))

;;; ---------------------------------------------------------------------------
;;; Events


(rf/reg-event-fx
 :projects/load-success
 base-interceptors
 (fn [{:keys [db]} [projects]]
   {:db (assoc db :projects/all projects)}))


(rf/reg-event-fx
 :projects/load
 base-interceptors
 (fn [_ _]
   (let [current-user (rf/subscribe [:identity])]
     (get-all-projects-by-user
      {:user_id (get @current-user "uid")
       :on-success #(rf/dispatch [:projects/load-success %])})
     nil)))


(rf/reg-event-fx
 :project/create-success
 base-interceptors
 (fn [{:keys [db]} [project_id]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id project_id}]
    :db (dissoc db :project/new)}))


(rf/reg-event-fx
 :project/create!
 base-interceptors
 (fn [_ [project]]
   (let [current-user (rf/subscribe [:identity])]
     (create-project
      {:params (assoc @project "user_id" (get @current-user "uid"))
       :on-success #(rf/dispatch [:project/create-success %])})
     nil)))


(rf/reg-event-fx
 :project/update-success
 base-interceptors
 (fn [{:keys [db]} _]
   {:dispatch [:navigate! :home]
    :db (dissoc db :project/active)}))


(rf/reg-event-fx
 :project/update!
 base-interceptors
 (fn [{:keys [db]} [project]]
   (update-project
    {:project-id (:id @project)
     :params (project->out @project)
     :on-success #(rf/dispatch [:project/update-success %])})
   nil))


(rf/reg-event-fx
 :project/load-project-success
 base-interceptors
 (fn [{:keys [db]} [project]]
   {:db (assoc db :project/active (js->edn project))}))


(rf/reg-event-fx
 :project/load-project
 base-interceptors
 (fn [_ [project-id]]
   (get-project-by-id
    {:project-id project-id
     :on-success #(rf/dispatch [:project/load-project-success %])})
   nil))


(rf/reg-event-fx
 :project/delete-success
 base-interceptors
 (fn [{:keys [db]} _]
   {:dispatch [:navigate! :home]
    :db (dissoc db :project/active)}))


(rf/reg-event-fx
 :project/delete!
 base-interceptors
 (fn [_ [project-id]]
   (delete-project
    {:project-id project-id
     :on-success #(rf/dispatch [:project/delete-success %])})
   nil))


;;; ---------------------------------------------------------------------------
;;; Subscriptions


(rf/reg-sub :projects/all query)
(rf/reg-sub :project/active query)
(rf/reg-sub :project/new query)