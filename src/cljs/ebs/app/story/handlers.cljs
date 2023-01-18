(ns ebs.app.story.handlers
  (:require
   [ajax.core :as ajax]
   [ebs.utils.events :as events]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :stories/load
 events/base-interceptors
 (fn [_ [project-id]]
   (ajax/GET (str "/api/projects/" project-id "/stories")
     {:handler #(rf/dispatch [:assoc-in [:stories/all] %])
      :error-handler #(rf/dispatch [:common/set-error %])
      :response-format :json
      :keywords? true})
   nil))

; reg-sub for :stories/all
(rf/reg-sub :stories/all events/query)