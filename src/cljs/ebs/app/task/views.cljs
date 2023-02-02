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

(defn create-task-ui
  [path {:keys [id status current_estimate elapsed_time] :as task}]

  [:div
   (when (and current_estimate elapsed_time)
     [:div.float-right
      [:small.text-muted
       (str elapsed_time " / " current_estimate " minutes")]])
   [:div.form-row
    [:div.form-group.col-md-1
     [:input.form-control.form-check-input
      {:type "checkbox"
       :checked (= status "complete")
       :on-change #(rf/dispatch [:assoc-in (conj path :status)
                                 (if (= status "complete")
                                   "pending"
                                   "complete")])}]]
    [:div.form-group.col-md-8
     [forms/textarea
      {:name (conj path :title)
       :rows 1
       :class "form-control"
       :auto-focus (temporary-id? id)}]]
    [:div.form-group.col-md-1
     [forms/input
      {:type :number
       :name (conj path :current_estimate)
       :class "form-control"}]]
    [:div.form-group.col-md-1
     [:input.form-control
      {:type "number"
       :disabled true
       :value (or elapsed_time "")}]]
    [:div.form-group.col-md-1
     ""]]

   [:div.form-row
    [:div.form-group.col-md-1]
    [:div.form-group.col-md-11
     [:button.btn.btn-primary
      {:on-click #(rf/dispatch [:task/create! task])}
      "Create"] " "
     [:button.btn.btn-danger
      {:on-click #(rf/dispatch [:task/delete! id])}
      "Cancel"]]]])

(defn update-task-ui
  [path {:keys [id status current_estimate elapsed_time]}]

  [:div
   (when (and current_estimate elapsed_time)
     [:div.float-right
      [:small.text-muted
       (str elapsed_time "/" current_estimate " minutes")]])
   [:div.form-row
    [:div.form-group.col-md-1
     [:input.form-control.form-check-input
      {:type "checkbox"
       :checked (= status "complete")
       :on-change #(rf/dispatch [:task/update-status!
                                 id
                                 (if (= status "complete")
                                   "pending"
                                   "complete")])}]]
    [:div.form-group.col-md-8
     [forms/textarea
      {:name (conj path :title)
       :rows 1
       :class "form-control"
       :auto-focus (temporary-id? id)}]]
    [:div.form-group.col-md-1
     [forms/input
      {:type :number
       :name (conj path :current_estimate)
       :class "form-control"}]]
    [:div.form-group.col-md-1
     [:input.form-control
      {:type "number"
       :disabled true
       :value (or elapsed_time "")}]]
    [:div.form-group.col-md-1
     [:button.btn.btn-light
      {:on-click #(rf/dispatch [:task/delete! id])}
      [:i.material-icons.md-18 "delete"]]]]])

(defn task-item
  [{:keys [id] :as task}]
  (r/with-let [path [:story/tasks-map id]]

    (if (temporary-id? id)
      [create-task-ui path task]
      [update-task-ui path task])))

(defn tasks-ui
  "Component to display a list of tasks."
  []
  (r/with-let [story (rf/subscribe [:story/active])
               tasks (rf/subscribe [:story/tasks-list])
               new-task? (rf/subscribe [:task/temporary])]
    [:div
     [:h4 "Tasks"]

     (when (seq @tasks)

       [:div
        [:div.form-row.text-center
         [:div.form-group.col-md-1
          [:label "Done?"]]
         [:div.form-group.col-md-8
          [:label "Title"]]
         [:div.form-group.col-md-1
          [:label "Estimate"]]
         [:div.form-group.col-md-1
          [:label "Elapsed"]]
         [:div.form-group.col-md-1
          [:label "Actions"]]]

        (for [task @tasks]
          ^{:key (:id task)}
          [task-item task])])

     (when  (empty? @new-task?)
       [:button.btn.btn-light
        {:on-click #(rf/dispatch [:task/add-item (:id @story)])}
        "New item"])]))
