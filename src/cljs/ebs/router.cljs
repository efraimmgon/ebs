(ns ebs.router
  (:require
   [reagent.core :as r]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [ebs.app.project.views :as project]
   [ebs.app.story.views :as story]))

(defn page []
  (when-let [page @(rf/subscribe [:common/page])]
    [page]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'project/projects-ui
          :controllers [{:start (fn [_]
                                  (let [current-user (rf/subscribe [:identity])]
                                    (when @current-user
                                      (rf/dispatch [:projects/load]))))}]}]
    ["/project"
     ["/new"
      {:name :project/new
       :view #'project/new-project-ui}]
     ["/{project-id}"
      {:parameters {:path {:project-id int?}}
       :controllers [{:parameters {:path [:project-id]}
                      :start (fn [path]
                               (rf/dispatch
                                [:project/load-project
                                 (js/parseInt
                                  (get-in path [:path :project-id]))]))}]}
      ["/edit"
       {:name :project/edit
        :view #'project/edit-project-ui}]
      ; "/project/{project-id}/stories"
      ["/stories"
       [""
        {:name :project/view-stories
         :view #'story/stories-ui
         :controllers [{:parameters {:path [:project-id]}
                        :start (fn [path]
                                 (rf/dispatch
                                  [:stories/load
                                   (js/parseInt
                                    (get-in path [:path :project-id]))]))}]}]
       ["/{story-id}"
        {:parameters {:path {:story-id int?}}
         :controllers [{:parameters {:path [:project-id :story-id]}
                        :start (fn [path]
                                 (rf/dispatch
                                  [:story/load
                                   (js/parseInt
                                    (get-in path [:path :project-id]))
                                   (js/parseInt
                                    (get-in path [:path :story-id]))])
                                 (rf/dispatch
                                  [:story/load-tasks
                                   (js/parseInt
                                    (get-in path [:path :project-id]))
                                   (js/parseInt
                                    (get-in path [:path :story-id]))]))}]}
        ["/edit"
         {:name :story/edit
          :view #'story/edit-story-ui}]]]]]]))


(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))