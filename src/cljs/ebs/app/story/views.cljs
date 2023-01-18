(ns ebs.app.story.views
  (:require
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
  (r/with-let [stories (rf/subscribe [:stories/all])]
    [views/base-ui
     [:div
      [:h1 "Stories"
       [:button.btn.btn-primary.float-right
        {:on-click #(rf/dispatch [:navigate! :story/new])}
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
           :class "form-control"}]]]
        [c/form-group
         [:label "Status"]
         [:select.form-control
          {:value (get-in @fields [:status])
           :on-change #(rf/dispatch [:assoc-in (conj path :status) (-> % .-target .-value)])}
          [:option {:value ""} "Select a status"]
          (doall
           (for [status (rf/subscribe [:statuses/all])]
             ^{:key (:id status)}
             [:option {:value (:id status)} (:title status)]))]]
        [c/form-group
         [:label "Type"]
         [:select.form-control
          {:value (get-in @fields [:type])
           :on-change #(rf/dispatch [:assoc-in (conj path :type) (-> % .-target .-value)])}
          [:option {:value ""} "Select a type"]
          (doall
           (for [type (rf/subscribe [:types/all])]
             ^{:key (:id type)}
             [:option {:value (:id type)} (:title type)]))]]
        [c/form-group
         [:label "Priority"]
         [:select.form-control
          {:value (get-in @fields [:priority])}
          :on-change #(rf/dispatch [:assoc-in (conj path :priority) (-> % .-target .-value)])
          [:option {:value ""} "Select a priority"]
          (doall
           (for [priority (rf/subscribe [:priorities/all])]
             ^{:key (:id priority)}
             [:option {:value (:id priority)} (:title priority)]))]]]
       :footer
       [:div
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:story/create @fields])}
         "Create"]]}]]))
