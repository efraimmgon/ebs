(ns ebs.app.project.views
  (:require
   clojure.string
   ebs.app.project.handlers
   [ebs.utils.components :as c]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.input :as input]
   [ebs.utils.views :as views]
   [markdown.core :as md]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))


(defn project-card
  "Component to display a project."
  [{:keys [id title description]}]
  [c/card
   {:title title
    :body [:p description]
    :footer
    [:div
         ; Use a link instead of a button
     [:a.btn.btn-primary
      {:href (rfe/href :project/view-stories {:project-id id})}
      "Open"] " "
     [:a.btn.btn-warning
      {:href (rfe/href :project/edit {:project-id id})}
      "Edit"] " "]}])


(defn projects-ui
  "Component to display the projects."
  []
  (r/with-let [projects (rf/subscribe [:projects/all])]
    [views/base-ui
     [:div
      [:hr]
      [:h1 "Projects"
       [:button.btn.btn-primary.float-right
        {:on-click #(rf/dispatch [:navigate! :project/new])}
        "New Project"]]
      (if (seq @projects)
        [:div
         (doall
          (for [project @projects]
            ^{:key (:id project)}
            [:div [project-card project]
             [:hr]]))]
        [:div "There are no projects yet"])]]))


(defn project-ui
  "Component to display a project."
  [{:keys [title footer project]}]
  (r/with-let [view-mode? (r/atom true)]
    [views/base-ui
     [c/card
      {:title title

       :body
       [:div.row
        [:div.col-md-9

         ;;; Title
         (when-not (:id @project)
           [c/form-group
            "Title"
            [input/text-input
             {:name :title
              :doc project
              :class "form-control"
              :placeholder "Title"}]])

         ;;; Description
         [c/form-group
          [:span "Description "
           [:button.btn.btn-primary.btn-sm
            {:on-click #(swap! view-mode? not)}
            (if @view-mode? "Edit" "View")]]
          (let [?description (:description @project)]
          ;; View or edit mode
            (if @view-mode?
              [:div
               (if (clojure.string/blank? ?description)
                 "Add a more detailed description..."
                 {:dangerouslySetInnerHTML
                  {:__html (md/md->html ?description)}})]
              [input/textarea
               {:name :description
                :doc project
                :class "form-control"
                :placeholder "Description"
                :rows 10}]))]]


        [:div.col-md-3

         ;; Created at (read only)
         [c/form-group
          "Created at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [created-at (:created_at @project)]
                     (datetime/firestore->datetime-input-fmt created-at)
                     "")
            :disabled true}]]

         ;; Updated at (read only)
         [c/form-group
          "Updated at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [updated-at (:updated_at @project)]
                     (datetime/firestore->datetime-input-fmt updated-at)
                     "")
            :disabled true}]]]]

       :footer footer}]]))


(defn new-project-ui
  "Component to create a new project."
  []
  (r/with-let [project (r/atom {})]
    [project-ui
     {:project project

      :title "New Project"

      :footer
      [:div
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:project/create! project])}
        "Create"] " "
       [:a.btn.btn-light
        {:href (rfe/href :home)}
        "Cancel"]]}]))


(defn edit-project-ui
  "Component to edit a project."
  []
  (let [project (r/atom @(rf/subscribe [:project/active]))]
    (when @project
      (fn []
        [project-ui
         {:project project

          ;;; Title
          :title
          [:div.row
           [:div.col-md-10

            ;; Toggle view/edit mode
            [c/toggle-comp
             (:title @project)
             [input/text-input
              {:name :title
               :doc project
               :class "form-control"
               :placeholder "Title"}]]]



           [:div.col-md-2
            ;;; Update
            [:button.btn.btn-primary.btn-sm
             {:on-click #(rf/dispatch [:project/update! project])}
             "Update"] " "

            ;;; Delete
            [:button.btn.btn-danger.btn-sm
             {:on-click #(rf/dispatch [:project/delete! (:id @project)])}
             "Delete"]]]

          :footer
          [:div

           ;;; Update
           [:button.btn.btn-primary
            {:on-click #(rf/dispatch [:project/update! project])}
            "Update"] " "

           ;;; Cancel
           [:a.btn.btn-light
            {:href (rfe/href :home)}
            "Cancel"]]}]))))