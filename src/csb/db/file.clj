(ns csb.db.file
  "Database operations for file entities.
   
   All operations return Result<T> for Railway-Oriented error handling.
   Failures propagate automatically through attempt-all pipelines."
  (:require
   [csb.db :as db]
   [csb.db.types :as types]
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

(t/ann ^:no-check create-file
       [SQLiteConnection NewFile :-> (types/Result File)])

(t/ann ^:no-check get-file-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option File))])

(t/ann ^:no-check get-files-by-project-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable File))])

(t/ann ^:no-check update-file
       [SQLiteConnection t/Int FileUpdate :-> (types/Result File)])

(t/ann ^:no-check delete-file
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-file
  "Creates a new file and returns the created file with its ID.

   The file-data map should contain:
   - :project_id (required) - Associated project ID
   - :path (required) - File path within the project
   - :summary (optional) - Summary of the file's content

   Returns Result<File> - the created record or Failure on database error."
  [conn file-data]
  (db/execute-one conn {:insert-into :file
                        :values [file-data]
                        :returning [:*]}))

(defn get-file-by-id
  "Retrieves a file by its ID.

   Returns Result<File | nil> - the record, nil if not found, or Failure."
  [conn file-id]
  (db/execute-one conn {:select [:*]
                        :from [:file]
                        :where [:= :id file-id]}))

(defn get-files-by-project-id
  "Retrieves all files associated with a project.

   Returns Result<Seq<File>> - sequence of records or Failure."
  [conn project-id]
  (db/execute-many conn {:select [:*]
                         :from [:file]
                         :where [:= :project_id project-id]
                         :order-by [[:path :asc]]}))

(defn update-file
  "Updates an existing file and returns the updated record.

   The file-data map can contain any of:
   - :path - New file path
   - :summary - New summary

   Returns Result<File> - the updated record or Failure."
  [conn file-id file-data]
  (db/execute-one conn {:update :file
                        :set file-data
                        :where [:= :id file-id]
                        :returning [:*]}))

(defn delete-file
  "Deletes a file by its ID.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn file-id]
  (db/execute-one conn {:delete-from :file
                        :where [:= :id file-id]}))
