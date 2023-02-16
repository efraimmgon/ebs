(ns ebs.routes.services.interval
  (:require
   [clojure.spec.alpha :as s]
   [ebs.routes.services.common :as common]
   [ebs.utils.fsdb :as fsdb]
   [ring.util.http-response :as response]))

;;; ----------------------------------------------------------------------------
;;; DOMAIN
;;; ----------------------------------------------------------------------------

(s/def :interval/id int?)
(s/def :interval/task_id int?)
(s/def :interval/start inst?)
(s/def :interval/end inst?)

(s/def :interval/Interval
  (s/keys :req-un [:interval/id
                   :interval/task_id
                   :interval/start
                   :interval/end]))

(s/def :interval/NewInterval
  (s/keys :req-un [:interval/task_id
                   :interval/start
                   :interval/end]))

(s/def :interval/intervals (s/* :interval/Interval))

;;; ----------------------------------------------------------------------------
;;; ROUTES
;;; ----------------------------------------------------------------------------

(defn get-task-intervals
  "Get all intervals for a given task id."
  [task-id]
  (if-let [rows (seq (fsdb/select :interval {:where #(= (:task_id %) task-id)}))]
    (response/ok rows)
    (response/not-found {:result {:message "No intervals found for task id."}})))

(defn get-interval-by-id
  "Get an interval by id."
  [interval-id]
  (if-let [row (fsdb/get-by-id :interval interval-id)]
    (response/ok row)
    (response/not-found {:result {:message "No interval found for id."}})))

(defn create-interval!
  "Create a new interval."
  [interval]
  (response/ok
   (fsdb/create! :interval interval)))

(defn delete-interval!
  "Delete an interval by id."
  [interval-id]
  (if (fsdb/delete! :interval interval-id)
    (response/ok {:result :ok})
    (response/not-found {:result {:message "No interval found for id."}})))

(comment
  (fsdb/create-table! :interval))