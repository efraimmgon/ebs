(ns ebs.app.story.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.db :as db]
   [ebs.utils.events :as events]
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))


; (1) The checkbox input expects a set of labels, but the API returns a vector.
(defn story->in [story]
  (let [story (js->clj story :keywordize-keys true)]
    (cond-> story
      (:labels story) (update :labels set)))) ; (1)


;;; ---------------------------------------------------------------------------
;;; Events

(rf/reg-event-fx
 :stories/load-success
 events/base-interceptors
 (fn [{:keys [db]} [stories]]
   {:db (assoc db :stories/all (->> stories
                                    (map story->in)
                                    (sort-by :priority)))}))


(rf/reg-event-fx
 :stories/load
 events/base-interceptors
 (fn [_ [project-id]]
   (db/get-all-stories-by-project
    {:project-id project-id
     :on-success #(rf/dispatch [:stories/load-success %])})
   nil))


(rf/reg-event-fx
 :story/load-success
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   {:db (assoc db :story/active (story->in story))}))


(rf/reg-event-fx
 :story/load
 events/base-interceptors
 (fn [_ [project-id story-id]]
   (db/get-story-by-id
    {:project-id project-id
     :story-id story-id
     :on-success #(rf/dispatch [:story/load-success %])})
   nil))


(rf/reg-event-fx
 :story/create-success
 events/base-interceptors
 (fn [_ [story]]
   {:dispatch-n [[:remove-modal]
                 [:update-in [:stories/all] conj story]]}))


(rf/reg-event-fx
 :story/create!
 events/base-interceptors
 (fn [_ [story]]
   (db/create-story!
    {:params @story
     :on-success #(rf/dispatch [:story/create-success %])})
   nil))


(rf/reg-event-fx
 :story/update-success
 events/base-interceptors
 (fn [{:keys [db]} [story]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id (:project_id story)}]
    :db (dissoc db :story/active)}))


(rf/reg-event-fx
 :story/update!
 events/base-interceptors
 (fn [_ [story]]
   (db/update-story!
    {:story-id (:id @story)
     :project-id (:project_id @story)
     :params @story
     :on-success #(rf/dispatch [:story/update-success %])})
   nil))


(rf/reg-event-fx
 :story/delete-success
 events/base-interceptors
 (fn [{:keys [db]} [project-id]]
   {:dispatch [:navigate! :project/view-stories
               {:project-id project-id}]
    :db (dissoc db :story/active)}))


(rf/reg-event-fx
 :story/delete!
 events/base-interceptors
 (fn [_ [project-id story-id]]
   (db/delete-story!
    {:project-id project-id
     :story-id story-id
     :on-success #(rf/dispatch [:story/delete-success project-id])})
   nil))


;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :stories/all events/query)
(rf/reg-sub :stories/show-complete? events/query)
(rf/reg-sub :story/active events/query)

(rf/reg-sub
 :stories/pending
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "pending" (:status %)) stories)))

(rf/reg-sub
 :stories/in-progress
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "in progress" (:status %)) stories)))

(rf/reg-sub
 :stories/complete
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "complete" (:status %)) stories)))

