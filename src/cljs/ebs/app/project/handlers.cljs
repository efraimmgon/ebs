(ns ebs.app.project.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.db :as udb]
   [ebs.utils.events :refer [query base-interceptors js->edn]]
   [re-frame.core :as rf]))


(defn project->in
  "Coerce project data for the views."
  [project]
  (cond-> project
    :created_at (datetime/update-datetime-in :created_at)
    :updated_at (datetime/update-datetime-in :updated_at)))


(defn project->out
  "Coerce project data for the API."
  [project]
  (cond-> project
    :created_at (datetime/update-datetime-out :created_at)
    :updated_at (datetime/update-datetime-out :updated_at)))



;;; ---------------------------------------------------------------------------
;;; Events


(rf/reg-event-fx
 :projects/load-success
 base-interceptors
 (fn [{:keys [db]} [projects]]
   {:db (assoc db :projects/all
               (map js->edn projects))}))


(rf/reg-event-fx
 :projects/load
 base-interceptors
 (fn [_ _]
   (let [current-user (rf/subscribe [:identity])]
     {:dispatch [:db/get-all-projects-by-user
                 {:user-id (get @current-user "uid")
                  :on-success #(rf/dispatch [:projects/load-success %])}]})))


(rf/reg-event-fx
 :project/create-success
 base-interceptors
 (fn [{:keys [db]} [project]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id (:id project)}]
    :db (dissoc db :project/new)}))


(rf/reg-event-fx
 :project/create!
 base-interceptors
 (fn [_ [project]]
   {:http-xhrio {:method :post
                 :uri "/api/projects"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:project/create-success]
                 :on-failure [:common/set-error]
                 :params @project}}))


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
   {:http-xhrio {:method :put
                 :uri (str "/api/projects/" (:id @project))
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:project/update-success]
                 :on-failure [:common/set-error]
                 :params (project->out @project)}}))


(rf/reg-event-fx
 :project/load-project-success
 base-interceptors
 (fn [{:keys [db]} [project]]
   {:db (assoc db :project/active
               (project->in project))}))


(rf/reg-event-fx
 :project/load-project
 base-interceptors
 (fn [_ [project-id]]
   {:http-xhrio {:method :get
                 :uri (str "/api/projects/" project-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:project/load-project-success]
                 :on-failure [:common/set-error]}}))


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
   {:http-xhrio {:method :delete
                 :uri (str "/api/projects/" project-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:project/delete-success]
                 :on-failure [:common/set-error]}}))



;;; ---------------------------------------------------------------------------
;;; Subscriptions


(rf/reg-sub :projects/all query)
(rf/reg-sub :project/active query)
(rf/reg-sub :project/new query)