(ns ebs.app.task.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (= id @(rf/subscribe [:task/new])))

;;; ---------------------------------------------------------------------------
;;; Handlers

; :story/load-tasks-success
(rf/reg-event-fx
 :story/load-tasks-success
 events/base-interceptors
 (fn [{:keys [db]} [tasks]]
   (let [tasks-map (reduce (fn [acc task]
                             (assoc acc (:id task) task))
                           {}
                           tasks)]
     {:db (assoc db :story/tasks-map tasks-map)})))

(rf/reg-event-fx
 :story/load-tasks
 events/base-interceptors
 (fn [_ [project-id story-id]]
   {:http-xhrio {:method :get
                 :uri (str "/api/projects/" project-id "/stories/" story-id "/tasks")
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:story/load-tasks-success]
                 :on-failure [:common/set-error]}}))

(rf/reg-event-fx
 :task/add-item
 events/base-interceptors
 (fn [{:keys [db]} [story-id]]
   (let [id (.getTime (js/Date.))]
     {:db (-> db
              (assoc-in [:story/tasks-map id]
                        {:id id
                         :story_id story-id
                         :status "pending"
                         :title ""})
              (assoc :task/new id))})))

(rf/reg-event-fx
 :task/create-success
 events/base-interceptors
 (fn [{:keys [db]} [temp-id task]]
   {:db (-> db
            (update :story/tasks-map
                    dissoc temp-id)
            (assoc-in [:story/tasks-map (:id task)]
                      task))}))

(rf/reg-event-fx
 :task/create!
 events/base-interceptors
 (fn [_ [task]]
   (let [new-task (-> task
                      (dissoc :id)
                      (assoc :original_estimate (:current_estimate task)))]
     {:http-xhrio {:method :post
                   :uri "/api/tasks"
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params new-task
                   :on-success [:task/create-success (:id task)]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :task/update-success
 events/base-interceptors
 (fn [{:keys [db]} [task]]
   {:db (assoc-in db [:story/tasks-map (:id task)] task)}))

(rf/reg-event-fx
 :task/update!
 events/base-interceptors
 (fn [_ [task]]
   (let [new-task (dissoc task
                          :original_estimate
                          :created_at
                          :updated_at)]
     {:http-xhrio {:method :put
                   :uri (str "/api/tasks/" (:id task))
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params new-task
                   :on-success [:task/update-success]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :task/delete!
 events/base-interceptors
 (fn [{:keys [db]} [task-id]]
   (let [new-db (cond-> db
                  (temporary-id? task-id) (update :story/tasks-map
                                                  dissoc task-id)
                  :always (dissoc :task/new))]
     (if (temporary-id? task-id)
       {:db new-db}
       {:http-xhrio {:method :delete
                     :uri (str "/api/tasks/" task-id)
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:task/delete-success]
                     :on-failure [:common/set-error]}
        :db new-db}))))


;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :story/tasks-map events/query)
(rf/reg-sub :task/new events/query)

(rf/reg-sub
 :story/tasks-list
 :<- [:story/tasks-map]
 (fn [tasks-map]
   (->> tasks-map
        vals
        (sort-by :id))))
