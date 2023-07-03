(ns ebs.app.auth.core
  (:require
   [ebs.utils.events :refer [to-edn base-interceptors]]
   [oops.core :as oops]
   [re-frame.core :as rf]
   ["firebase/auth" :as firebase-auth]
   ["firebase/firestore" :as firestore]))


(defn auth []
  (firebase-auth/getAuth @(rf/subscribe [:firebase/app])))


(defn sign-in [auth-provider opts]
  (-> (firebase-auth/signInWithPopup
       (auth) auth-provider)
      (.then (fn [^js result]
               (rf/dispatch [:set-identity
                             (-> result (oops/oget "user") to-edn)])
               (rf/dispatch [:auth/initialize-firestore])))
      (.catch (fn [e]
                (if-let [handler (:error-handler opts)]
                  (handler e)
                  (js/alert e))))))


(defn google-sign-in [opts]
  (sign-in (firebase-auth/GoogleAuthProvider.)
           opts))


(defn sign-out [error-handler]
  (-> (auth)
      (.signOut)
      (.catch (fn [e] (or (and error-handler (error-handler e))
                          (println e)))))
  (set! (.-location js/window) "/"))


(rf/reg-fx :auth/google-sign-in google-sign-in)


(rf/reg-fx :auth/sign-out sign-out)


(rf/reg-event-fx
 :auth/sign-in
 base-interceptors
 (fn [_ [opts]]
   {:auth/google-sign-in opts}))


(rf/reg-event-db
 :set-identity
 base-interceptors
 (fn [db [identity]]
   (assoc db :identity identity)))


(rf/reg-event-db
 :firestore/set-db
 base-interceptors
 (fn [db [firestore-db]]
   (assoc db :firestore/db firestore-db)))


(rf/reg-event-fx
 :auth/initialize-firestore
 base-interceptors
 (fn [_ _]
   (let [app (rf/subscribe [:firebase/app])
         current-user (rf/subscribe [:identity])]
     (when @current-user
       {:dispatch [:firestore/set-db
                   (firestore/getFirestore @app)]}))))


