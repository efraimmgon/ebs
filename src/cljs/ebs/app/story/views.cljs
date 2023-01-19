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

(defn story-card
  "Component to display a story."
  [{:keys [id project-id title description]}]
  [c/card
   {:title title
    :body [:p description]
    :footer
    [:div
     ; Use a link instead of a button
     [:a.btn.btn-primary
      {:href (rfe/href :story/view {:project-id project-id :story-id id})}
      "Open"] " "
     [:a.btn.btn-warning
      {:href (rfe/href :story/edit {:project-id project-id :story-id id})}
      "Edit"] " "
     [:button.btn.btn-light
      {:on-click #(rf/dispatch [:story/archive id])}
      "Archive"]]}])

(defn stories-ui
  "Component to display the stories."
  []
  (r/with-let [project (rf/subscribe [:project/active])
               stories (rf/subscribe [:stories/all])]
    [views/base-ui
     [:div
      [:h1 "Stories"
       [:a.btn.btn-primary.float-right
        {:href (rfe/href :story/new {:project-id (:id @project)})}
        "New Story"]]]
     (if (seq @stories)
       [:div
        (doall
         (for [story @stories]
           ^{:key (:id story)}
           [:div [story-card story]
            [:hr]]))]
       [:div "There are no stories yet"])]))

(defn new-story-ui
  "Component to create a new story."
  []
  ;; TODO: assoc project-id to the new story
  (r/with-let [path [:story/new]
               fields (rf/subscribe [:query path])]
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
