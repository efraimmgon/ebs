(ns ebs.utils.input)

(defn clean-attrs [attrs]
  (dissoc attrs
          :doc))

(defn text-input
  [{:keys [doc name] :as attrs}]
  (let [edited-attrs
        (merge {:on-change (fn [event]
                             (let [value (-> event .-target .-value)]
                               (swap! doc assoc name value)))}
               (-> attrs
                   clean-attrs))]
    (fn []
      [:input
       (assoc edited-attrs
              :value (get @doc name))])))

(defn textarea
  [{:keys [doc name] :as attrs}]
  (let [edited-attrs
        (merge {:on-change (fn [event]
                             (let [value (-> event .-target .-value)]
                               (swap! doc assoc name value)))}
               (-> attrs
                   clean-attrs))]
    (fn []
      [:textarea
       (assoc edited-attrs
              :value (get @doc name))])))