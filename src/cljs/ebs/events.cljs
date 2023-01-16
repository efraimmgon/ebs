(ns ebs.events
  (:require
   cljs.pprint
   [ebs.utils.events :refer [base-interceptors query]]
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

;;dispatchers

(rf/reg-event-fx
 :ajax-error
 (fn [_ [_ response]]
   (js/console.log response)
   (let [message (get-in response [:response :message])
         error (with-out-str
                 (cljs.pprint/pprint
                  ((juxt :status-text :response) response)))]
     {:dispatch [:set-error (or message error)]})))

(rf/reg-event-db
 :assoc-in
 base-interceptors
 (fn [db [path v]]
   (assoc-in db path v)))

(rf/reg-event-db
 :update-in
 base-interceptors
 (fn [db [path f & args]]
   (apply update-in db path f args)))

(rf/reg-event-db
 :modal
 base-interceptors
 (fn [db [comp]]
   (js/window.scrollTo #js {"top" 0 "left" 0 "behavior" "smooth"})
   (let [modal-stack (:modal db)]
     (if (seq modal-stack)
       (prn "seq") (prn "not-seq"))
     (if (seq modal-stack)
       (update db :modal conj comp)
       (assoc db :modal [comp])))))

(rf/reg-event-db
 :remove-modal
 base-interceptors
 (fn [db _]
   (let [modal-stack (:modal db)]
     (if (seq modal-stack)
       (update db :modal pop)
       (assoc db :modal [])))))

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-docs]}}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-docs]}))

;;; ---------------------------------------------------------------------------
;;; SUBSCRIPTIONS

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :modal
 (fn [db _]
   (let [modal-stack (:modal db)]
     (when (seq modal-stack)
       (peek modal-stack)))))

(rf/reg-sub
 :query
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub :common/error query)
(rf/reg-sub :identity query)
