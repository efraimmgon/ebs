(ns ebs.app.project.views
  (:require
   ebs.app.project.handlers
   [ebs.utils.components :as c]
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
      "Edit"] " "
     [:button.btn.btn-light
      {:on-click #(rf/dispatch [:project/archive id])}
      "Archive"]]}])

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

(defn new-project-ui
  "Component to create a new project."
  []
  (r/with-let [path [:project/new]
               fields (rf/subscribe [:query path])]
    [views/base-ui
     [c/card
      {:title "New Project"

       :body
       [:div
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
           :class "form-control"}]]]

       :footer
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:project/create fields])}
        "Create"]}]]))

(defn edit-project-ui
  "Component to edit a project."
  []
  (r/with-let [path [:project/active]
               fields (rf/subscribe [:query path])]
    [views/base-ui
     [c/card
      ; next to the title there should be a button to delete the project
      {:title [:span "Edit Project"
               [:button.btn.btn-danger.float-right
                ;; TODO: add a modal to confirm the deletion.
                {:on-click #(rf/dispatch [:project/delete (:id @fields)])}
                "Delete"]]

       :body
       [:div
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
           :class "form-control"}]]]

       :footer
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:project/update fields])}
        "Update"]}]]))