(ns ebs.routes.services.common
  (:require
   [clojure.spec.alpha :as s]
   [ebs.utils :refer [string->date]]
   [spec-tools.core :as st]
   [java-time.api :as jt]
   [clojure.spec.alpha :as spec])
  (:import
   java.time.LocalDateTime
   java.time.Instant))


(defn now []
  (java.time.Instant/now))

; ------------------------------------------------------------------------------
; Common
; ------------------------------------------------------------------------------

(def local-date-time-spec
  (st/spec
   {:spec #(or (instance? java.time.LocalDateTime %) (string? %))
    :decode/json #(string->date %2)
    :encode/json #(str %2)}))

(def instant-spec
  (st/spec
   {:spec #(or (instance? java.time.Instant %) (string? %))
    :decode/json #(java.time.Instant/parse %2)
    :encode/json #(str %2)}))

(s/def ::id int?)
(s/def :common/date instant-spec)


(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

; ------------------------------------------------------------------------------
; Generic Result
; ------------------------------------------------------------------------------

(s/def :result/result keyword?)
(s/def :result/message (or string? keyword?))

(s/def :result/Result
  (s/keys :req-un [:result/result]
          :opt-un [:result/message]))