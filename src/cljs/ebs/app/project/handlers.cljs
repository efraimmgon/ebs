(ns ebs.app.project.handlers
  (:require
   [ebs.utils.db :as db]
   [ebs.utils.events :refer [query base-interceptors]]
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

;;; ---------------------------------------------------------------------------
;;; Events


(rf/reg-event-fx
 :projects/load-success
 base-interceptors
 (fn [{:keys [db]} [projects]]
   {:db (assoc db :projects/all (map #(js->clj % :keywordize-keys true) projects))}))


(rf/reg-event-fx
 :projects/load
 base-interceptors
 (fn [_ _]
   (let [current-user (rf/subscribe [:identity])]
     (db/get-all-projects-by-user
      {:user_id (oops/oget @current-user "uid")
       :on-success #(rf/dispatch [:projects/load-success %])})
     nil)))


(rf/reg-event-fx
 :project/create-success
 base-interceptors
 (fn [{:keys [db]} [project-id]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id project-id}]
    :db (dissoc db :project/new)}))


(rf/reg-event-fx
 :project/create!
 base-interceptors
 (fn [_ [project]]
   (let [current-user (rf/subscribe [:identity])]
     (db/create-project
      {:params (-> @project
                   (select-keys [:title :description])
                   (assoc :user_id (oops/oget @current-user "uid")))
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
 (fn [_ [project]]
   (db/update-project
    {:project-id (:id @project)
     :params (-> @project
                 (select-keys [:title :description]))
     :on-success #(rf/dispatch [:project/update-success %])})
   nil))


(rf/reg-event-fx
 :project/load-project-success
 base-interceptors
 (fn [{:keys [db]} [project]]
   {:db (assoc db :project/active (js->clj project :keywordize-keys true))}))


(rf/reg-event-fx
 :project/load-project
 base-interceptors
 (fn [_ [project-id]]
   (db/get-project-by-id
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
   (db/delete-project
    {:project-id project-id
     :on-success #(rf/dispatch [:project/delete-success %])})
   nil))


;;; ---------------------------------------------------------------------------
;;; Subscriptions


(rf/reg-sub :projects/all query)
(rf/reg-sub :project/active query)
(rf/reg-sub :project/new query)