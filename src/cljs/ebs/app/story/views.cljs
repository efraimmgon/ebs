(ns ebs.app.story.views
  (:require
   clojure.string
   [reagent.core :as r]
   [re-frame.core :as rf]
   [oops.core :as oops]
   [markdown.core :as md]
   [ebs.app.timer.views :as timer]
   [ebs.app.task.views :as task]
   [ebs.utils.components :as c]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.input :as input]
   [ebs.utils.forms :as forms]
   [ebs.utils.views :as views]
   [reitit.frontend.easy :as rfe]
   ebs.app.story.handlers))

; TODO: move this to a sub
(def label->class
  {"bug" "badge-danger"
   "feature" "badge-success"
   "chore" "badge-info"})

(defn story-list-item
  "Component to display a story."
  [{:keys [id project_id title priority labels due_date]}]

  [:a.no-link-style
   {:href (rfe/href :story/edit {:project-id project_id :story-id id})}
   [:li.list-group-item
    [:p
     (when priority
       [:small.text-muted (str "[" priority "]")]) " "
     title " "
     (doall
      (for [label labels]
        ^{:key label}
        [:span
         [:span.badge {:class (label->class label)} label]
         " "]))]

    (when (and due_date (not (clojure.string/blank? due_date)))
      [:div
       [:small.text-muted
        (datetime/datetime-ui due_date)]])]])

(defn extra-options
  "Component to display extra options for the stories"
  [project]
  (r/with-let [show? (rf/subscribe [:stories/show-complete?])]
    [:div.dropdown.dropleft.float-right
     [:button.btn.btn-light.dropdown-toggle
      {:type "button"
       :id "dropdownMenu2"
       :data-toggle "dropdown"
       :aria-haspopup "true"
       :aria-expanded "false"}
      "⋮"]
     [:div.dropdown-menu
      [:button.dropdown-item
       {:type "button"
        :on-click #(rf/dispatch [:update-in [:stories/show-complete?] not])}
       (if @show?
         "Hide complete stories"
         "View complete stories")]
      [:a.dropdown-item
       ; {:href (rfe/href :story/new {:project-id (:id @project)})}
       "New Story"]]]))

(defn new-story-modal
  [params]
  (r/with-let [story (r/atom params)
               create-handler #(rf/dispatch [:story/create! story])
               cancel-handler #(rf/dispatch [:remove-modal])]
    [c/modal
     {:header "New Story"

      :attrs {:on-key-down
              #(case (oops/oget % "key")
                 "Escape" (cancel-handler)
                 "Enter" (create-handler)
                 nil)}

      :body [:div
             [c/form-group
              "Title"
              [input/text-input
               {:class "form-control"
                :doc story
                :name :title
                :auto-focus true
                :placeholder "Title"}]]]

      :footer [:div
               [:button.btn.btn-primary
                {:on-click create-handler}
                "Create"] " "
               [:button.btn.btn-secondary
                {:on-click cancel-handler}
                "Cancel"]]}]))


(defn stories-ui
  "Component to display the stories."
  []
  (r/with-let [project (rf/subscribe [:project/active])
               statuses (rf/subscribe [:statuses/all])
               pending (rf/subscribe [:stories/pending])
               in-progress (rf/subscribe [:stories/in-progress])
               complete (rf/subscribe [:stories/complete])
               show-complete? (rf/subscribe [:stories/show-complete?])
               stories+titles (map vector
                                   [pending in-progress complete]
                                   @statuses)]
    [views/base-ui
     [:div
      [:h1 (:title @project)
       [extra-options project]]]

     [:div.row
      (doall
       (for [[stories title] (if @show-complete?
                               stories+titles
                               (drop-last stories+titles))]
         ^{:key title}
         [:div {:class (if @show-complete?
                         "col-md-4"
                         "col-md-6")}

          [:h3 (clojure.string/capitalize title)
           [:button.btn.btn-light.float-right
            {:on-click
             #(rf/dispatch [:modal (partial new-story-modal
                                            {:status title
                                             :project_id (:id @project)})])}
            "Add New"]]
          (if (empty? @stories)
            [:p "-"]
            [:ul.list-group
             (for [story @stories]
               ^{:key (:id story)}
               [story-list-item story])])]))]]))


(defn story-ui
  "Component to display the story form."
  [{:keys [title footer path story]}]
  (r/with-let [view-mode? (r/atom true)]
    [views/base-ui
     [c/card
      {:title title

       :body
       [:div.row

      ;; Left column
        [:div.col-md-9
         [c/form-group
          [:span "Description "
           [:button.btn.btn-primary.btn-sm
            {:on-click #(swap! view-mode? not)}
            (if @view-mode? "Edit" "View")]]
          (if @view-mode?
            [:div
             (if (clojure.string/blank? (:description @story))
               "Add a more detailed description..."
               {:dangerouslySetInnerHTML
                {:__html (md/md->html (:description @story))}})]
            [forms/textarea
             {:name (conj path :description)
              :placeholder "Description"
              :class "form-control"
              :rows 10}])]

         [:hr]

         [task/tasks-ui]]

      ;; Right column
        [:div.col-md-3
         [c/form-group
          "Status"
          [forms/select
           {:name (conj path :status)
            :default-value "pending"
            :class "form-control"}
           (doall
            (for [status @(rf/subscribe [:statuses/all])]
              ^{:key status}
              [:option {:value status} (clojure.string/capitalize status)]))]]
         [c/form-group
          "Priority"
          [forms/select
           {:name (conj path :priority)
            :class "form-control"
            :save-fn #(js/parseInt %)}
           (doall
            (into [[:option {:value ""} ""]]
                  (for [{:keys [id name]} @(rf/subscribe [:priorities/all])]
                    ^{:key id}
                    [:option {:value id} name])))]]
         [c/form-group
          "Due date"
          [forms/datetime-input
           {:name (conj path :due_date)
            :class "form-control"}]]
         [c/form-group
          "Labels"
          (doall
           (for [label @(rf/subscribe [:labels/all])]
             ^{:key label}
             [forms/checkbox-comp
              {:name (conj path :labels)
               :label (clojure.string/capitalize label)
               :value label}]))]
         [c/form-group
          "Created at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [created-at (:created_at @story)]
                     (datetime/to-datetime-local-string created-at)
                     "")
            :disabled true}]]
         [c/form-group
          "Updated at"
          [:input.form-control
           {:type :datetime-local
            :value (if-let [updated-at (:updated_at @story)]
                     (datetime/to-datetime-local-string updated-at)
                     "")
            :disabled true}]]]]


       :footer footer}]]))

(defn edit-story-ui
  "Component to edit a story."
  []
  (r/with-let [path [:story/active]
               story (rf/subscribe path)]
    [story-ui
     {:title
      [:div.row
       [:div.col-md-11
        [c/toggle-comp
         (:title @story)
         [forms/input
          {:type :text
           :name (conj path :title)
           :placehodler "Title"
           :class "form-control"}]]]
       [:div.col-md-1
        [:span.float-right
         [:button.btn.btn-danger
          {:on-click #(rf/dispatch [:story/delete! (:project_id @story) (:id @story)])}
          "Delete"]]]]

      :path path

      :story story

      :footer
      [:div
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:story/update! @story])}
        "Update"]
       [:a.btn.btn-secondary.ml-2
        {:href (rfe/href :project/view-stories {:project-id (:project_id @story)})}
        "Cancel"]]}]))