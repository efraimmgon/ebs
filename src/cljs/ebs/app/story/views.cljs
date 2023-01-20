(ns ebs.app.story.views
  (:require
   clojure.string
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.components :as c]
   [ebs.utils.forms :as forms]
   [ebs.utils.views :as views]
   [reitit.frontend.easy :as rfe]
   ebs.app.story.handlers))

(defn story-list-item
  "Component to display a story."
  [{:keys [id project_id title]}]
  [:a.no-link-style
   {:href (rfe/href :story/edit {:project-id project_id :story-id id})}
   [:li.list-group-item
    [:p title]]])

(defn stories-ui
  "Component to display the stories."
  []
  (r/with-let [project (rf/subscribe [:project/active])
               pending (rf/subscribe [:stories/pending])
               in-progress (rf/subscribe [:stories/in-progress])
               completed (rf/subscribe [:stories/completed])]
    [views/base-ui
     [:div
      [:h1 "Stories"
       [:a.btn.btn-primary.float-right
        {:href (rfe/href :story/new {:project-id (:id @project)})}
        "New Story"]]]
     [:div.row
      (doall
       (for [[stories title] [[pending "Pending"] [in-progress "In Progress"] [completed "Completed"]]]
         ^{:key title}
         [:div.col-md-4
          (if (seq @stories)
            [:div
             [:h3 title]
             [:ul.list-group
              (doall
               (for [story @stories]
                 ^{:key (:id story)}
                 [story-list-item story]))]]
            [:div [:h3 title] [::p "-"]])]))]]))

(defn new-story-ui
  "Component to create a new story."
  []
  ;; TODO: assoc project-id to the new story
  (r/with-let [path [:story/new]
               fields (rf/subscribe path)]
    [views/base-ui
     [c/card
      {:title "New Story"

       :body
       [:div
        [:div.row
         [:div.col-md-9
          [c/form-group
           "Title"
           [forms/input
            {:type :text
             :name (conj path :title)
             :placehodler "Title"
             :class "form-control"}]]
          [c/form-group
           "Description"
           [forms/textarea
            {:name (conj path :description)
             :placeholder "Description"
             :class "form-control"
             :rows 5}]]]
         [:div.col-md-3
          [c/form-group
           "Status"
           [forms/select
            {:name (conj path :status)
             :default-value "pending"
             :class "form-control"}
            (doall
             (for [status @(rf/subscribe [:statuses/all])]
               ^{:key status}
               [:option {:value status} (clojure.string/capitalize status)]))]]
          [c/form-group
           "Priority"
           [forms/select
            {:name (conj path :priority)
             :class "form-control"}
            (doall
             (into [[:option {:value ""} ""]]
                   (for [{:keys [id name]} @(rf/subscribe [:priorities/all])]
                     ^{:key id}
                     [:option {:value id} name])))]]
          [c/form-group
           "Labels"
           (doall
            (for [label @(rf/subscribe [:labels/all])]
              ^{:key label}
              [forms/checkbox-comp
               {:name (conj path :labels)
                :label (clojure.string/capitalize label)
                :value label}]))]]]]

       :footer
       [:div
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:story/create @fields])}
         "Create"]]}]]))

; edit-story-ui (same ui as new-story-ui, just chage the title and the button label)
(defn edit-story-ui
  "Component to edit a story."
  []
  (r/with-let [path [:story/active]
               story (rf/subscribe path)]
    [views/base-ui
     [c/card
      {:title
       [:span (:title @story)
        [:button.btn.btn-danger.float-right
         {:on-click #(rf/dispatch
                      [:story/delete (:project_id @story) (:id @story)])}
         "Delete"]]

       :body
       [:div
        [:div.row
         [:div.col-md-9
          [c/form-group
           "Title"
           [forms/input
            {:type :text
             :name (conj path :title)
             :placehodler "Title"
             :class "form-control"}]]
          [c/form-group
           "Description"
           [forms/textarea
            {:name (conj path :description)
             :placeholder "Description"
             :class "form-control"
             :rows 5}]]]
         [:div.col-md-3
          [c/form-group
           "Status"
           [forms/select
            {:name (conj path :status)
             :default-value "pending"
             :class "form-control"}
            (doall
             (for [status @(rf/subscribe [:statuses/all])]
               ^{:key status}
               [:option {:value status} (clojure.string/capitalize status)]))]]
          [c/form-group
           "Priority"
           [forms/select
            {:name (conj path :priority)
             :class "form-control"}
            (doall
             (into [[:option {:value ""} ""]]
                   (for [{:keys [id name]} @(rf/subscribe [:priorities/all])]
                     ^{:key id}
                     [:option {:value id} name])))]]
          [c/form-group
           "Labels"
           (doall
            (for [label @(rf/subscribe [:labels/all])]
              ^{:key label}
              [forms/checkbox-comp
               {:name (conj path :labels)
                :label (clojure.string/capitalize label)
                :value label}]))]]]]

       :footer
       [:div
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:story/update @story])}
         "Update"]]}]]))