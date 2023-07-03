(ns ebs.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [ebs.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[ebs started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[ebs has shut down successfully]=-"))
   :middleware wrap-dev})
