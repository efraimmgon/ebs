(ns ebs.app.task.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (contains? @(rf/subscribe [:task/temporary]) id))

(defn remove-temporary-task
  "Removes a temporary task from the db."
  [db id]
  (-> db
      (update :task/temporary disj id)
      (update :tasks/tree dissoc id)))

(defn remove-task
  "Removes a task from the db."
  [db id]
  (-> db
      (update :tasks/tree dissoc id)))

;;; ---------------------------------------------------------------------------
;;; Handlers

(rf/reg-event-fx
 :story/load-tasks-success
 events/base-interceptors
 (fn [{:keys [db]} [tasks]]
   (let [tasks-map (reduce (fn [acc task]
                             (assoc acc (:id task) task))
                           {}
                           tasks)]
     {:db (assoc db :tasks/tree tasks-map)})))

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
              (assoc-in [:tasks/tree id]
                        {:id id
                         :story_id story-id
                         :status "pending"
                         :title ""})
              (update :task/temporary (fnil conj #{}) id))})))

(rf/reg-event-fx
 :task/create-success
 events/base-interceptors
 (fn [{:keys [db]} [temp-id task]]
   {:db (-> db
            (remove-temporary-task temp-id)
            (assoc-in [:tasks/tree (:id task)]
                      task))}))

(rf/reg-event-fx
 :task/create!
 events/base-interceptors
 (fn [_ [{:keys [id original_estimate current_estimate] :as task}]]
   (let [new-task (dissoc task :id)
         new-task (if (and current_estimate (not original_estimate))
                    (assoc new-task :original_estimate current_estimate)
                    new-task)]
     {:http-xhrio {:method :post
                   :uri "/api/tasks"
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params new-task
                   :on-success [:task/create-success id]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :task/update-success
 events/base-interceptors
 (fn [{:keys [db]} [task]]
   {:db (assoc-in db [:tasks/tree (:id task)] task)}))

(rf/reg-event-fx
 :task/update!
 events/base-interceptors
 (fn [_ [{:keys [id original_estimate current_estimate] :as task}]]
   (let [ks [:id :story_id :title :status :current_estimate :original_estimate]
         new-task (select-keys task ks)
         new-task (if (and current_estimate (not original_estimate))
                    (assoc new-task :original_estimate current_estimate)
                    new-task)]
     {:http-xhrio {:method :put
                   :uri (str "/api/tasks/" id)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params new-task
                   :on-success [:task/update-success]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :task/delete-success
 events/base-interceptors
 (fn [_ _]
   nil))

(rf/reg-event-fx
 :task/delete!
 events/base-interceptors
 (fn [{:keys [db]} [task-id]]
   (let [new-db (if (temporary-id? task-id)
                  (remove-temporary-task db task-id)
                  (remove-task db task-id))]
     (if (temporary-id? task-id)
       {:db new-db}
       {:http-xhrio {:method :delete
                     :uri (str "/api/tasks/" task-id)
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:task/delete-success task-id]
                     :on-failure [:common/set-error]}
        :db new-db}))))

;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :tasks/tree events/query)
(rf/reg-sub :task/temporary events/query)

(rf/reg-sub
 :tasks/all
 :<- [:tasks/tree]
 (fn [tasks-map]
   (->> tasks-map
        vals
        (sort-by :id))))

(rf/reg-sub
 :tasks/pending
 :<- [:tasks/all]
 (fn [tasks]
   (filter #(= "pending" (:status %)) tasks)))