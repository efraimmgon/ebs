(ns ebs.app.task.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.forms :as forms]
   [ebs.utils.input :as input]
   ebs.app.task.handlers
   [ebs.utils.datetime :as datetime]
   [ebs.utils.components :as c]))

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (contains? @(rf/subscribe [:task/temporary]) id))

(defn create-task-ui
  [task-params]
  (r/with-let [task (r/atom task-params)]

    [:div
     [c/pretty-display @task]
     [:div.form-row

      ;;; Status (pending or complete)
      [:div.form-group.col-md-1
       [:input.form-control.form-check-input
        {:type "checkbox"
         :checked (= (:status @task) "complete")
         :on-change #(swap! task assoc :status
                            (if (= (:status @task) "complete")
                              "pending"
                              "complete"))}]]

      ;;; Title
      [:div.form-group.col-md-7
       [input/textarea
        {:doc task
         :name :title
         :rows 1
         :class "form-control"
         :auto-focus (temporary-id? (:id @task))}]]

      ;;; Current Estimate
      [:div.form-group.col-md-1
       [input/number-input
        {:doc task
         :name :current_estimate
         :class "form-control"}]]

      ;;; Time elapsed (to keep spacing consistent)
      [:div.form-group.col-md-1
       [:input.form-control
        {:type "number"
         :disabled true
         :value ""}]]

      ;;; Delete button (to keep spacing consistent)
      [:div.form-group.col-md-2
       ""]]

     [:div.form-row
      [:div.form-group.col-md-1]
      [:div.form-group.col-md-11
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:task/create! task])}
        "Create"] " "
       [:button.btn.btn-danger
        {:on-click #(rf/dispatch [:task/delete! (:id @task)])}
        "Cancel"]]]]))

(defn handle-task-being-tracked?
  "Returns true if the task has been set to 'complete' and is currently
   being tracked by the timer."
  [task timer-settings]
  (and (= (:status task) "complete")
       (= (:id task) (get-in timer-settings [:task :id]))
       (= (:state timer-settings) :running)))

(defn update-task-ui
  [task-params]
  (r/with-let [task (r/atom task-params)
               timer-settings (rf/subscribe [:timer/settings])]
    [:div
     [:div.form-row
      [:div.form-group.col-md-1
       [:input.form-control.form-check-input
        {:type "checkbox"
         :checked (= (:status @task) "complete")
         :on-change (fn [_]
                      (swap! task assoc :status
                             (if (= (:status @task) "complete")
                               "pending"
                               "complete"))
                      (when (handle-task-being-tracked? @task @timer-settings)
                        (rf/dispatch
                         [:timer/task-completed-being-tracked]))
                      (rf/dispatch [:task/update! task]))}]]
      ;;; Title
      [:div.form-group.col-md-7
       [input/textarea
        {:doc task
         :name :title
         :rows 1
         :class "form-control"
         :auto-focus (temporary-id? (:id @task))
         :on-blur #(rf/dispatch [:task/update! task])}]]

      ;;; Current estimate
      [:div.form-group.col-md-1
       [input/number-input
        {:doc task
         :name :current_estimate
         :class "form-control"
         :on-blur #(rf/dispatch [:task/update! task])}]]

      ;;; Time elapsed
      [:div.form-group.col-md-2
       (let [{:keys [hours minutes]} (-> @task :time-elapsed datetime/ms->hours-mins-sec)]
         [:div
          (str (if (< hours 10) "0" "") hours "h"
               (if (< minutes 10) "0" "")
               minutes
               "min")])]

      ;;; Delete 
      [:div.form-group.col-md-1
       [:button.btn.btn-light
        {:on-click #(rf/dispatch [:task/delete! (:id @task)])}
        [:i.material-icons.md-18 "delete"]]]]]))

(defn task-item
  [{:keys [id] :as task}]
  (r/with-let [path [:tasks/tree id]]

    (if (temporary-id? id)
      [create-task-ui task]
      [update-task-ui task])))

(defn tasks-ui
  "Component to display a list of tasks."
  []
  (r/with-let [story (rf/subscribe [:story/active])
               tasks (rf/subscribe [:tasks/all])
               new-task? (rf/subscribe [:task/temporary])]
    [:div
     [:h4 "Tasks"]

     (when (seq @tasks)

       [:div
        [:div.form-row.text-center
         [:div.form-group.col-md-1
          [:label "Done?"]]
         [:div.form-group.col-md-7
          [:label "Title"]]
         [:div.form-group.col-md-1
          [:label "Estimate"]]
         [:div.form-group.col-md-2
          [:label "Elapsed"]]
         [:div.form-group.col-md-1
          [:label ""]]]

        (for [task @tasks]
          ^{:key (:id task)}
          [task-item task])])

     (when  (empty? @new-task?)
       [:button.btn.btn-light
        {:on-click #(rf/dispatch [:task/add-item (:id @story)])}
        "New item"])]))
