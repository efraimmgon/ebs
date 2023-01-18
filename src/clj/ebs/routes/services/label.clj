(ns ebs.routes.services.label
  (:require
   [clojure.spec.alpha :as s]))

(def labels
  [{:id :bug :name "Bug"}
   {:id :feature :name "Feature"}
   {:id :chore :name "Chore"}])

(s/def :label/id keyword?)
(s/def :label/name string?)

(s/def :label/Label
  (s/keys :req-un [:label/id :label/name]))
