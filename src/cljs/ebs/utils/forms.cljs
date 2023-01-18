(ns ebs.utils.forms
  (:require
   [cljs.reader :as reader]
   [clojure.string :as string]
   [oops.core :as oops]
   [reagent.core :as r]
   [reagent.dom :refer [dom-node]]
   [re-frame.core :as rf]))

;;; ---------------------------------------------------------------------------
;;; Utils

(defn make-vec [x]
  (if (coll? x) x [x]))

(defn clean-attrs [attrs]
  (dissoc attrs
          :default-checked
          :default-value
          :doc
          :save-fn
          :get-fn))

(defn target-value [event]
  (oops/oget event "target" "value"))

(defn get-stored-val [path]
  @(rf/subscribe [:query path]))

(defn parse-number [string]
  (when-not (empty? string)
    (let [parsed (js/parseFloat string)]
      (when-not (js/isNaN parsed)
        parsed))))

(defn read-string*
  "Same as cljs.reader/read-string, except it returns a string when
  read-string returns a symbol."
  [x]
  (let [parsed (reader/read-string x)]
    (if (symbol? parsed)
      (str parsed)
      parsed)))

(defn make-handler->set!
  [path f]
  (fn [event]
    (rf/dispatch-sync [:assoc-in path (f event)])))

(defn make-handler->update!
  "Takes a path and a function and returns a handler.
  The function will be called on the value stored at path."
  [path f]
  (fn [event]
    (rf/dispatch-sync [:update-in path f])))

; NOTE: Reason for `""`: 
; https://zhenyong.github.io/react/tips/controlled-input-null-value.html
(defn value-attr [value]
  (or value ""))

(defn maybe-set-default!
  [{:keys [data-type default? path value save-fn get-fn]}]
  (case data-type
    :scalar
    (when (and default?
               (nil? (get-stored-val path)))
      (rf/dispatch-sync [:assoc-in path (or value default?)]))

    :set
    (when (and default? (nil? (get-fn)))
      (rf/dispatch-sync [:update-in path save-fn]))))

;;; ---------------------------------------------------------------------------
;;; Components

(defmulti input :type)

;; text, email, password
(defmethod input :default
  [{:keys [name default-value] :as attrs}]
  (let [name (make-vec name)
        edited-attrs
        (merge {:on-change (make-handler->set! name target-value)}
               (-> attrs
                   clean-attrs))]
    (fn []
      (maybe-set-default!
       {:path name :default? default-value :data-type :scalar})
      [:input (assoc edited-attrs
                     :value (or (get-stored-val name) default-value))])))

(defmethod input :number
  [{:keys [name default-value] :as attrs}]
  (let [name (make-vec name)
        edited-attrs
        (merge {:on-change (make-handler->set!
                            name (comp parse-number target-value))}
               (-> attrs
                   clean-attrs))]
    (fn []
      (maybe-set-default!
       {:path name :default? default-value :data-type :scalar})
      [:input (assoc edited-attrs
                     :value (or (get-stored-val name) default-value))])))

(defn textarea
  [{:keys [name default-value] :as attrs}]
  (let [name (make-vec name)
        edited-attrs
        (merge {:on-change (make-handler->set! name target-value)}
               (-> attrs
                   clean-attrs))]
    (fn []
      (maybe-set-default!
       {:path name :default? default-value :data-type :scalar})
      [:textarea (assoc edited-attrs
                        :value (or (get-stored-val name) default-value))])))


; NOTE: js types can be used as values, but not cljs 
; (no symbols, or keywords, for instance)
(defmethod input :radio
  [{:keys [name default-checked value] :as attrs}]
  (let [name (make-vec name)
        edited-attrs
        (merge {:on-change (make-handler->set!
                            name (comp read-string* target-value))}
               (clean-attrs attrs))]
    (fn []
      (maybe-set-default!
       {:path name :default? default-checked :value value
        :data-type :scalar})
      [:input (assoc edited-attrs
                     :checked (= value (get-stored-val name)))])))

