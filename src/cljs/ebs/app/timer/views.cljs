(ns ebs.app.timer.views
  (:require
   clojure.string
   [ebs.utils.components :as c]
   [oops.core :as oops]
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
  (let [tasks-count (count tasks)
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
     [:i.material-icons "play_circle"]
     [:i.material-icons "play_arrow"])])

(defn pause-timer-button
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/pause])}
   [:i.material-icons "pause"]])

(defn skip-break-button
  []
  [:button.btn.btn-info.btn-sm
   {:on-click #(rf/dispatch [:timer/skip-break])}
   [:i.material-icons "skip_next"]])

(defn cancel-timer-button
  []
  [:button.btn.btn-warning.btn-sm
   {:on-click #(rf/dispatch [:timer/cancel])}
   [:i.material-icons "cancel"]])

(defn timer-control-buttons
  [{:keys [tasks state current-session]}]
  [:div
   (when (and (display? @tasks) (= :idle @state))
     [start-timer-button state]) " "
   (when (and (= :idle @state)
              (not= :work @current-session))
     [skip-break-button])
   (when (= :paused @state)
     [start-timer-button state])
   (when (= :running @state)
     [pause-timer-button]) " "
   (when (and (not= :idle @state)
              (= :work @current-session))
     [cancel-timer-button])])

(defn select-task
  [tasks]
  (r/with-let [timer-task (rf/subscribe [:timer.ui.select/selected-task])

               set-selected-task-if-nil!
               (fn [timer-task tasks]
                 (when (and (seq @tasks)
                            (nil? @timer-task))
                   (rf/dispatch-sync [:timer/set-task (first @tasks)])))

               tasks-list (rf/subscribe [:timer.ui.select/tasks])]

    (set-selected-task-if-nil! timer-task tasks)
    (when (seq @tasks)
      [:select.form-control.form-control-sm
       {:value (or (:id @timer-task) "")
        :on-change #(rf/dispatch-sync
                     [:timer/set-task
                      (let [task-id (-> % .-target .-value js/parseInt)]
                        (some (fn [task]
                                (when (= (:id task) task-id)
                                  task))
                              @tasks))])}
       (for [task @tasks-list]
         ^{:key (:id task)}
         [:option {:value (:id task)} (:title task)])])))

(defn time-remaining-ui
  "Displays the time remaining in the current interval, in the format mm:ss."
  [[minutes seconds]]
  (let [format-time (fn [time] (if (< time 10) (str "0" time) time))]
    (str (format-time minutes) ":" (format-time seconds))))

(defn timer-modal
  []
  (r/with-let [tasks (rf/subscribe [:tasks/pending])
               state (rf/subscribe [:timer/settings :state])
               current-session (rf/subscribe [:timer/settings :current-session])
               cancel-handler #(rf/dispatch [:remove-modal])]

    [c/modal
     {:header [:h5 "Pomodoro Timer"]

      :attrs {:on-key-down
              #(do
                 (prn "key" (oops/oget % "key"))
                 (case (oops/oget % "key")
                   "Escape" (cancel-handler)
                   nil))}

      :body [:div
             ; todo add a circular progress ui
             [:div.form-group.row
              [:label.col-sm-1.col-form-label "Task"]
              [:div.col-sm-7
               (if (display? @tasks)
                 [select-task tasks]
                 [:div.col-form-label.text-muted "Add a task to use the timer."])]

              [timer-control-buttons
               {:tasks tasks
                :state state
                :current-session current-session}]]]

      :footer [:div
               [:button.btn.btn-secondary
                {:type "button"
                 :on-click cancel-handler}
                "Close"]]}]))

(defn navbar-timer-ui
  []
  [:li.nav-item
   [:button.nav-link.btn.btn-outline-info
    {:type "button"
     :on-click #(rf/dispatch [:modal timer-modal])}
    [:span.material-icons "timer"]
    " "
    [:span.badge.badge-danger.font-weight-bold
     [time-remaining-ui (handlers/time-remaining-for-ui)]]]])