(ns ebs.utils.db
  (:require
   [ebs.utils.events :refer [base-interceptors query to-edn]]
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/firestore" :as firestore]))

(defn create
  [collection-name data-map]
  (let [fdb (rf/subscribe [:firestore/db])
        docRef (firestore/addDoc
                (firestore/collection
                 @fdb collection-name)
                (clj->js data-map))]
    (oops/oget docRef "id")))


(defn get-by-id
  [collection-name id]
  (let [fdb (rf/subscribe [:firestore/db])]
    (firestore/getDoc
     (firestore/doc
      (str collection-name "/" id)))))

(defn get-all
  [+collection handler]
  (let [fdb (rf/subscribe [:firestore/db])]
    (-> (apply firestore/collection @fdb +collection)
        firestore/getDocs
        (.then (fn [^js querySnapshot]
                 (-> (oops/oget querySnapshot "docs")
                     (.map #(oops/ocall % "data"))
                     handler))))))


(comment
  (def fdb (rf/subscribe [:firestore/db]))

  (def result (atom nil))
  (get-all ["projects" "PUnlZI3zHMyoMs7lZDNM" "stories"] #(reset! result %))
  (keys (first @result))
  (.data (oops/oget (first @result) "project_id"))

  (def result (atom nil))
  (-> (firestore/collection @fdb "projects" "PUnlZI3zHMyoMs7lZDNM" "stories")
      (firestore/getDocs)
      (.then (fn [^js querySnapshot]
               (reset! result
                       (.map (.-docs querySnapshot) #(oops/ocall % "data"))))))

  :end)
  