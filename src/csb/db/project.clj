(ns csb.db.project
  "Database operations for project entities.
   
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

(t/defalias NewProject
  "Data required to create a new project.

   Required fields:
   - :name - The name of the project (String)
   - :path - The filesystem path to the project (String)

   Optional fields:
   - :description - Project description (String or nil)"
  (t/HMap :mandatory {:name t/Str
                      :path t/Str}
          :optional {:description (t/Option t/Str)}))

(t/defalias ProjectUpdate
  "Data for updating an existing project. All fields are optional."
  (t/HMap :optional {:name t/Str
                     :path t/Str
                     :description (t/Option t/Str)}))

(t/defalias Project
  "Complete project record as returned from database.

   Fields:
   - :id - Unique identifier (Integer)
   - :name - Project name (String)
   - :path - Filesystem path (String)
   - :description - Project description (String or nil)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :name t/Str
                      :path t/Str
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:description (t/Option t/Str)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-project
       [SQLiteConnection NewProject :-> (types/Result Project)])

(t/ann ^:no-check get-project-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option Project))])

(t/ann ^:no-check get-project-by-path
       [SQLiteConnection t/Str :-> (types/Result (t/Option Project))])

(t/ann ^:no-check get-all-projects
       [SQLiteConnection :-> (types/Result (t/Seqable Project))])

(t/ann ^:no-check update-project
       [SQLiteConnection t/Int ProjectUpdate :-> (types/Result Project)])

(t/ann ^:no-check delete-project
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-project
  "Creates a new project and returns the created project with its ID.

   The project-data map should contain:
   - :name (required) - Project name
   - :path (required) - Filesystem path to the project
   - :description (optional) - Project description

   Returns Result<Project> - the created record or Failure on database error."
  [conn project-data]
  (db/execute-one conn {:insert-into :project
                        :values [project-data]
                        :returning [:*]}))

(defn get-project-by-id
  "Retrieves a project by its ID.

   Returns Result<Project | nil> - the record, nil if not found, or Failure."
  [conn project-id]
  (db/execute-one conn {:select [:*]
                        :from [:project]
                        :where [:= :id project-id]}))

(defn get-project-by-path
  "Retrieves a project by its filesystem path.

   Returns Result<Project | nil> - the record, nil if not found, or Failure."
  [conn path]
  (db/execute-one conn {:select [:*]
                        :from [:project]
                        :where [:= :path path]}))

(defn get-all-projects
  "Retrieves all projects.

   Returns Result<Seq<Project>> - sequence of records or Failure."
  [conn]
  (db/execute-many conn {:select [:*]
                         :from [:project]
                         :order-by [[:created_at :asc]]}))

(defn update-project
  "Updates an existing project and returns the updated record.

   The project-data map can contain any of:
   - :name - New project name
   - :path - New filesystem path
   - :description - New description

   Returns Result<Project> - the updated record or Failure."
  [conn project-id project-data]
  (db/execute-one conn {:update :project
                        :set project-data
                        :where [:= :id project-id]
                        :returning [:*]}))

(defn delete-project
  "Deletes a project by its ID.

   Note: This will cascade delete all associated plans, files, etc.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn project-id]
  (db/execute-one conn {:delete-from :project
                        :where [:= :id project-id]}))
