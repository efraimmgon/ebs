(ns ebs.app.project.views
  (:require
   clojure.string
   ebs.app.project.handlers
   [ebs.utils.components :as c]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.views :as views]
   [ebs.utils.forms :as forms]
   [markdown.core :as md]
   [oops.core :as oops]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))


(defn project-card
  "Component to display a project."
  [{:strs [id title description]}]
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

(def projects (rf/subscribe [:projects/all]))

(defn projects-ui
  "Component to display the projects."
  []
  (r/with-let [projects (rf/subscribe [:projects/all])]
    [views/base-ui
     [:div
      [:hr]
      [:h1 "Projects!"
       [:button.btn.btn-primary.float-right
        {:on-click #(rf/dispatch [:navigate! :project/new])}
        "New Project"]]
      (if (seq @projects)
        [:div
         (doall
          (for [project @projects]
            ^{:key (get project "id")}
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
         (when-not (get @project "id")
           [c/form-group
            "Title"
            [:input.form-control
             {:type "text"
              :value (get @project "title")
              :on-change #(swap! project assoc "title"
                                 (oops/oget % "target" "value"))
              :placeholder "Title"}]])

         ;;; Description
         [c/form-group
          [:span "Description "
           [:button.btn.btn-primary.btn-sm
            {:on-click #(swap! view-mode? not)}
            (if @view-mode? "Edit" "View")]]
          (let [?description (get @project "description")]
          ;; View or edit mode
            (if @view-mode?
              [:div
               (if (clojure.string/blank? ?description)
                 "Add a more detailed description..."
                 {:dangerouslySetInnerHTML
                  {:__html (md/md->html ?description)}})]
              [:textarea.form-control
               {:placeholder "Description"
                :rows 10
                :value ?description
                :on-change #(swap! project assoc "description"
                                   (oops/oget % "target" "value"))}]))]]


        [:div.col-md-3

         ;;; Created at
         #_[c/form-group
            "Created at"
            [:input.form-control
             {:type :datetime-local
              :value (if-let [created-at (oops/oget @project "?created_at")]
                       (datetime/firestore->datetime-input-fmt created-at)
                       "")
              :disabled true}]]

         ;;; Updated at
         #_[c/form-group
            "Updated at"
            [:input.form-control
             {:type :datetime-local
              :value (if-let [updated-at (oops/oget @project "?updated_at")]
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
           [:div.col-md-11

            ;; Toggle view/edit mode
            [c/toggle-comp
             (get @project "title")
             [:input.form-control
              {:type "text"
               :placeholder "Title"
               :value (get @project "title")
               :on-change #(swap! project assoc "title"
                                  (oops/oget % "target" "value"))}]]]

           ;;; Delete
           [:div.col-md-1
            [:button.btn.btn-danger.float-right.btn-sm
             {:on-click #(rf/dispatch [:project/delete! (get @project "id")])}
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