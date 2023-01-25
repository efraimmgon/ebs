(ns ebs.routes.services.task
  (:require
   [clojure.spec.alpha :as s]
   [ebs.routes.services.common :as common]
   [ebs.utils.fsdb :as fsdb]
   [ring.util.http-response :as response]))

;;; ---------------------------------------------------------------------------
;;; DOMAIN

; An interval is a time period between two timestamps. It is represented by a
; map with a start and end key.

(s/def :interval/id int?)
(s/def :interval/task_id int?)
(s/def :interval/start inst?)
(s/def :interval/end inst?)


; - A task belongs to a story.
; - A task has an id.
; - A task has a story_id
; - A task has a title.
; - A task has a status. ("pending" "complete")
; - A task has a original estimate, a current estimate, a elapsed time.

(s/def :task/id int?)
(s/def :task/story_id int?)
(s/def :task/title string?)
(s/def :task/status string?)
;; The user's first estimate.
(s/def :task/original_estimate int?)
;; The user's current (updated, if needed) estimate.
(s/def :task/current_estimate int?)
;; The time the user has spent on the task so far.
(s/def :task/elapsed_time int?)
;; When you divide estimate by actual time, you get velocity: how fast the  
;; task was done relative to estimate. (/ original_estimate elapsed_time)
(s/def :task/velocity float?)
(s/def :task/created_at inst?)
(s/def :task/updated_at inst?)

(s/def :task/Task
  (s/keys :req-un [:task/id
                   :task/story_id
                   :task/title
                   :task/status
                   :task/created_at
                   :task/updated_at]
          :opt-un [:task/original_estimate
                   :task/current_estimate
                   :task/elapsed_time
                   :task/velocity]))

(s/def :task/NewTask
  (s/keys :req-un [:task/story_id
                   :task/title
                   :task/status]
          :opt-un [:task/original_estimate
                   :task/current_estimate
                   :task/elapsed_time]))

(s/def :task/UpdateTask
  (s/keys :req-un [:task/id
                   :task/story_id]
          :opt-un [:task/title
                   :task/status
                   :task/current_estimate
                   :task/elapsed_time
                   :task/velocity]))

(s/def :task/tasks (s/* :task/Task))

;;; ---------------------------------------------------------------------------
;;; ROUTES

(defn get-tasks
  "Return all task records."
  [_]
  (response/ok
   (if-let [tasks (seq (fsdb/get-all :task))]
     tasks
     [])))

(defn get-story-tasks
  "Return all task records for a story."
  [story_id]
  (response/ok
   (if-let [tasks (seq (fsdb/select :task {:where #(= (:story_id %) story_id)}))]
     tasks
     [])))

(defn get-task
  "Return a task record by id."
  [id]
  (if-let [task (fsdb/get-by-id :task id)]
    (response/ok task)
    (response/not-found {:result {:message "Task not found."}})))

(defn create-task!
  "Create a task record in the db. Returns the created task."
  [task]
  (let [now (common/now)]
    (response/ok
     (fsdb/create! :task
                   (assoc task
                          :created_at now
                          :updated_at now)))))

(defn update-task!
  "Update a task record in the db. Returns the updated task."
  [task]
  (prn :task task)
  (if-let [old-task (fsdb/get-by-id :task (:id task))]
    (response/ok
     (fsdb/update! :task
                   (merge old-task
                          (assoc task
                                 :updated_at (common/now)))))
    (response/not-found {:result {:message "Task not found."}})))

(defn delete-task!
  "Delete a task record by id."
  [id]
  (if (seq (fsdb/get-by-id :task id))
    (do
      (fsdb/delete! :task id)
      (response/ok {:result :ok}))
    (response/not-found {:result {:message "Task not found."}})))

(comment
  (fsdb/create-table! :task))