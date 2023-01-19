(ns ebs.app.story.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :stories/load
 events/base-interceptors
 (fn [_ [project-id]]
   (ajax/GET (str "/api/projects/" project-id "/stories")
     {:handler #(rf/dispatch [:assoc-in [:stories/all] %])
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywords? true})
   nil))

(rf/reg-event-fx
 :story/create-success
 events/base-interceptors
 (fn [_ [story]]
   {:dispatch-n [[:navigate! :project/view-stories
                  {:project-id (:project_id story)}]
                 [:assoc-in [:story/new] nil]]}))

(rf/reg-event-fx
 :story/create
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   (let [project-id (get-in db [:project/active :id])]
     (prn :story story)
     {:http-xhrio {:method :post
                   :uri (str "/api/projects/" project-id "/stories")
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params (assoc story :project_id project-id)
                   :on-success [:story/create-success]
                   :on-failure [:common/set-error]}})))


(rf/reg-sub :stories/all events/query)