(comment
  "Checkbox component. 
   - The value of the checkbox is stored in a set. The set is stored in the 
   app-db at the path `name`.
   - `default-checked` is a boolean that determines whether the checkbox is
   checked by default.
   - `save-fn` is a function that takes the set of checked values and returns
   a new set of checked values. By default it is a function that adds or
   removes the value from the set when the checkbox is clicked.
   - `get-fn` is a function that takes the set of checked values and returns
   the value of the checkbox. By default it is a function that returns the
   value if it is in the set.")
(defmethod input :checkbox
  [{:keys [name default-checked save-fn get-fn value] :as attrs}]
  (let [name (make-vec name)
        f (fn [acc]
            (if (get acc value)
              (disj acc value)
              ((fnil conj #{}) acc value)))
        save-fn (or save-fn f)
        get-fn (or (and get-fn #(get-fn (get-stored-val name)))
                   #(get-stored-val (conj name value)))
        edited-attrs
        (merge {:on-change (make-handler->update! name save-fn)}
               (clean-attrs attrs))]
    (fn []
      (maybe-set-default!
       (assoc attrs
              :path name,
              :data-type :set,
              :default? default-checked
              :save-fn save-fn
              :get-fn get-fn))
      [:input (assoc edited-attrs
                     :checked (boolean (get-fn)))])))

(defn select
  "Select component, with common boilerplate. 
   - The only mandatory attribute is `name`. The options are passed as a 
   rest argument in the form of `[:option {:value \"value\"} \"label\"]`. 
   - The `default-value` attribute can be used to set the default value. 
   Otherwise the value of the first option will be used."
  [{:keys [name default-value] :as attrs} & options]
  (let [name (make-vec name),
        default-value (or default-value
                          (-> options first second :value)),
        edited-attrs
        (merge {:on-change (make-handler->set!
                            name (comp read-string* target-value))}
               (clean-attrs attrs))]
    (fn []
      (maybe-set-default!
       {:path name :default? default-value :data-type :scalar})
      (into
       [:select (assoc edited-attrs
                       :value (value-attr (get-stored-val name)))]
       options))))

(defn radio-input
  "Radio component, with common boilerplate."
  [{:keys [class label] :as attrs}]
  [:div.form-check.form-check-radio
   [:label.form-check-label
    [input (assoc attrs
                  :type :radio
                  :class (or class "form-check-input"))]
    label
    [:span.circle>span.check]]])

(defn checkbox-input
  "Checkbox component, with common boilerplate."
  [{:keys [class label] :as attrs}]
  [:div.form-check
   [:label.form-check-label
    [input (assoc attrs
                  :type :checkbox
                  :class (or class "form-check-input"))]
    label
    [:span.form-check-sign>span.check]]])

(def datetime-format "YYYY-MM-DDTHH:mm:ss.SSS")

; Requires: jquery, bootstrap-datepicker
(defn datetime-input-group
  "Takes a path to where to save the input."
  [{:keys [name] :as params}]
  (r/create-class
   {:display-name "datetime component"

    :reagent-render
    (fn [{:keys [name] :as params}]
      [input
       (merge
        {:type :text
         :class "form-control"
         :placeholder "No date"
         :name name}
        params)])

    :component-did-mount
    (fn [this]
      (let [$dp (js/$ (dom-node this))]
        (.datetimepicker
         $dp (clj->js {:format datetime-format}))

        (.on $dp "dp.change"
             #(rf/dispatch
               [:assoc-in name
                (-> % (oops/oget "date") .toISOString)]))))}))


;;; test
(comment
  [:div

   [forms/input
    {:type :text
     :class "form-control"
     :name :text
     :default-value "my name"}]

   [forms/radio-input
    {:name :radio
     :label "Radio 1"
     :value :radio1}]
   [forms/radio-input
    {:name :radio
     :label "Radio 2"
     :value :radio2
     :default-checked true}]

   [forms/checkbox-input
    {:name :checkbox
     :label "Checkbox 2"
     :value :checkbox2}]

   [forms/select
    {:name :select
     :class "form-control"}
    [:option {:value "1"} 1]
    [:option {:value "2"} 2]]

   [forms/textarea
    {:class "form-control"
     :name :textarea
     :default-value "hi"}]])
