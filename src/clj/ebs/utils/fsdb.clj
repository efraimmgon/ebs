(ns ebs.utils.fsdb
  (:require
   clojure.edn
   clojure.pprint
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]))

;;; ----------------------------------------------------------------------------
;;; Filesystem-based Database
;;; ----------------------------------------------------------------------------

;;; ----------------------------------------------------------------------------
;;; Utils

(def db-dir (io/file fs/*cwd* "resources" "db"))
(def settings-path (io/file db-dir "settings.edn"))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (clojure.edn/read
       (java.io.PushbackReader.
        r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn save-edn!
  "Save edn data to file.
  Opts is a map of #{:pretty-print}."
  ([file data] (save-edn! file data nil))
  ([file data opts]
   (with-open [wrt (io/writer file)]
     (binding [*out* wrt]
       (if (:pretty-print? opts)
         (clojure.pprint/pprint data)
         (prn data))))
   data))

(declare settings)

(defn table-path
  "Returns the table's path to the dir where records are stored."
  [tname]
  (get-in @settings [:tables tname :path]))

(defn table-file
  "Return the file for the table dir/record, if it exists."
  ([tname]
   (table-file tname nil))
  ([tname id]
   (when-let [path (table-path tname)]
     (let [file (or (and id (io/file path (str id)))
                    (io/file path))]
       (when (fs/exists? file)
         file)))))

;;; ----------------------------------------------------------------------------
;;; Settings

; Management of id increment

(def settings
  "DB settings."
  (atom nil))

(defn load-settings! []
  (reset! settings (load-edn settings-path)))

(defn save-settings! []
  (save-edn! settings-path @settings {:pretty-print? true}))

(defn next-id!
  "Increment the table's counter and return the incremented number."
  [tname]
  (swap! settings update-in
         [:tables tname :counter] inc)
  (save-settings!)
  (get-in @settings
          [:tables tname :counter]))

(defn setup!
  "Checks if the db path and the settings file are set, otherwise will do it."
  [& [opts]]
  (when-not (fs/exists? db-dir)
    (fs/mkdirs db-dir))
  (when-not (fs/exists? settings-path)
    (let [opts (merge {:use-qualified-keywords? false}
                      opts)]
      (save-edn! settings-path opts)))
  (load-settings!))

(setup!)

;;; ----------------------------------------------------------------------------
;;; CREATE, DELETE TABLE

(defn create-table!
  "Creates the settings for the table. These settings will be used when 
  querying the table."
  [tname]
  (when-not (table-path tname)
    (let [table-path (io/file db-dir (name tname))
          config {:path (str table-path)
                  :counter 0}]
      ;; Create a the dir where the records will be saved.
      (fs/mkdir table-path)
      ;; Update the settings with the new table config.
      (save-edn!
       settings-path
       (swap! settings assoc-in [:tables tname] config)))))

(defn delete-table!
  "Deletes all data and settings related to the given table."
  [tname]
  (when-let [dir (table-path tname)]
    (fs/delete-dir dir)
    (save-edn!
     settings-path
     (swap! settings update :tables dissoc tname))))


;;; ----------------------------------------------------------------------------
;;; GET, SAVE, DELETE

; All files are expected to contain edn objects, so we just use
; clojure.edn/read when loading them from the file.

(defn get-by-id
  "Reads and returns the contents of the given file."
  [tname id]
  (some-> (table-file tname id)
          load-edn))

(defn get-all
  "Reads and returns the contents of the given dir."
  [tname]
  (some->> (table-file tname)
           fs/list-dir
           (map fs/name)
           (map #(get-by-id tname %))))

(defn create!
  "Creates a new table record. Returns the data with the id."
  [tname data]
  ; We generate the next id for the table and assoc it to the data map before
  ; adding the data to the db.
  (let [id (next-id! tname)
        data (assoc data
                    (if (:use-qualified-keywords? @settings)
                      (keyword (name tname) "id")
                      :id)
                    id)]
    (save-edn! (io/file
                (table-path tname)
                (str id))
               data)))

(defn update!
  "Updates the record for the given table id."
  [tname data]
  (when-let [f (table-file tname
                           (get data
                                (if (:use-qualified-keywords? @settings)
                                  (keyword (name tname) "id")
                                  :id)))]
    (save-edn! f data)))

(defn delete!
  "Deletes the record at the given file. If successful returns true. If the
  file doesn't exist, returns false."
  [tname id]
  (some-> (table-file tname id)
          fs/delete))

; Now that I have completed some basic functionaly I realized that I am missing
; some crucial features. 
; - id counter 

(comment

  (create-table! :project)
  "tests:"

  (delete-table! :user)
  (create-table! :user)
  (get-by-id :user 1)
  (get-all :user)
  (create! :user {:user/name "Guest"})
  (update! :user {:user/id 1 :user/name "gu357" :user/age 18})
  (delete! :user 1))