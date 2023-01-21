(ns ebs.routes.services.story
  (:require
   [clojure.spec.alpha :as s]
   [ebs.routes.services.common :as common]
   [ebs.utils.fsdb :as fsdb]
   [ring.util.http-response :as response]))

;;; ---------------------------------------------------------------------------
;;; DOMAIN

; - A story belongs to a project.
; - A story has an id.
; - A story has a title.
; - A story has a description.
; - A story has a status.
; - A story has a priority. A lower number is higher priority.
; - A story has a due date.
; - A story has many labels.
; - A story has many tasks.
; - A story has many comments.

(s/def :story/id int?)
(s/def :story/project_id int?)
(s/def :story/title string?)
(s/def :story/description string?)
(s/def :story/status string?)
(s/def :story/labels (s/* string?))
(s/def :story/priority int?)
(s/def :story/due_date inst?)
(s/def :story/created_at inst?)
(s/def :story/updated_at inst?)

(s/def :story/Story
  (s/keys :req-un [:story/id
                   :story/project_id
                   :story/title
                   :story/status
                   :story/created_at
                   :story/updated_at]
          :opt-un [:story/description
                   :story/labels
                   :story/due_date
                   :story/priority]))

(s/def :story/NewStory
  (s/keys :req-un [:story/project_id
                   :story/title
                   :story/status]
          :opt-un [:story/description
                   :story/labels
                   :story/due_date
                   :story/priority]))

(s/def :story/UpdateStory
  (s/keys :req-un [:story/id
                   :story/project_id]
          :opt-un [:story/title
                   :story/description
                   :story/status
                   :story/labels
                   :story/due_date
                   :story/priority
                   :story/created_at
                   :story/updated_at]))

(s/def :story/stories (s/* :story/Story))

;;; ---------------------------------------------------------------------------
;;; ROUTES

(defn get-stories
  "Return all story records."
  [_]
  (response/ok
   (if-let [stories (seq (fsdb/get-all :story))]
     stories
     [])))

(defn get-project-stories
  "Return all story records for a project."
  [project_id]
  (let [stories
        (fsdb/select :story {:where #(= (:project_id %) project_id)})]
    (response/ok
     (if (seq stories)
       stories
       []))))

(defn get-story
  "Return a story record by id."
  [id]
  (response/ok
   (fsdb/get-by-id :story id)))

(defn create-story!
  "Create a story record in the db. Returns the created story."
  [story]
  (let [now (common/now)]
    (response/ok
     (fsdb/create! :story
                   (assoc story
                          :created_at now
                          :updated_at now)))))

(defn update-story!
  "Update a story record in the db. Returns the updated story."
  [story]
  (let [now (common/now)
        old (fsdb/get-by-id :story (:story/id story))]
    (response/ok
     (fsdb/update! :story
                   (assoc (merge old story)
                          :updated_at now)))))

(defn delete-story!
  "Delete a story record in the db. Returns the deleted story."
  [id]
  (fsdb/delete! :story id)
  (response/ok {:result :ok}))

(comment
  (fsdb/create-table! :story)
  (get-project-stories 5)
  (fsdb/get-all :story))