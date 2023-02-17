(ns ebs.app.timer.handlers
  (:require

   [clojure.spec.alpha :as s]
   [ajax.core :as ajax]
   [cljs-time.core :as time]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ebs.utils.datetime :as datetime]
   [ebs.utils.events :as events]))

(defonce timer (r/atom {:time-remaining (* 1000 60 25)}))

(s/def :timer/state #{:idle :paused :running})

(defn time-remaining-for-ui
  []
  (-> (:time-remaining @timer)
      datetime/ms->hours-mins-sec
      ((juxt :minutes :seconds))))

(defn short-or-long-break
  "Takes the timer/settings map and returns :short-break or :long-break 
   depending on the interval-count."
  [{:keys [interval-count long-break-interval]}]
  (if (zero? (mod interval-count long-break-interval))
    :long-break
    :short-break))

(defn set-state [db v]
  (assoc-in db [:timer/settings :state] v))

(defn set-current-session [db v]
  (assoc-in db [:timer/settings :current-session] v))

(defn set-time-remaining! [v]
  (swap! timer assoc :time-remaining v))

(defn interval-success
  "Common updates to db on a successful interval."
  [db]
  (-> db
      (set-state :idle)
      (assoc-in [:timer/settings :start-datetime] nil)
      (assoc-in [:timer/settings :end-datetime] nil)))

(defn end-datetime
  "The end-datetime is the datetime now plus the time remaining for the timer."
  []
  (time/plus (time/now)
             (time/millis (:time-remaining @timer))))

(defn start-interval
  "Updates on db when starting an interval."
  [db]
  (-> db
      (set-state :running)
      (assoc-in [:timer/settings :start-datetime] (time/now))
      (assoc-in [:timer/settings :end-datetime] (end-datetime))))

(defn start-work-interval
  "Updates on db when starting a work interval."
  [db]
  (-> db
      start-interval
      (set-current-session :work)))

(defn start-break-interval
  "Updates on db when starting a break interval."
  [db]
  (let [break (short-or-long-break (:timer/settings db))]
    (-> db
        start-interval
        (set-current-session break))))

(defn work-interval-success
  "Updates on db when a work interval is successful."
  [db]
  (let [db (update-in db [:timer/settings :interval-count] inc)
        break (short-or-long-break (:timer/settings db))]
    (set-time-remaining!
     (if (= break :short-break)
       @(rf/subscribe [:timer/short-break-duration])
       @(rf/subscribe [:timer/long-break-duration])))
    (-> db
        interval-success
        (set-current-session break))))

(defn break-interval-success
  "Updates on db when a break interval is successful."
  [db]
  (set-time-remaining! @(rf/subscribe [:timer/work-duration]))
  (-> db
      interval-success
      (set-current-session :work)))

(defn timer-running?
  "Returns true if the timer is running."
  [timer-settings]
  (= :running (:state timer-settings)))

(defn timer-done?
  "Returns true if the timer is done."
  [{:keys [end-datetime]}]
  (time/after? (time/now)
               end-datetime))

(defn tick?
  "Returns true if the timer is running and not done."
  [timer-settings]
  (and (timer-running? timer-settings)
       (not (timer-done? timer-settings))))

(defn time-remaining
  [datetime-now end-datetime]
  (time/in-millis
   (time/interval datetime-now
                  end-datetime)))

(defn tick!
  "Updates the timer data."
  [{:keys [end-datetime]}]
  (set-time-remaining!
   (time-remaining (time/now) end-datetime)))

(defn timer-updater
  "Updates the timer every second."
  []
  (let [js-interval
        (js/setInterval
         (fn []
           (let [timer-settings (rf/subscribe [:timer/settings])]
             (if (tick? @timer-settings)
               (tick! @timer-settings)
               (rf/dispatch [:timer/done]))))
         1000)]

    (rf/dispatch [:assoc-in [:timer/settings :js-interval] js-interval])))

; ------------------------------------------------------------------------------
; HANDLERS
; ------------------------------------------------------------------------------

(rf/reg-event-fx
 :timer/set-task
 events/base-interceptors
 (fn [{:keys [db]} [task]]
   {:db (assoc-in db [:timer/settings :task] task)}))

(rf/reg-event-fx
 :timer/create-interval!
 events/base-interceptors
 (fn [_ [{:keys [task-id start-datetime end-datetime]}]]
   {:http-xhrio {:method :post
                 :uri "/api/intervals"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params {:task_id task-id
                          :start (datetime/unparse-utc-datetime start-datetime)
                          :end (datetime/unparse-utc-datetime end-datetime)}
                 :on-success [:common/log]
                 :on-failure [:common/set-error]}}))

(rf/reg-event-fx
 :timer/start
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [current-session (rf/subscribe [:timer/settings :current-session])
         db-update-f (if (= @current-session :work)
                       start-work-interval
                       start-break-interval)]
     (timer-updater)
     {:db (db-update-f db)})))

