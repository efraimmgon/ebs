(ns ebs.app.timer.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.components :as c]
   [ebs.utils.forms :as forms]
   ebs.app.timer.handlers))

(defn start-timer-button
  "Start timer button"
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/start])}
   "Start timer"])

(defn stop-timer-button
  "Stop timer button"
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/stop])}
   "Stop timer"])

(defn select-task
  "Select task"
  [tasks]
  (when (seq @tasks)
    [:div.col-sm-7
     [forms/select
      {:name [:timer/task]
       :class "form-control form-control-sm"}
      (doall
       (for [task @tasks]
         ^{:key (:id task)}
         [:option {:value (:id task)} (:title task)]))]]))

(defn timer-ui
  "Timer UI"
  []
  (r/with-let [current-task (rf/subscribe [:timer/task])
               tasks (rf/subscribe [:story/tasks-list])
               time-left "25:00"]
    (when (seq @tasks)
      [:div
       [:h5 [:span.material-icons "timer"] " Pomodoro Timer"]
       [:div.form-group.row
        [:label.col-sm-1.col-form-label "Task"]
        [select-task tasks]
        [:div.col-sm-4
         [start-timer-button] " "
         [stop-timer-button] " "
         [:span.badge.badge-danger.font-weight-bold time-left]]]])))