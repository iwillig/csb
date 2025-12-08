(ns csb.db.file-change
  "Database operations for file change entities"
  (:require
   [csb.db :as db]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewFileChange
  "Data required to create a new file change.

   Required fields:
   - :plan_id - ID of the associated plan (Integer)
   - :file_id - ID of the file being changed (Integer)

   Optional fields: None"
  (t/HMap :mandatory {:plan_id t/Int
                      :file_id t/Int}))

(t/defalias FileChange
  "Complete file change record as returned from database.

   Fields:
   - :id - Unique identifier (Integer)
   - :plan_id - Associated plan ID (Integer)
   - :file_id - File being changed (Integer)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :plan_id t/Int
                      :file_id t/Int
                      :created_at t/Str
                      :updated_at t/Str}))

(t/defalias FileChangeChange
  "A change within a file change (addition, removal, or update).

   Fields:
   - :id - Unique identifier (Integer)
   - :file_change_id - Parent file change (Integer)
   - :change_type - Type of change: 'addition', 'removal', or 'update' (String)
   - :line_start - Starting line number (Integer)
   - :line_end - Ending line number (Integer)
   - :change_content - The actual change content (String or nil)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :file_change_id t/Int
                      :change_type t/Str
                      :line_start t/Int
                      :line_end t/Int
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:change_content (t/Option t/Str)}))

(t/defalias FileChangeApplication
  "Status of a file change application to a file.

   Fields:
   - :id - Unique identifier (Integer)
   - :file_change_id - Parent file change (Integer)
   - :plan_id - Associated plan (Integer)
   - :status - Status: 'pending', 'applied', or 'failed' (String)
   - :result_message - Details about the application result (String or nil)
   - :applied_at - Timestamp when applied (String)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :file_change_id t/Int
                      :plan_id t/Int
                      :status t/Str
                      :applied_at t/Str
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:result_message (t/Option t/Str)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann create-file-change
       [SQLiteConnection NewFileChange :-> FileChange])

(t/ann get-file-change-by-id
       [SQLiteConnection t/Int :-> (t/Option FileChange)])

(t/ann get-file-changes-by-plan-id
       [SQLiteConnection t/Int :-> (t/Seqable FileChange)])

(t/ann get-file-changes-by-file-id
       [SQLiteConnection t/Int :-> (t/Seqable FileChange)])

(t/ann create-file-change-change
       [SQLiteConnection (t/HMap :mandatory {:file_change_id t/Int
                                             :change_type t/Str
                                             :line_start t/Int
                                             :line_end t/Int}
                                 :optional {:change_content (t/Option t/Str)})
        :-> FileChangeChange])

(t/ann get-file-change-changes-by-file-change-id
       [SQLiteConnection t/Int :-> (t/Seqable FileChangeChange)])

(t/ann create-file-change-application
       [SQLiteConnection (t/HMap :mandatory {:file_change_id t/Int
                                             :plan_id t/Int
                                             :status t/Str}
                                 :optional {:result_message (t/Option t/Str)})
        :-> FileChangeApplication])

(t/ann get-file-change-application-by-file-change-id
       [SQLiteConnection t/Int :-> (t/Option FileChangeApplication)])

(t/ann update-file-change-application-status
       [SQLiteConnection t/Int t/Str (t/Option t/Str) :-> FileChangeApplication])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-file-change
  "Creates a new file change and returns the created file change with its ID.

   The file-change-data map should contain:
   - :plan_id (required) - Associated plan ID
   - :file_id (required) - File being changed

   Returns the complete FileChange record with generated ID and timestamps."
  [conn file-change-data]
  (let [sql-map {:insert-into :file_change
                 :values [file-change-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-file-change-by-id
  "Retrieves a file change by its ID.

   Returns the FileChange record if found, nil otherwise."
  [conn file-change-id]
  (let [sql-map {:select [:*]
                 :from [:file_change]
                 :where [:= :id file-change-id]}]
    (db/execute-one conn sql-map)))

(defn get-file-changes-by-plan-id
  "Retrieves all file changes associated with a plan.

   Returns a sequence of FileChange records ordered by creation time."
  [conn plan-id]
  (let [sql-map {:select [:*]
                 :from [:file_change]
                 :where [:= :plan_id plan-id]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn get-file-changes-by-file-id
  "Retrieves all file changes associated with a file.

   Returns a sequence of FileChange records ordered by creation time."
  [conn file-id]
  (let [sql-map {:select [:*]
                 :from [:file_change]
                 :where [:= :file_id file-id]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn create-file-change-change
  "Creates a new file change change (addition, removal, or update) and returns it.

   The change-data map should contain:
   - :file_change_id (required) - Parent file change ID
   - :change_type (required) - Type of change: 'addition', 'removal', or 'update'
   - :line_start (required) - Starting line number
   - :line_end (required) - Ending line number
   - :change_content (optional) - The actual change content

   Returns the FileChangeChange record with generated ID and timestamps."
  [conn change-data]
  (let [sql-map {:insert-into :file_change_change
                 :values [change-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-file-change-changes-by-file-change-id
  "Retrieves all changes associated with a file change.

   Returns a sequence of FileChangeChange records ordered by line numbers."
  [conn file-change-id]
  (let [sql-map {:select [:*]
                 :from [:file_change_change]
                 :where [:= :file_change_id file-change-id]
                 :order-by [[:line_start :asc]]}]
    (db/execute-many conn sql-map)))

(defn create-file-change-application
  "Creates a new file change application status and returns it.

   The application-data map should contain:
   - :file_change_id (required) - Parent file change ID
   - :plan_id (required) - Associated plan ID
   - :status (required) - Status: 'pending', 'applied', or 'failed'
   - :result_message (optional) - Details about the application result

   Returns the FileChangeApplication record with generated ID and timestamps."
  [conn application-data]
  (let [sql-map {:insert-into :file_change_application
                 :values [application-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-file-change-application-by-file-change-id
  "Retrieves the application status for a file change.

   Returns the FileChangeApplication record if found, nil otherwise."
  [conn file-change-id]
  (let [sql-map {:select [:*]
                 :from [:file_change_application]
                 :where [:= :file_change_id file-change-id]}]
    (db/execute-one conn sql-map)))

(defn update-file-change-application-status
  "Updates the status of a file change application.

   Returns the updated FileChangeApplication record."
  [conn file-change-application-id status result-message]
  (let [sql-map {:update :file_change_application
                 :set {:status status
                       :result_message result-message}
                 :where [:= :id file-change-application-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))
