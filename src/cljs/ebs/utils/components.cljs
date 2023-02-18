(ns ebs.utils.components
  (:require
   cljs.pprint
   [clojure.string :as string]
   [cljs.reader :as reader]
   [ebs.utils.forms :as forms]
   [oops.core :as oops]
   [reagent.core :as r]
   [re-frame.core :as rf]))

;;; ---------------------------------------------------------------------------
;;; UTILS

(defn on-key-handler
  "Takes a map of .-key's and functions. Returns a matching function. If
  the event.key str is present in the map, it calls the respective function."
  [keymap]
  (fn [event]
    (when-let [f (get keymap
                      (oops/oget event "key"))]
      (f))))

; ------------------------------------------------------------------------------
; Debugging
; ------------------------------------------------------------------------------

(defn pretty-display [data]
  [:pre
   (with-out-str
     (cljs.pprint/pprint data))])


; ------------------------------------------------------------------------------
; Forms
; ------------------------------------------------------------------------------

(defn form-group
  "Bootstrap's `form-group` component."
  [label & input]
  [:div.form-group
   [:label.label-control label]
   (into
    [:div]
    input)])


; ------------------------------------------------------------------------------
; MISC
; ------------------------------------------------------------------------------

(defn card [{:keys [title subtitle body footer]}]
  [:div.card
   [:div.card-header
    [:h5.card-title title]
    [:p.card-category subtitle]]
   [:div.card-body body]
   [:div.card-footer footer]])

(defn breadcrumbs [& items]
  (into
   [:ol.breadcrumb
    [:li [:a {:href "/"} "Home"]]]
   (for [{:keys [href title active?] :as item} items]
     (if active?
       [:li.active title]
       [:li [:a {:href href} title]]))))

(defn dismissible-alert [{:keys [attrs body]}]
  [:div.alert.alert-dismissible.fade.show
   (merge {:role "alert"} attrs)
   body
   [:button {:type "button"
             :class "close"
             :data-dismiss "alert"
             :aria-label "Fechar"}
    [:span {:aria-hidden "true"} "x"]]])


; ------------------------------------------------------------------------------
; TABLE
; ------------------------------------------------------------------------------

(defn thead
  ([headers] (thead nil headers))
  ([attrs headers]
   [:thead
    attrs
    [:tr
     (for [th headers]
       ^{:key th}
       [:th th])]]))

(defn tbody [rows]
  (into
   [:tbody]
   (for [row rows]
     (into
      [:tr]
      (for [td row]
        [:td td])))))

(defn thead-indexed
  "Coupled with `tbody-indexed`, allocates a col for the row's index."
  [headers]
  [:thead
   (into
    [:tr
     [:th "#"]]
    (for [th headers]
      [:th th]))])

(defn tbody-indexed
  "Coupled with `thead-indexed`, allocates a col for the row's index."
  [rows]
  (into
   [:tbody]
   (map-indexed
    (fn [i row]
      (into
       [:tr [:td (inc i)]]
       (for [td row]
         [:td
          td])))
    rows)))

(defn tabulate
  "Render data as a table.
  `rows` is expected to be a coll of maps.
  `ks` are the a the set of keys from rows we want displayed.
  `class` is css class to be aplied to the `table` element."
  ([ks rows] (tabulate ks rows {}))
  ([ks rows {:keys [class]}]
   [:table
    {:class class}
    ;; if there are extra headers we append them
    [thead (map (comp (fn [s] (string/replace s #"-" " "))
                      string/capitalize
                      name)
                ks)]
    [tbody (map (apply juxt ks) rows)]]))

; ------------------------------------------------------------------------------
; Modal
; ------------------------------------------------------------------------------

; Note: on-key-down won't work unless the modal is focused.
(defn modal [{:keys [attrs header body footer]}]
  [:div
   (assoc attrs
          :tab-index 0)
   [:div.modal-dialog
    [:div.modal-content
     (when header
       [:div.modal-header
        [:div.modal-title
         header]])
     (when body [:div.modal-body body])
     (when footer
       [:div.modal-footer
        footer])]]
   [:div.modal-backdrop
    {:on-click #(rf/dispatch [:remove-modal])}]])

; --------------------------------------------------------------------
; PAGER
; --------------------------------------------------------------------

(defn partition-links [n links]
  (when (seq links)
    (vec (partition-all n links))))

(defn forward [i page-count]
  (if (< i (dec page-count)) (inc i) i))

(defn back [i]
  (if (pos? i) (dec i) i))

(defn nav-link [current-page i]
  [:li.page-item
   {:class (when (= i @current-page) "active")}
   [:a.page-link
    {:on-click #(reset! current-page i)}
    (inc i)]])

(defn pager [page-count current-page]
  (when (> page-count 1)
    [:nav {:aria-label "..."}
     (into
      [:ul.pagination.justify-content-center]
       ;; "Previous" button
      (concat
       [[:li.page-item
         {:class (when (= @current-page 0) "disabled")}
         [:a.page-link
          {:on-click #(swap! current-page back page-count)}
          "<<"]]]
         ;; "Pages" buttons
       (for [i (range page-count)]
         ^{:key i}
         [nav-link current-page i])
         ;; "Next" button
       [[:li.page-item
         {:class (when (= @current-page (dec page-count)) "disabled")}
         [:a.page-link
          {:on-click #(swap! current-page forward page-count)}
          ">>"]]]))]))

; --------------------------------------------------------------------
; TOGGLE-COMP ON FOCUS
; --------------------------------------------------------------------

(defn toggle-comp
  [c1 c2]
  (r/with-let [focus? (r/atom false)]
    [:span.link-style
     {:tab-index 0
      :on-focus #(reset! focus? true)
      :on-blur #(reset! focus? false)}
     (if @focus?
       c2
       c1)]))


(comment
  (let [current-page (atom 0)
        page-count (atom [{:page 1} {:page 2}])]
    [pager (count @page-count) current-page]))