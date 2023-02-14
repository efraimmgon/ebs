(ns ebs.app.project.views
  (:require
   ebs.app.project.handlers
   [ebs.utils.components :as c]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.views :as views]
   [ebs.utils.forms :as forms]
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
      [:h1 "Projects"
       [:button.btn.btn-primary.float-right
        {:on-click #(rf/dispatch [:navigate! :project/new])}
        "New Project"]]]
     (if (seq @projects)
       [:div
        (doall
         (for [project @projects]
           ^{:key (:id project)}
           [:div [project-card project]
            [:hr]]))]
       [:div "There are no projects yet"])]))

(defn project-ui
  "Component to display a project."
  [{:keys [path title footer]}]
  (r/with-let [project (rf/subscribe path)]
    [views/base-ui
     [c/card
      {:title title

       :body
       [:div.row
        [:div.col-md-9
         [c/form-group
          "Title"
          [forms/input
           {:type "text"
            :name (conj path :title)
            :placeholder "Title"
            :class "form-control"}]]
         [c/form-group
          "Description"
          [forms/textarea
           {:name (conj path :description)
            :placeholder "Description"
            :class "form-control"
            :rows 6}]]]
        [:div.col-md-3
         [c/form-group
          "Created at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [created-at (:created_at @project)]
                     (datetime/to-datetime-local-string created-at)
                     "")
            :disabled true}]]
         [c/form-group
          "Updated at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [updated-at (:updated_at @project)]
                     (datetime/to-datetime-local-string updated-at)
                     "")
            :disabled true}]]]]

       :footer footer}]]))

(defn new-project-ui
  "Component to create a new project."
  []
  (r/with-let [path [:project/new]
               new-project (rf/subscribe path)]
    [project-ui
     {:path path

      :title "New Project"

      :footer
      [:div
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:project/create! new-project])}
        "Create"] " "
       [:a.btn.btn-light
        {:href (rfe/href :home)}
        "Cancel"]]}]))

(defn edit-project-ui
  "Component to edit a project."
  []
  (r/with-let [path [:project/active]
               project (rf/subscribe path)]
    [project-ui
     {:path path

      :title
      ; add a delete button next to the title:
      [:span
       "Edit Project "
       [:button.btn.btn-danger.float-right.btn-sm
        {:on-click #(rf/dispatch [:project/delete! (:id @project)])}
        "Delete"]]

      :footer
      [:div
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:project/update! project])}
        "Update"] " "
       [:a.btn.btn-light
        {:href (rfe/href :home)}
        "Cancel"]]}]))