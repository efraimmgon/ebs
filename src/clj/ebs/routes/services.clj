(ns ebs.routes.services
  (:require
   [buddy.auth :refer [authenticated?]]
   [clojure.spec.alpha :as s]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [ebs.middleware.formats :as formats]
   [ebs.middleware.exception :as exception]
   [ebs.routes.services.project :as project]
   [ebs.routes.services.story :as story]
   ebs.routes.services.label
   ebs.routes.services.priority
   ebs.routes.services.status
   [ring.util.http-response :refer :all]
   [reitit.core :as r]))

(s/def :result/result keyword?)
(s/def :result/message (or string? keyword?))

(s/def :result/Result
  (s/keys :req-un [:result/result]
          :opt-un [:result/message]))

;;; -----------------------------------------------------------------------
;;; Utils

(defn admin? [{:keys [identity] :as req}]
  (and identity
       (:users/admin identity)))

(defn forbidden-error []
  (forbidden
   {:error "Action not permitted for this user."}))

;;; -----------------------------------------------------------------------
;;; Middlewares

(defn admin-middleware [handler]
  (fn [req]
    (if (admin? req)
      (handler req)
      (forbidden-error))))

(defn auth-middleware [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      (unauthorized
       {:error "You must be logged in to perform this action."}))))

(defn users-any-granted? [{:keys [parameters identity] :as req}]
  (when identity
    (or (:users/admin identity)
        (= (:users/id identity)
           (get-in parameters [:path :users/id])))))

(defn user-any-granted-middleware [handler]
  (fn [req]
    (if (users-any-granted? req)
      (handler req)
      (forbidden-error))))

;;; -----------------------------------------------------------------------
;;; Routes

(defn story-routes
  "Return a vector of story routes."
  []
  ["/stories"
   [""
    {:get {:summary "Return all story records."
           :responses {200 {:body :story/stories}}
           :handler (fn [{:keys [parameters]}]
                      (story/get-project-stories
                       (get-in parameters [:path :project-id])))}
     :post {:summary "Create a story record in the db."
            :parameters {:body :story/NewStory}
            :responses {200 {:body :story/Story}}
            :handler (fn [{:keys [parameters]}]
                       (story/create-story!
                        (:body parameters)))}}]

   ["/{story-id}"
    {:parameters {:path {:story-id int?}}}
    [""
     {:get {:summary "Return a story record by id."
            :responses {200 {:body :story/Story}}
            :handler (fn [{:keys [parameters]}]
                       (story/get-story
                        (get-in parameters [:path :story-id])))}
      :put {:summary "Update a story record with params."
            :parameters {:body :story/UpdateStory}
            :responses {200 {:body :story/Story}}
            :handler (fn [{:keys [parameters]}]
                       (story/update-story!
                        (:body parameters)))}
      :delete {:summary "Delete a story record."
               :responses {200 {:body :result/Result}}
               :handler (fn [{:keys [parameters]}]
                          (story/delete-story!
                           (get-in parameters [:path :story-id])))}}]]])

(defn project-routes []
  ["/projects"
   [""
    {:get {:summary "Return all project records."
           :responses {200 {:body :project/projects}}
           :handler project/get-projects}
     :post {:summary "Create a project record in the db."
            :parameters {:body (s/keys :req-un [:project/title]
                                       :opt-un [:project/description])}
            :responses {200 {:body :project/Project}}
            :handler (fn [{:keys [parameters]}]
                       (project/create-project!
                        (:body parameters)))}}]

   ["/{project-id}"
    {:parameters {:path {:project-id int?}}}
    [""
     {:get {:summary "Return a project record by id."
            :responses {200 {:body :project/Project}}
            :handler (fn [{:keys [parameters]}]
                       (project/get-project
                        (get-in parameters [:path :project-id])))}
      :put {:summary "Update a project record with params."
            :parameters {:body (s/keys :req-un [:project/id
                                                :project/title]
                                       :opt-un [:project/description])}
            :responses {200 {:body :project/Project}}
            :handler (fn [{:keys [parameters]}]
                       (project/update-project!
                        (:body parameters)))}
      :delete {:summary "Delete a project record by id."
               :responses {200 {:body :result/Result}}
               :handler (fn [{:keys [parameters]}]
                          (project/delete-project!
                           (get-in parameters [:path :project-id])))}}]
    ;; "/api/project/{project-id}/stories/...r"
    (story-routes)]])




(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ;; "/projects/..."
   (project-routes)])

(comment

  (r/match-by-path
   (r/router (service-routes))
   "/api/projects/5/stories"))