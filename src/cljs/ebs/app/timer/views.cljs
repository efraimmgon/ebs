(ns ebs.app.timer.views
  (:require
   clojure.string
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.components :as c]
   [ebs.utils.forms :as forms]
   ebs.app.timer.handlers
   [ebs.utils.datetime :as datetime]))

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

(defn time-remaining-ui
  "Displays the time remaining in the current interval, in the format mm:ss."
  [[minutes seconds]]
  (let [format-time (fn [time] (if (zero? time) "00" time))]
    (str (format-time minutes) ":" (format-time seconds))))

(defn timer-ui
  "Timer UI"
  []
  (r/with-let [tasks (rf/subscribe [:story/tasks-list])
               time-left "25:00"
               state (rf/subscribe [:timer/state])
               time-remaining (rf/subscribe [:timer/time-remaining])]
    (when (seq @tasks)
      [:div
       [:h5
        [:span.material-icons "timer"]
        " Pomodoro Timer "
        [:span.badge.badge-danger.font-weight-bold
         [time-remaining-ui @time-remaining]]]
       [:div.form-group.row
        [:label.col-sm-1.col-form-label "Task"]
        [select-task tasks]
        [:div.col-sm-4
         (when (= :stopped @state)
           [start-timer-button])
         (when (= :running @state)
           [stop-timer-button])]]])))