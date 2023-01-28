(ns ebs.app.timer.handlers
  (:require
   [re-frame.core :as rf]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]))

(defn interval-success
  [db]
  (-> db
      (assoc-in [:timer/settings :state] :stopped)
      (update-in [:timer/settings :interval-count] inc)
      (assoc-in [:timer/settings :time-elapsed] 0)))

(defn start-work-interval
  [db]
  (-> db
      (assoc-in [:timer/settings :state] :running)
      (assoc-in [:timer/settings :current-session] :work)))

(defn start-break-interval
  [db]
  (let [break (if (zero? (mod (get-in db [:timer/settings :interval-count])
                              (get-in db [:timer/settings :long-break-interval])))
                :long-break
                :short-break)]
    (-> db
        (assoc-in [:timer/settings :state] :running))))


; ------------------------------------------------------------------------------
; HANDLERS
; ------------------------------------------------------------------------------

(rf/reg-event-fx
 :timer/tick
 events/base-interceptors
 (fn [{:keys [db]} [current-session]]
   (let [{:keys [time-elapsed] :as settings} (:timer/settings db)
         duration (get settings current-session)
         timer-done? (>= time-elapsed duration)]
     (if timer-done?
       {:db (interval-success db)}
       {:dispatch-later [{:ms 1000
                          :dispatch [:timer/tick current-session]}]
        :db (update-in db [:timer/settings :time-elapsed] inc)}))))

(rf/reg-event-fx
 :timer/start
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [current-session (get-in db [:timer/settings :current-session])
         db-update-f (if (= current-session :work)
                       start-work-interval
                       start-break-interval)]
     {:dispatch [:timer/tick current-session]
      :db (db-update-f db)})))

; ------------------------------------------------------------------------------
; SUBSCRIPTIONS
; ------------------------------------------------------------------------------

(rf/reg-sub :timer/task events/query)

(rf/reg-sub :timer/settings events/query)

(rf/reg-sub
 :timer/state
 :<- [:timer/settings]
 (fn [settings]
   (:state settings)))

; Returns the time duration of the current interval
(rf/reg-sub
 :timer/time-remaining
 :<- [:timer/settings]
 (fn [{:keys [current-session time-elapsed] :as settings}]
   (-> settings
       (get current-session)
       (- time-elapsed)
       datetime/time-remaining)))