(rf/reg-event-fx
 :timer/done
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [{:keys [js-interval current-session task start-datetime end-datetime]}
         @(rf/subscribe [:timer/settings])
         beep (rf/subscribe [:timer/beep])
         work-session? (= current-session :work)]
     (set! (.-volume @beep) 0.1)
     (.play @beep)
     (js/clearInterval js-interval)
     (cond-> {:db (if work-session?
                    (work-interval-success db)
                    (break-interval-success db))}
       work-session? (assoc :dispatch
                            [:timer/create-interval!
                             {:task-id (:id task)
                              :start-datetime start-datetime
                              :end-datetime end-datetime}])))))

(rf/reg-event-fx
 :timer/pause
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [{:keys [js-interval current-session task start-datetime]}
         @(rf/subscribe [:timer/settings])
         work-session? (= current-session :work)]
     (js/clearInterval js-interval)
     (cond-> {:db (set-state db :paused)}
       work-session? (assoc :dispatch
                            [:timer/create-interval!
                             {:task-id (:id task)
                              :start-datetime start-datetime
                              :end-datetime (time/now)}])))))



(rf/reg-event-fx
 :timer/skip-break
 events/base-interceptors
 (fn [{:keys [db]} _]
   (set-time-remaining! @(rf/subscribe [:timer/work-duration]))
   {:db (-> db
            (set-state :idle)
            (set-current-session :work))}))


(rf/reg-event-fx
 :timer/cancel
 events/base-interceptors
 (fn [{:keys [db]} _]
   (let [js-interval (rf/subscribe [:timer/settings :js-interval])]
     (js/clearInterval @js-interval)
     (set-time-remaining! @(rf/subscribe [:timer/work-duration]))
     {:db (-> db
              (set-state :idle)
              (set-current-session :work))})))

; ------------------------------------------------------------------------------
; SUBSCRIPTIONS
; ------------------------------------------------------------------------------

; This is the task value of the select input.
(rf/reg-sub :timer/select-task-id events/query)

(rf/reg-sub
 :timer/settings
 (fn [db [_ k]]
   (let [settings (:timer/settings db)]
     (if k
       (get settings k)
       settings))))

(rf/reg-sub
 :timer/work-duration
 :<- [:timer/settings]
 (fn [settings]
   (get settings :work)))

(rf/reg-sub
 :timer/short-break-duration
 :<- [:timer/settings]
 (fn [settings]
   (get settings :short-break)))

(rf/reg-sub
 :timer/long-break-duration
 :<- [:timer/settings]
 (fn [settings]
   (get settings :long-break)))

(rf/reg-sub
 :timer/beep
 :<- [:timer/settings]
 (fn [settings]
   (:session-done-audio settings)))

(rf/reg-sub
 :timer.ui.select/selected-task
 :<- [:timer/settings]
 (fn [settings]
   (:task settings)))

(rf/reg-sub
 :timer.ui.select/tasks
 :<- [:timer/settings]
 :<- [:story/active]
 :<- [:tasks/pending]
 (fn [[settings active-story pending-tasks]]
   (let [timer-state (:state settings)
         timer-task (:task settings)]
     (if (or (= :idle timer-state)
             (= (:id active-story) (:story_id timer-task)))
       pending-tasks
       (if (seq timer-task)
         (cons timer-task pending-tasks)
         pending-tasks)))))

(comment
  @(rf/subscribe [:tasks/pending])
  @(rf/subscribe [:timer/settings])
  @(rf/subscribe [:timer.ui.select/tasks]))