(ns ebs.app.task.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.forms :as forms]
   ebs.app.task.handlers))

(defn temporary-id?
  "Returns true if the id is a temporary id (a gensym)."
  [id]
  (contains? @(rf/subscribe [:task/temporary]) id))

(defn story-list-item
  "Component to display a task."
  [{:keys [id status estimate elapsed_time]}]
  (r/with-let [path [:story/tasks-map id]
               task (rf/subscribe [:query path])
               focused? (r/atom false)]
    [:div
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
          :rows 1
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
        (if (temporary-id? id)
          [:div:div.form-row
           [:div.form-group.col-md-1]
           [:div.form-group.col-md-11
            [:button.btn.btn-primary
             {:on-click #(rf/dispatch [:task/create! @task])}
             "Create"] " "
            [:button.btn.btn-danger
             {:on-click #(rf/dispatch [:task/delete! id])}
             "Cancel"]]]
          [:div.form-row
           [:div.form-group.col-md-1]
           [:div.form-group.col-md-11
            [:button.btn.btn-primary
             {:on-click #(do
                           (swap! focused? not)
                           (rf/dispatch [:task/update! @task]))}
             "Save"] " "
            [:button.btn.btn-danger
             {:on-click #(rf/dispatch [:task/delete! id])}
             "Delete"]]]))]]))

(defn tasks-ui
  "Component to display a list of tasks."
  []
  (r/with-let [story (rf/subscribe [:story/active])
               tasks (rf/subscribe [:story/tasks-list])
               new-task? (rf/subscribe [:task/temporary])]
    [:div
     [:h4 "Tasks"]

     #_(when (seq @tasks)
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
     ;; same code as the block above, but don't use  list-group or list-group-item,
     ;; just rows and cols
     (when (seq @tasks)
       [:div
        [:div.form-row.text-center
         [:div.form-group.col-md-1
          [:label "Done?"]]
         [:div.form-group.col-md-7
          [:label "Title"]]
         [:div.form-group.col-md-2
          [:label "Time Estimate"]]
         [:div.form-group.col-md-2
          [:label "Elapsed Time"]]]
        (doall
         (for [task @tasks]
           ^{:key (:id task)}
           [story-list-item task]))])

     (when  (empty? @new-task?)
       [:button.btn.btn-light
        {:on-click #(rf/dispatch [:task/add-item (:id @story)])}
        "New item"])]))
