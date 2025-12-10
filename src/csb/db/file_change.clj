(ns csb.db.file-change
  "Database operations for file change entities.
   
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

(t/ann ^:no-check create-file-change
       [SQLiteConnection NewFileChange :-> (types/Result FileChange)])

(t/ann ^:no-check get-file-change-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option FileChange))])

(t/ann ^:no-check get-file-changes-by-plan-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable FileChange))])

(t/ann ^:no-check get-file-changes-by-file-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable FileChange))])

(t/ann ^:no-check create-file-change-change
       [SQLiteConnection (t/HMap :mandatory {:file_change_id t/Int
                                             :change_type t/Str
                                             :line_start t/Int
                                             :line_end t/Int}
                                 :optional {:change_content (t/Option t/Str)})
        :-> (types/Result FileChangeChange)])

(t/ann ^:no-check get-file-change-changes-by-file-change-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable FileChangeChange))])

(t/ann ^:no-check create-file-change-application
       [SQLiteConnection (t/HMap :mandatory {:file_change_id t/Int
                                             :plan_id t/Int
                                             :status t/Str}
                                 :optional {:result_message (t/Option t/Str)})
        :-> (types/Result FileChangeApplication)])

(t/ann ^:no-check get-file-change-application-by-file-change-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option FileChangeApplication))])

(t/ann ^:no-check update-file-change-application-status
       [SQLiteConnection t/Int t/Str (t/Option t/Str) :-> (types/Result FileChangeApplication)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-file-change
  "Creates a new file change and returns the created file change with its ID.

   The file-change-data map should contain:
   - :plan_id (required) - Associated plan ID
   - :file_id (required) - File being changed

   Returns Result<FileChange> - the created record or Failure on database error."
  [conn file-change-data]
  (db/execute-one conn {:insert-into :file_change
                        :values [file-change-data]
                        :returning [:*]}))

(defn get-file-change-by-id
  "Retrieves a file change by its ID.

   Returns Result<FileChange | nil> - the record, nil if not found, or Failure."
  [conn file-change-id]
  (db/execute-one conn {:select [:*]
                        :from [:file_change]
                        :where [:= :id file-change-id]}))

(defn get-file-changes-by-plan-id
  "Retrieves all file changes associated with a plan.

   Returns Result<Seq<FileChange>> - sequence of records or Failure."
  [conn plan-id]
  (db/execute-many conn {:select [:*]
                         :from [:file_change]
                         :where [:= :plan_id plan-id]
                         :order-by [[:created_at :asc]]}))

(defn get-file-changes-by-file-id
  "Retrieves all file changes associated with a file.

   Returns Result<Seq<FileChange>> - sequence of records or Failure."
  [conn file-id]
  (db/execute-many conn {:select [:*]
                         :from [:file_change]
                         :where [:= :file_id file-id]
                         :order-by [[:created_at :asc]]}))

(defn create-file-change-change
  "Creates a new file change change (addition, removal, or update) and returns it.

   The change-data map should contain:
   - :file_change_id (required) - Parent file change ID
   - :change_type (required) - Type of change: 'addition', 'removal', or 'update'
   - :line_start (required) - Starting line number
   - :line_end (required) - Ending line number
   - :change_content (optional) - The actual change content

   Returns Result<FileChangeChange> - the created record or Failure."
  [conn change-data]
  (db/execute-one conn {:insert-into :file_change_change
                        :values [change-data]
                        :returning [:*]}))

(defn get-file-change-changes-by-file-change-id
  "Retrieves all changes associated with a file change.

   Returns Result<Seq<FileChangeChange>> - sequence of records or Failure."
  [conn file-change-id]
  (db/execute-many conn {:select [:*]
                         :from [:file_change_change]
                         :where [:= :file_change_id file-change-id]
                         :order-by [[:line_start :asc]]}))

(defn create-file-change-application
  "Creates a new file change application status and returns it.

   The application-data map should contain:
   - :file_change_id (required) - Parent file change ID
   - :plan_id (required) - Associated plan ID
   - :status (required) - Status: 'pending', 'applied', or 'failed'
   - :result_message (optional) - Details about the application result

   Returns Result<FileChangeApplication> - the created record or Failure."
  [conn application-data]
  (db/execute-one conn {:insert-into :file_change_application
                        :values [application-data]
                        :returning [:*]}))

(defn get-file-change-application-by-file-change-id
  "Retrieves the application status for a file change.

   Returns Result<FileChangeApplication | nil> - the record, nil if not found, or Failure."
  [conn file-change-id]
  (db/execute-one conn {:select [:*]
                        :from [:file_change_application]
                        :where [:= :file_change_id file-change-id]}))

(defn update-file-change-application-status
  "Updates the status of a file change application.

   Returns Result<FileChangeApplication> - the updated record or Failure."
  [conn file-change-application-id status result-message]
  (db/execute-one conn {:update :file_change_application
                        :set {:status status
                              :result_message result-message}
                        :where [:= :id file-change-application-id]
                        :returning [:*]}))
