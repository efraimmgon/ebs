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

      ;;; Original estimate (read-only)
      [:div.form-group.col-md-1
       [:input.form-control
        {:type "number"
         :disabled true
         :value ""}]]

      ;;; Time elapsed (read-only)
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
  [task timer-settings]
  (and (= (:status task) "complete")
       (= (:id task) (get-in timer-settings [:task :id]))
       (= (:state timer-settings) :running)))

(defn update-task-ui
  [path]
  (r/with-let [task (rf/subscribe [:query path])
               tsettings (rf/subscribe [:timer/settings])]
    [:div
     [:div.form-row
      [:div.form-group.col-md-1
       [:input.form-control.form-check-input
        {:type "checkbox"
         :checked (= (:status @task) "complete")
         :on-change #(do (rf/dispatch-sync
                          [:assoc-in (conj path :status)
                           (if (= (:status @task) "complete")
                             "pending"
                             "complete")])
                         (when (handle-task-being-tracked? @task @tsettings)
                           (rf/dispatch
                            [:timer/task-completed-being-tracked]))
                         (rf/dispatch [:task/update! @task]))}]]
      ;; title
      [:div.form-group.col-md-7
       [forms/textarea
        {:name (conj path :title)
         :rows 1
         :class "form-control"
         :auto-focus (temporary-id? (:id @task))
         :on-blur #(rf/dispatch [:task/update! @task])}]]

      ;; current-estimate
      [:div.form-group.col-md-1
       [forms/input
        {:type :number
         :name (conj path :current_estimate)
         :class "form-control"
         :on-blur #(rf/dispatch [:task/update! @task])}]]

      ;; time-elapsed
      [:div.form-group.col-md-2
       (let [{:keys [hours minutes]} (-> @task :time-elapsed datetime/ms->hours-mins-sec)]
         [:div
          (str (if (< hours 10) "0" "") hours "h"
               (if (< minutes 10) "0" "")
               minutes
               "min")])]

      [:div.form-group.col-md-1
       [:button.btn.btn-light
        {:on-click #(rf/dispatch [:task/delete! (:id @task)])}
        [:i.material-icons.md-18 "delete"]]]]]))

(defn task-item
  [{:keys [id] :as task}]
  (r/with-let [path [:tasks/tree id]]

    (if (temporary-id? id)
      [create-task-ui task]
      [update-task-ui path])))

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
