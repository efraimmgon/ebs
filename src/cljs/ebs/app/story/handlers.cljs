(ns ebs.app.story.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

; (1) The checkbox input expects a set of labels, but the API returns a vector.
(defn story->in
  "Coerce story data for the views."
  [story]
  (cond-> story
    :labels (update :labels set) ; (1)
    :due_date (datetime/update-datetime-in :due_date)
    :created_at (datetime/update-datetime-in :created_at)
    :updated_at (datetime/update-datetime-in :updated_at)))

(defn story->out
  "Coerce story data for the API."
  [story]
  (cond-> story
    :due_date (datetime/update-datetime-out :due_date)
    :created_at (datetime/update-datetime-out :created_at)
    :updated_at (datetime/update-datetime-out :updated_at)))

;;; ---------------------------------------------------------------------------
;;; Events

(rf/reg-event-fx
 :stories/load-success
 events/base-interceptors
 (fn [{:keys [db]} [stories]]
   (let [stories (->> stories
                      (map story->in)
                      (sort-by (juxt :priority :created_at) <))]
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
   (let [story (story->in story)]
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
   {:dispatch-n [[:remove-modal]
                 [:assoc-in [:story/new] nil]
                 [:update-in [:stories/all] conj story]]}))

(rf/reg-event-fx
 :story/create!
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   (let [project-id (get-in db [:project/active :id])]
     (prn :story story)
     {:http-xhrio {:method :post
                   :uri (str "/api/projects/" project-id "/stories")
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params (-> story
                               story->out
                               (assoc :project_id project-id))
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
 :story/update!
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   (let [project-id (get-in db [:project/active :id])
         story-id (get-in db [:story/active :id])]
     {:http-xhrio {:method :put
                   :uri (str "/api/projects/" project-id "/stories/" story-id)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params (-> story
                               story->out
                               (assoc :project_id project-id))
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
 :story/delete!
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
(rf/reg-sub :stories/show-complete? events/query)
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
 :stories/complete
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "complete" (:status %)) stories)))

