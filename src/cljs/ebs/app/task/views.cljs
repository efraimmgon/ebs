(ns ebs.app.task.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [ebs.utils.forms :as forms]
   [ebs.utils.views :as views]
   ebs.app.task.handlers
   [ebs.utils.components :as c]))

; - task-ui: A task should be displayed as a li.group-item.
; - a task is a map with the following keys:
;   :title
;   :status
;   :estimate
;   :elapsed_time

; - :status can be "pending" or "complete". The ui should display a checkbox
;   for pending tasks and a checkmark for complete tasks.
; - :estimate is a number input. It represents the number of minutes the task
;   is expected to take.
; - :elapsed_time is a number input. It represents the number of minutes the
;   task has taken so far.
; - :title is a text input. It represents the title of the task.
; - Each story-list-item should also have a save button. When clicked, it
;   should dispatch a :task/update event with the task id and the new values
;   for the task.
; - The task-ui should also have a delete button. When clicked, it should
;   dispatch a :task/delete event with the task id.

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (symbol? id))

(defn story-list-item
  "Component to display a task."
  [{:keys [id status estimate elapsed_time]}]
  (r/with-let [path [:story/tasks-map id]
               task (rf/subscribe [:query path])
               focused? (r/atom false)]
    [:li.list-group-item
     {:on-focus #(swap! focused? not)
      :on-blur #(swap! focused? not)}
     [:div
      (when (and estimate elapsed_time)
        [:div.float-right
         [:small.text-muted
          (str elapsed_time "/" estimate " minutes")]])
      [:div.form-row
       [:div.form-group.col-md-1
        [:input.form-control.form-check-input
         {:type "checkbox"
          :checked (= status "complete")
          :on-change #(rf/dispatch [:assoc-in (conj path :status)
                                    (if (= status "complete")
                                      "pending"
                                      "complete")])}]]
       [:div.form-group.col-md-7
        [forms/textarea
         {:name (conj path :title)
          :class "form-control"
          :auto-focus (temporary-id? id)}]]
       [:div.form-group.col-md-2
        [forms/input
         {:type :number
          :name (conj path :current_estimate)
          :class "form-control"}]]
       [:div.form-group.col-md-2
        [:input.form-control
         {:type "number"
          :disabled true
          :value (or elapsed_time "")}]]]
      (when (or @focused? (temporary-id? id))
        [:div
         (if (temporary-id? id)
           [:button.btn.btn-primary
            {:on-click #(rf/dispatch [:task/create! @task])}
            "Create"]
           [:button.btn.btn-primary
            {:on-click #(rf/dispatch [:task/update {:id id}])}
            "Save"])
         " "
         [:button.btn.btn-danger
          {:on-click #(rf/dispatch [:task/delete! id])}
          "Delete"]])]]))

(defn tasks-ui
  "Component to display a list of tasks."
  []
  (r/with-let [story (rf/subscribe [:story/active])
               tasks (rf/subscribe [:story/tasks-list])]
    [:div
     [:h4 "Tasks"]

     (when (seq @tasks)
       [:ul.list-group
        [:li.list-group-item
         [:div.form-row.text-center
          [:div.form-group.col-md-1
           [:label "Done?"]]
          [:div.form-group.col-md-7
           [:label "Title"]]
          [:div.form-group.col-md-2
           [:label "Time Estimate"]]
          [:div.form-group.col-md-2
           [:label "Elapsed Time"]]]]
        (doall
         (for [task @tasks]
           ^{:key (:id task)}
           [story-list-item task]))])
      ; a button to add a new task
     [:button.btn.btn-light
      {:on-click #(rf/dispatch [:task/add-item (:id @story)])}
      "New item"]]))
