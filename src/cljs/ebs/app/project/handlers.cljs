(ns ebs.app.project.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.events :refer [query base-interceptors dispatch-n]]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :projects/fetch
 base-interceptors
 (fn [_ _]
   (ajax/GET "/api/projects"
     {:handler #(rf/dispatch [:assoc-in [:projects/all] %])
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywords? true})
   nil))

(rf/reg-event-fx
 :project/create
 base-interceptors
 (fn [{:keys [db]} [project]]
   (ajax/POST "/api/projects"
     {:handler #(do (rf/dispatch [:assoc-in [:project/new] nil])
                    (rf/dispatch [:navigate! :home]))
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywordize-keys true
      :params @project})
   nil))

; a handler to update a project
(rf/reg-event-fx
 :project/update
 base-interceptors
 (fn [{:keys [db]} [project]]
   (ajax/PUT (str "/api/projects/" (:id @project))
     {:handler #(do (rf/dispatch [:assoc-in [:project/active] nil])
                    (rf/dispatch [:navigate! :home]))
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywordize-keys true
      :params (dissoc @project :id)})
   nil))

(rf/reg-event-fx
 :project/load-project
 base-interceptors
 (fn [_ [project-id]]
   (ajax/GET (str "/api/projects/" project-id)
     {:handler #(rf/dispatch [:assoc-in [:project/active] %])
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywords? true})
   nil))

;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :projects/all query)
(rf/reg-sub :project/active query)