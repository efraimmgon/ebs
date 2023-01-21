(ns ebs.routes.services.task
  (:require
   [clojure.spec.alpha :as s]
   [ebs.routes.services.common :as common]
   [ebs.utils.fsdb :as fsdb]
   [ring.util.http-response :as response]))

;;; ---------------------------------------------------------------------------
;;; DOMAIN

; - A task belongs to a story.
; - A task has an id.
; - a task has a story_id
; - A task has a title.
; - A task has a due date.
; - A task has a original estimate, a current estimate, a elapsed time, 
; and a remaining time (time is measured in minutes).
; - A task has many comments.

(s/def :task/id int?)
(s/def :task/story_id int?)
(s/def :task/title string?)
(s/def :task/description string?)
(s/def :task/status string?)
(s/def :task/due_date inst?)
(s/def :task/original_estimate int?)
(s/def :task/current_estimate int?)
(s/def :task/elapsed_time int?)
(s/def :task/remaining_time int?)
(s/def :task/created_at inst?)
(s/def :task/updated_at inst?)

(s/def :task/Task
  (s/keys :req-un [:task/id
                   :task/story_id
                   :task/title
                   :task/status
                   :task/created_at
                   :task/updated_at]
          :opt-un [:task/description
                   :task/due_date
                   :task/original_estimate
                   :task/current_estimate
                   :task/elapsed_time
                   :task/remaining_time]))

(s/def :task/NewTask
  (s/keys :req-un [:task/story_id
                   :task/title
                   :task/status]
          :opt-un [:task/description
                   :task/due_date
                   :task/original_estimate
                   :task/current_estimate
                   :task/elapsed_time
                   :task/remaining_time]))

(s/def :task/UpdateTask
  (s/keys :req-un [:task/id
                   :task/story_id]
          :opt-un [:task/title
                   :task/description
                   :task/status
                   :task/due_date
                   :task/original_estimate
                   :task/current_estimate
                   :task/elapsed_time
                   :task/remaining_time]))


;;; ---------------------------------------------------------------------------
;;; ROUTES

; get-task
; get-tasks
; create-task
; update-task
; delete-task

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

(defn delete-task!
  "Delete a task record by id."
  [id]
  (if-let [task (fsdb/get-by-id :task id)]
    (do
      (fsdb/delete! :task id)
      (response/ok {:result :ok}))
    (response/not-found {:result {:message "Task not found."}})))

(comment
  (fsdb/create-table! :task))