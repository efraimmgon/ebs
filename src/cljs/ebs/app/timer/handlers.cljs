(ns ebs.app.timer.handlers
  (:require
   [re-frame.core :as rf]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]))

(defn short-or-long-break
  "Takes the timer/settings map and returns :short-break or :long-break 
   depending on the interval-count."
  [{:keys [interval-count long-break-interval]}]
  (if (zero? (mod interval-count long-break-interval))
    :long-break
    :short-break))

(defn set-time-elapsed [db v] (assoc-in db [:timer/settings :time-elapsed] v))
(defn update-time-elapsed [db f] (update-in db [:timer/settings :time-elapsed] f))
(defn set-state [db v] (assoc-in db [:timer/settings :state] v))
(defn set-current-session [db v] (assoc-in db [:timer/settings :current-session] v))


(defn interval-success
  "Common updates to db on a successful interval."
  [db]
  (-> db
      (set-state :stopped)
      (set-time-elapsed 0)))

(defn start-work-interval
  "Updates on db when starting a work interval."
  [db]
  (-> db
      (set-state :running)
      (set-current-session :work)))

(defn start-break-interval
  "Updates on db when starting a break interval."
  [db]
  (let [break (short-or-long-break (:timer/settings db))]
    (-> db
        (set-state :running)
        (set-current-session break))))

(defn work-interval-success
  "Updates on db when a work interval is successful."
  [db]
  (-> db
      interval-success
      (update-in [:timer/settings :interval-count] inc)
      (set-current-session
       (short-or-long-break
        (update (:timer/settings db) :interval-count inc)))))

(defn break-interval-success
  "Updates on db when a break interval is successful."
  [db]
  (-> db
      interval-success
      (set-current-session :work)))

(defn current-session-duration
  "Returns the duration of the current session."
  [{:keys [current-session] :as settings}]
  (get settings current-session))

; ------------------------------------------------------------------------------
; HANDLERS
; ------------------------------------------------------------------------------

(rf/reg-event-fx
 :timer/tick
 [rf/trim-v]
 (fn [{:keys [db]} _]
   (let [{:keys [time-elapsed current-session] :as settings} (:timer/settings db)
         duration (current-session-duration settings)
         timer-done? (>= time-elapsed duration)]
     (if timer-done?
       {:db (if (= current-session :work)
              (work-interval-success db)
              (break-interval-success db))}
       {:dispatch-later [{:ms 1000
                          :dispatch [:timer/tick]}]
        :db (update-time-elapsed db inc)}))))

(rf/reg-event-fx
 :timer/start
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [current-session (get-in db [:timer/settings :current-session])
         db-update-f (if (= current-session :work)
                       start-work-interval
                       start-break-interval)]
     {:dispatch [:timer/tick]
      :db (db-update-f db)})))

(rf/reg-event-fx
 :timer/stop
 events/base-interceptors
 (fn [{:keys [db]} _]
   {:db (-> db
            (set-state :stopped)
            (set-time-elapsed 0))}))

(rf/reg-event-fx
 :timer/skip-break
 events/base-interceptors
 (fn [{:keys [db]} _]
   {:db (-> db
            (set-state :stopped)
            (set-time-elapsed 0)
            (set-current-session :work))}))

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

(rf/reg-sub
 :timer/current-session
 :<- [:timer/settings]
 (fn [settings]
   (:current-session settings)))

; Returns the time duration of the current interval
(rf/reg-sub
 :timer/time-remaining
 :<- [:timer/settings]
 (fn [{:keys [time-elapsed] :as settings}]
   (-> settings
       current-session-duration
       (- time-elapsed)
       datetime/time-remaining)))