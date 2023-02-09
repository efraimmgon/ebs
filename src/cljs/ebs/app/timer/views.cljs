(ns ebs.app.timer.views
  (:require
   clojure.string
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.app.timer.handlers :as handlers]))

; ------------------------------------------------------------------------------
; Utils
; ------------------------------------------------------------------------------

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (contains? @(rf/subscribe [:task/temporary]) id))

(defn display?
  [tasks]
  (let [tasks-count (count @tasks)
        temp-tasks-count (count @(rf/subscribe [:task/temporary]))]
    (or (> tasks-count 1)
        (and (= tasks-count 1)
             (not= temp-tasks-count 1)))))

; ------------------------------------------------------------------------------
; Views
; ------------------------------------------------------------------------------

(defn start-timer-button
  [state]
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/start])}
   (if (= :paused @state)
     "Resume"
     "Start")])

(defn pause-timer-button
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/pause])}
   "Pause"])

(defn skip-break-button
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/skip-break])}
   "Skip break"])

(defn cancel-timer-button
  []
  [:button.btn.btn-warning.btn-sm
   {:on-click #(rf/dispatch [:timer/cancel])}
   "Cancel"])

(defn select-task
  [tasks]
  (r/with-let [selected (rf/subscribe [:timer/task])]
    (when (nil? @selected)
      (rf/dispatch-sync [:assoc-in [:timer/task] (-> @tasks first :id)]))
    (when (seq @tasks)
      [:select.form-control.form-control-sm
       {:value (or @selected "")
        :on-change #(rf/dispatch [:assoc-in [:timer/task]
                                  (-> % .-target .-value js/parseInt)])}
       (for [task @tasks]
         ^{:key (:id task)}
         [:option {:value (:id task)} (:title task)])])))

(defn time-remaining-ui
  "Displays the time remaining in the current interval, in the format mm:ss."
  [[minutes seconds]]
  (let [format-time (fn [time] (if (< time 10) (str "0" time) time))]
    (str (format-time minutes) ":" (format-time seconds))))

(defn timer-ui
  "Timer UI"
  []
  (r/with-let [tasks (rf/subscribe [:tasks/pending])
               state (rf/subscribe [:timer/state])
               current-session (rf/subscribe [:timer/current-session])]
    [:div
     [:h5
      [:span.material-icons "timer"]
      " Pomodoro Timer "
      [:span.badge.badge-danger.font-weight-bold
       [time-remaining-ui (handlers/time-remaining-for-ui)]]]

     [:div.form-group.row

      [:label.col-sm-1.col-form-label "Task"]
      [:div.col-sm-7
       (if (display? tasks)
         [select-task tasks]
         [:div.col-form-label.text-muted "Add a task to use the timer."])]

      [:div.col-sm-4
       (when (and (display? tasks) (= :idle @state))
         [start-timer-button state])
       " "
       (when (and (= :idle @state)
                  (not= :work @current-session))
         [skip-break-button])
       (when (= :paused @state)
         [start-timer-button state])
       (when (= :running @state)
         [pause-timer-button]) " "
       (when (and (not= :idle @state)
                  (= :work @current-session))
         [cancel-timer-button])]]]))