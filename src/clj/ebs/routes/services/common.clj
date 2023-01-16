(ns ebs.routes.services.common
  (:require
   [clojure.spec.alpha :as s]
   [ebs.utils :refer [string->date]]
   [spec-tools.core :as st])
  (:import
   java.time.LocalDateTime))


(defn now []
  (-> (java.util.Date.) .getTime java.sql.Timestamp.))

; ------------------------------------------------------------------------------
; Common
; ------------------------------------------------------------------------------

(def local-date-time-spec
  (st/spec
   {:spec #(or (instance? java.time.LocalDateTime %) (string? %))
    :decode/json #(string->date %2)
    :encode/json #(str %2)}))


(s/def ::id int?)
(s/def ::date local-date-time-spec)

(s/def ::status #{"published", "draft"})

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

; ------------------------------------------------------------------------------
; Generic Result
; ------------------------------------------------------------------------------

(s/def :result/result keyword?)
(s/def :result/message (or string? keyword?))

(s/def :result/Result
  (s/keys :req-un [:result/result]
          :opt-un [:result/message]))