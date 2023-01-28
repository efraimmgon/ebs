(ns ebs.app.timer.handlers
  (:require
   [re-frame.core :as rf]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]))

(defn short-or-long-break
  [interval-count long-break-interval]
  (if (zero? (mod interval-count long-break-interval))
    :long-break
    :short-break))

(short-or-long-break 0 4)

(defn set-time-elapsed [db v] (assoc-in db [:timer/settings :time-elapsed] v))
(defn update-time-elapsed [db f] (update-in db [:timer/settings :time-elapsed] f))
(defn set-state [db v] (assoc-in db [:timer/settings :state] v))
(defn set-current-session [db v] (assoc-in db [:timer/settings :current-session] v))

(defn interval-success
  [db]
  (-> db
      (set-state :stopped)
      (set-time-elapsed 0)))

(defn start-work-interval
  [db]
  (-> db
      (set-state :running)
      (set-current-session :work)))

(defn start-break-interval
  [db]
  (let [break (short-or-long-break
               (get-in db [:timer/settings :interval-count])
               (get-in db [:timer/settings :long-break-interval]))]
    (-> db
        (set-state :running)
        (set-current-session break))))

(defn work-interval-success
  [db]
  (-> db
      interval-success
      (update-in [:timer/settings :interval-count] inc)
      (set-current-session
       (short-or-long-break
        (inc (get-in db [:timer/settings :interval-count]))
        (get-in db [:timer/settings :long-break-interval])))))

(defn break-interval-success
  [db]
  (-> db
      interval-success
      (set-current-session :work)))

; ------------------------------------------------------------------------------
; HANDLERS
; ------------------------------------------------------------------------------

(rf/reg-event-fx
 :timer/tick
 [rf/trim-v]
 (fn [{:keys [db]} [current-session]]
   (let [{:keys [time-elapsed] :as settings} (:timer/settings db)
         duration (get settings current-session)
         timer-done? (>= time-elapsed duration)]
     (if timer-done?
       {:db (if (= current-session :work)
              (work-interval-success db)
              (break-interval-success db))}
       {:dispatch-later [{:ms 1000
                          :dispatch [:timer/tick current-session]}]
        :db (update-time-elapsed db inc)}))))

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
 (fn [{:keys [current-session time-elapsed] :as settings}]
   (-> settings
       (get current-session)
       (- time-elapsed)
       datetime/time-remaining)))