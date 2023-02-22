(ns ebs.routes.services.interval
  (:require
   [clojure.spec.alpha :as s]
   [ebs.utils.fsdb :as fsdb]
   [java-time.api :as jt]
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
;;; AUXILIARY FUNCTIONS
;;; ----------------------------------------------------------------------------

(defn intervals-for-task
  "Get all intervals for a given task id."
  [task-id]
  (if-let [rows (seq (fsdb/select :interval {:where #(= (:task_id %) task-id)}))]
    rows
    []))

(defn interval-duration
  "Get the duration of an interval in milliseconds."
  [interval]
  (if (seq interval)
    (let [start (:start interval)
          end (:end interval)]
      (jt/time-between start end :millis))
    0))

(defn task-intervals->time-elapsed
  [task-id]
  (->> task-id
       intervals-for-task
       (map interval-duration)
       (apply +)))

;;; ----------------------------------------------------------------------------
;;; ROUTES
;;; ----------------------------------------------------------------------------

(defn get-task-intervals
  "Get all intervals for a given task id."
  [task-id]
  (if-let [rows (seq (intervals-for-task task-id))]
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
  (map #(assoc % :duration (interval-duration %)) (intervals-for-task 35))
  (fsdb/create-table! :interval))