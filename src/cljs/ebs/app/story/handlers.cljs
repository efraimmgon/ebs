(ns ebs.app.story.handlers
  (:require
   clojure.string
   [ajax.core :as ajax]
   [ebs.utils.datetime :as datetime]
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
;;; DB

(defn get-all-stories-by-project
  [{:keys [project-id on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (firestore/collection @fdb "projects" project-id "stories")
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map (fn [doc]
                             (oops/ocall doc "data")))
                     on-success))))))

(defn create-story
  [{:keys [params on-success]}]
  (let [fdb (rf/subscribe [:firestore/db])
        docRef (-> (firestore/collection @fdb "projects" (:project_id params) "stories")
                   firestore/doc)
        params (db/prepare-input docRef params)]
    (-> docRef
        (firestore/setDoc (clj->js params))
        (.then #(on-success params)))))

;;; ---------------------------------------------------------------------------
;;; Events

(rf/reg-event-fx
 :stories/load-success
 events/base-interceptors
 (fn [{:keys [db]} [stories]]
   {:db (assoc db :stories/all (map story->in stories))}))


(rf/reg-event-fx
 :stories/load
 events/base-interceptors
 (fn [_ [project-id]]
   (get-all-stories-by-project
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
   {:http-xhrio {:method :get
                 :uri (str "/api/projects/" project-id "/stories/" story-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:story/load-success]
                 :on-failure [:common/set-error]}}))


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
   (let [current-user (rf/subscribe [:identity])]
     (create-story
      {:params (-> @story
                   (assoc :user_id (oops/oget @current-user "uid")))
       :on-success #(rf/dispatch [:story/create-success %])})
     nil)))


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
 (fn [{:keys [db]} [story]]
   (let [project-id (get-in db [:project/active :id])
         story-id (get-in db [:story/active :id])]
     {:http-xhrio {:method :put
                   :uri (str "/api/projects/" project-id "/stories/" story-id)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params (-> story
                               ;story->out
                               (assoc :project_id project-id))
                   :on-success [:story/update-success]
                   :on-failure [:common/set-error]}})))

(rf/reg-event-fx
 :story/delete-success
 events/base-interceptors
 (fn [{:keys [db]} _]
   {:dispatch [:navigate! :project/view-stories
               {:project-id (get-in db [:project/active :id])}]
    :db (dissoc db :story/active)}))

(rf/reg-event-fx
 :story/delete!
 events/base-interceptors
 (fn [_ [project-id story-id]]
   {:http-xhrio {:method :delete
                 :uri (str "/api/projects/" project-id "/stories/" story-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:story/delete-success]
                 :on-failure [:common/set-error]}}))


;;; ---------------------------------------------------------------------------
;;; Subscriptions

(rf/reg-sub :stories/all events/query)
(rf/reg-sub :stories/show-complete? events/query)
(rf/reg-sub :story/active events/query)
(rf/reg-sub :story/new events/query)

(rf/reg-sub
 :stories/pending
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "pending" (get % "status")) stories)))

(rf/reg-sub
 :stories/in-progress
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "in progress" (get % "status")) stories)))

(rf/reg-sub
 :stories/complete
 :<- [:stories/all]
 (fn [stories]
   (filter #(= "complete" (get % "status")) stories)))

