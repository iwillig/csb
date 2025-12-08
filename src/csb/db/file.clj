(ns csb.db.file
  "Database operations for file entities"
  (:require
   [csb.db :as db]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewFile
  "Data required to create a new file.

   Required fields:
   - :project_id - ID of the associated project (Integer)
   - :path - File path within the project (String)

   Optional fields:
   - :summary - Summary of the file's content (String or nil)"
  (t/HMap :mandatory {:project_id t/Int
                      :path t/Str}
          :optional {:summary (t/Option t/Str)}))

(t/defalias FileUpdate
  "Data for updating an existing file. All fields are optional."
  (t/HMap :optional {:path t/Str
                     :summary (t/Option t/Str)}))

(t/defalias File
  "Complete file record as returned from database.

   Fields:
   - :id - Unique identifier (Integer)
   - :project_id - Associated project ID (Integer)
   - :path - File path within the project (String)
   - :summary - Summary of the file's content (String or nil)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :project_id t/Int
                      :path t/Str
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:summary (t/Option t/Str)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann create-file
       [SQLiteConnection NewFile :-> File])

(t/ann get-file-by-id
       [SQLiteConnection t/Int :-> (t/Option File)])

(t/ann get-files-by-project-id
       [SQLiteConnection t/Int :-> (t/Seqable File)])

(t/ann update-file
       [SQLiteConnection t/Int FileUpdate :-> File])

(t/ann delete-file
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-file
  "Creates a new file and returns the created file with its ID.

   The file-data map should contain:
   - :project_id (required) - Associated project ID
   - :path (required) - File path within the project
   - :summary (optional) - Summary of the file's content

   Returns the complete File record with generated ID and timestamps."
  [conn file-data]
  (let [sql-map {:insert-into :file
                 :values [file-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-file-by-id
  "Retrieves a file by its ID.

   Returns the File record if found, nil otherwise."
  [conn file-id]
  (let [sql-map {:select [:*]
                 :from [:file]
                 :where [:= :id file-id]}]
    (db/execute-one conn sql-map)))

(defn get-files-by-project-id
  "Retrieves all files associated with a project.

   Returns a sequence of File records ordered by path."
  [conn project-id]
  (let [sql-map {:select [:*]
                 :from [:file]
                 :where [:= :project_id project-id]
                 :order-by [[:path :asc]]}]
    (db/execute-many conn sql-map)))

(defn update-file
  "Updates an existing file and returns the updated record.

   The file-data map can contain any of:
   - :path - New file path
   - :summary - New summary

   Returns the updated File record with new timestamp."
  [conn file-id file-data]
  (let [sql-map {:update :file
                 :set file-data
                 :where [:= :id file-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-file
  "Deletes a file by its ID.

   Returns the number of rows deleted (0 or 1)."
  [conn file-id]
  (let [sql-map {:delete-from :file
                 :where [:= :id file-id]}]
    (db/execute-one conn sql-map)))
