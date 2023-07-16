(ns ebs.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [ebs.ajax :as ajax]
   [ebs.events]
   [ebs.firebase :as init]
   [ebs.router :as router])
  (:import goog.History))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'router/page] (.getElementById js/document "app")))

(defn init! []
  (init/firebase-init!)
  (router/start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch-sync [:initialize-app!])
  (mount-components))
