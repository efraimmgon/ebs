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
  (cond-> (events/js->edn story)
    (get story "labels") (update "labels" set) ; (1)
    (get story "due_date") (assoc "due_date" (oops/oget story "!due_date"))
    true (assoc "created_at" (oops/oget story "!created_at"))
    true (assoc "updated_at" (oops/oget story "!updated_at"))))


(defn story->out
  "Coerce story data for the API."
  [story]
  (cond-> story
    :due_date (datetime/update-datetime-out :due_date)
    :created_at (datetime/update-datetime-out :created_at)
    :updated_at (datetime/update-datetime-out :updated_at)))

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
        project-id (oops/oget params "project_id")
        docRef (-> (firestore/collection @fdb "projects" project-id "stories")
                   firestore/doc)
        js-params (->> params story->out (db/prepare-input docRef))]
    (-> docRef
        (firestore/setDoc js-params)
        (.then (fn [_]
                 (when on-success
                   (on-success js-params)))))))

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
   (let [story (story->in story)]
     {:db (assoc db :story/active story)})))


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
                 [:assoc-in [:story/new] nil]
                 [:update-in [:stories/all] conj (story->in story)]]}))


(rf/reg-event-fx
 :story/create!
 events/base-interceptors
 (fn [_ [story]]
   (let [current-user (rf/subscribe [:identity])]
     (create-story
      {:params (-> @story
                   (assoc "user_id" (get @current-user "uid"))
                   clj->js)
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
                               story->out
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

