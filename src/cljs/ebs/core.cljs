(ns ebs.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [ebs.ajax :as ajax]
   [ebs.events]
   [ebs.router :as router]
   [clojure.string :as string]
   ["firebase/app" :as firebase]
   [oops.core :as oops])
  (:import goog.History))


(defn firebase-init []
  (let [app
        (oops/ocall firebase "initializeApp"
                    (clj->js
                     {:apiKey "AIzaSyAjZLmv-gHHAR_ZMYbJQJCiDrLDDTUEInQ",
                      :authDomain "manager-bf95c.firebaseapp.com",
                      :projectId "manager-bf95c",
                      :storageBucket "manager-bf95c.appspot.com",
                      :messagingSenderId "1075455170693",
                      :appId "1:1075455170693:web:b47966b515a03c324331ec",
                      :measurementId "G-WM3LLG5E4C"}))]
    (rf/dispatch [:assoc-in [:firebase/app] app])))


;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'router/page] (.getElementById js/document "app")))

(defn init! []
  (firebase-init)
  (router/start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch-sync [:initialize-app!])
  (mount-components))
