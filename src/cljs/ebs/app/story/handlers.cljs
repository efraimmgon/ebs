(ns ebs.app.story.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

;;; ---------------------------------------------------------------------------
;;; Events

(rf/reg-event-fx
 :stories/load-success
 events/base-interceptors
 (fn [{:keys [db]} [stories]]
   (let [stories (map #(cond-> %
                         :labels (update :labels set))
                      stories)]
     {:db (assoc db :stories/all stories)})))

(rf/reg-event-fx
 :stories/load
 events/base-interceptors
 (fn [_ [project-id]]
   {:http-xhrio {:method :get
                 :uri (str "/api/projects/" project-id "/stories")
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:stories/load-success]
                 :on-failure [:common/set-error]}}))

(rf/reg-event-fx
 :story/load-success
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   (let [story (cond-> story
                 :labels (update :labels set))]
     {:db (assoc db :story/active story)})))

(rf/reg-event-fx
 :story/load
 events/base-interceptors
 (fn [_ [project-id story-id]]
   {:http-xhrio {:method :get
                 :uri (str "/api/projects/" project-id "/stories/" story-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:story/load-success]
                 :on-failure [:common/set-error]}}))

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


(rf/reg-event-fx
 :story/update-success
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id (:project_id story)}]
    :db (dissoc db :story/active)}))

(rf/reg-event-fx
 :story/update
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   (let [project-id (get-in db [:project/active :id])
         story-id (get-in db [:story/active :id])]
     {:http-xhrio {:method :put
                   :uri (str "/api/projects/" project-id "/stories/" story-id)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params (assoc story :project_id project-id)
                   :on-success [:story/update-success]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :story/delete-success
 events/base-interceptors
 (fn [{:keys [db]} _]
   {:dispatch [:navigate! :project/view-stories
               {:project-id (get-in db [:project/active :id])}]
    :db (dissoc db :story/active)}))

(rf/reg-event-fx
 :story/delete
 events/base-interceptors
 (fn [_ [project-id story-id]]
   {:http-xhrio {:method :delete
                 :uri (str "/api/projects/" project-id "/stories/" story-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:story/delete-success]
                 :on-failure [:common/set-error]}}))


;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :stories/all events/query)
(rf/reg-sub :story/active events/query)
(rf/reg-sub :story/new events/query)

(rf/reg-sub
 :stories/pending
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "pending" (:status %)) stories)))

(rf/reg-sub
 :stories/in-progress
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "in progress" (:status %)) stories)))

(rf/reg-sub
 :stories/completed
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "completed" (:status %)) stories)))