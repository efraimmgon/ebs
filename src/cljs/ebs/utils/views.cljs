(ns ebs.utils.views
  (:require
   [ebs.utils.components :as c]
   [ebs.utils.events :refer [<sub dispatch-n]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))

;;; ---------------------------------------------------------------------------
;;; Modal

(defn modal-ui
  "Component to display the currently active modal."
  []
  (when-let [modal @(rf/subscribe [:modal])]
    [modal]))

(defn error-modal-ui
  "Component to display the currently error (on a modal)."
  []
  (when-let [error-msg (<sub [:common/error])]
    [c/modal
     {:header
      "An error has occured"
      :body [c/pretty-display error-msg]
      :footer
      [:div
       [:button.btn.btn-sm.btn-danger
        {:on-click #(rf/dispatch [:common/set-error nil])}
        "OK"]]}]))

;;; ---------------------------------------------------------------------------
;;; Base

(defn create-project-button []
  [:li.nav-item
   [:a.nav-link.text-nowrap
    {:on-click #(rf/dispatch [:navigate! :project/new])}
    "Create project"]])

(defn navbar []
  (r/with-let [current-project (rf/subscribe [:project/active])]
    [:nav.navbar.navbar-dark.sticky-top.bg-dark.flex-md-nowrap.p-0
     [:a.navbar-brand.col-sm-3.col-md-2.mr-0 {:href "#"} "EBS"]
   ; TODO: search
     #_[:input.form-control.form-control-dark.w-100
        {:type "text", :placeholder "Search", :aria-label "Search"}]
   ; TODO: create project button.
     [:ul.navbar-nav.px-3
      (when-not @current-project
        [create-project-button])]]))

(defn sidebar []
  [:nav.col-md-2.d-none.d-md-block.bg-light.sidebar
   [:div.sidebar-sticky
    [:ul.nav.flex-column
     [:li.nav-item
      [:a.nav-link.active
       {:href "#"}
       [:span {:data-feather "home"}]
       "Projects"]]]]])

(defn base-ui [& components]
  [:div
   [navbar]
   [:div.container-fluid
    [modal-ui]
    [error-modal-ui]
    [:div.row
     [sidebar]
     [:main.col-md-9.ml-sm-auto.col-lg-10.pt-3.px-4
      {:role "main"}
      (into
       [:div]
       components)]]]])