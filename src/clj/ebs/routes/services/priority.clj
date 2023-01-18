(ns ebs.routes.services.priority
  (:require
   [clojure.spec.alpha :as s]))

(def priorities
  [{:id :urgent :name "Urgent"}
   {:id :high :name "High"}
   {:id :medium :name "Medium"}
   {:id :low :name "Low"}
   {:id :dont-fix :name "Don't Fix"}])

(s/def :priority/id keyword?)
(s/def :priority/name string?)

(s/def :priority/Priority
  (s/keys :req-un [:priority/id :priority/name]))
