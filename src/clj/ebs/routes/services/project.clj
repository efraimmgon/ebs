(ns ebs.routes.services.project
  (:require
   [clojure.spec.alpha :as s]
   [ebs.routes.services.common :as common]
   [ebs.utils.fsdb :as fsdb]
   [ring.util.http-response :as response]))

;;; ---------------------------------------------------------------------------
;;; DOMAIN

(s/def :project/id int?)
(s/def :project/title string?)
(s/def :project/description string?)
;(s/def :project/start-date inst?)
;(s/def :project/due-date inst?)
(s/def :project/created_at inst?)
(s/def :project/updated_at inst?)

(s/def :project/Project
  (s/keys :req-un [:project/id
                   :project/title
                   ;:project/start-date
                   ;:project/due-date
                   :project/created_at
                   :project/updated_at]
          :opt-un [:project/description]))

(s/def :project/projects (s/* :project/Project))

;;; ---------------------------------------------------------------------------
;;; ROUTES

(defn get-projects
  "Return all project records."
  [_]
  (response/ok
   (if-let [projects (seq (fsdb/get-all :project))]
     projects
     [])))

(defn get-project
  "Return a project record by id."
  [id]
  (response/ok
   (fsdb/get-by-id :project id)))

(defn create-project!
  "Create a project record in the db. Returns the created project."
  [project]
  (let [now (common/now)]
    (response/ok
     (fsdb/create! :project
                   (assoc project
                          :created_at now
                          :updated_at now)))))

(defn update-project!
  "Update a project record in the db. Returns the updated project."
  [project]
  (if-not (fsdb/get-by-id :project (:id project))
    (response/bad-request {:error :not-found})
    (response/ok
     (fsdb/update! :project
                   (assoc project
                          :updated_at (common/now))))))

(defn delete-project!
  "Delete a project record in the db."
  [id]
  (fsdb/delete! :project id)
  (response/ok {:result :ok}))