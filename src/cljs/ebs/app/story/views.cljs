(ns ebs.app.story.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.components :as c]
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