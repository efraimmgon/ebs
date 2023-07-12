(ns ebs.app.task.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.db :as db]
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
                           (js->clj tasks :keywordize-keys true))]
     {:db (assoc db :tasks/tree tasks-map)})))


(rf/reg-event-fx
 :story/load-tasks
 events/base-interceptors
 (fn [_ [project-id story-id]]
   (db/get-tasks-by-story
    {:project-id project-id
     :story-id story-id
     :on-success #(rf/dispatch [:story/load-tasks-success %])})
   nil))


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
   (let [project-id (:id @(rf/subscribe [:project/active]))
         new-task (dissoc task :id)
         new-task (if (and current_estimate (not original_estimate))
                    (assoc new-task :original_estimate current_estimate)
                    new-task)]
     (db/create-task!
      {:project-id project-id
       :story-id (:story_id task)
       :params new-task
       :on-success #(rf/dispatch [:task/create-success id %])})
     nil)))


(rf/reg-event-fx
 :task/update-success
 events/base-interceptors
 (fn [{:keys [db]} [task]]
   {:db (assoc-in db [:tasks/tree (:id task)] task)}))


(rf/reg-event-fx
 :task/update!
 events/base-interceptors
 (fn [_ [{:keys [id original_estimate current_estimate] :as task}]]
   (let [keys_ [:id :story_id :title :status :current_estimate :original_estimate]
         new-task (select-keys task keys_)
         new-task (if (and current_estimate (not original_estimate))
                    (assoc new-task :original_estimate current_estimate)
                    new-task)]
     (db/update-task!
      {:project-id (:id @(rf/subscribe [:project/active]))
       :story-id (:story_id task)
       :task-id id
       :params new-task
       :on-success #(rf/dispatch [:task/update-success %])})
     nil)))


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
     (when-not (temporary-id? task-id)
       (db/delete-task!
        {:project-id (:id @(rf/subscribe [:project/active]))
         :story-id (:story_id (get-in db [:tasks/tree task-id]))
         :task-id task-id
         :on-success #(rf/dispatch [:task/delete-success %])}))
     {:db new-db})))


;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :tasks/tree events/query)
(rf/reg-sub :task/temporary events/query)

(rf/reg-sub
 :tasks/all
 :<- [:tasks/tree]
 (fn [tasks-map]
   (->> tasks-map
        vals)))
        ;(sort-by :id))))

(rf/reg-sub
 :tasks/pending
 :<- [:tasks/all]
 (fn [tasks]
   (filter #(= "pending" (:status %)) tasks)))