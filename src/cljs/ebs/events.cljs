(ns ebs.events
  (:require
   cljs.pprint
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :refer [base-interceptors query]]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

;;; ---------------------------------------------------------------------------
;;; HANDLERS
;;; ---------------------------------------------------------------------------

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
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :common/log
 base-interceptors
 (fn [_ [msg]]
   (prn msg)
   nil))


;;; ---------------------------------------------------------------------------
;;; Initial state

(def timer-settings
  {:work (datetime/min->ms 25)
   :short-break (datetime/min->ms 5)
   :long-break (datetime/min->ms 15)
   :long-break-interval 4
   :js-interval nil
   :start-datetime nil
   :end-datetime nil
   :interval-count 0
   :state :idle
   :current-session :work})
   ;:session-done-audio (js/Audio. "/audio/tada.wav")})

; TODO: fetch data from the server.
(def priorities
  [{:id 1 :name "urgent"}
   {:id 2 :name "high"}
   {:id 3 :name "medium"}
   {:id 4 :name "low"}
   {:id 5 :name "don't fix"}])

(def statuses
  ["pending" "in progress" "complete"])

(def labels
  ["bug" "feature" "chore"])

(def default-db
  {:statuses/all statuses
   :priorities/all priorities
   :labels/all labels
   :timer/settings timer-settings})

(rf/reg-event-fx
 :initialize-app!
 base-interceptors
 (fn [{:keys [db]} _]
   {:db (merge db default-db)}))


;;; ---------------------------------------------------------------------------
;;; SUBSCRIPTIONS
;;; ---------------------------------------------------------------------------

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
(rf/reg-sub :statuses/all query)
(rf/reg-sub :priorities/all query)
(rf/reg-sub :labels/all query)
(rf/reg-sub :firebase/app query)
(rf/reg-sub :firestore/db query)