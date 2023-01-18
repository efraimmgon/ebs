(ns ebs.routes.services.status
  (:require
   [clojure.spec.alpha :as s]))

(def statuses
  [{:id :pending :name "Pending"}
   {:id :in-progress :name "In Progress"}
   {:id :done :name "Done"}])

(s/def :status/id keyword?)
(s/def :status/name string?)

(s/def :status/Status
  (s/keys :req-un [:status/id :status/name]))
