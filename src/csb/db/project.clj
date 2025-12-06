(ns csb.db.project
  "Database operations for project entities"
  (:require
   [csb.db :as db]
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
       [SQLiteConnection NewProject :-> Project])

(t/ann ^:no-check get-project-by-id
       [SQLiteConnection t/Int :-> (t/Option Project)])

(t/ann ^:no-check get-project-by-path
       [SQLiteConnection t/Str :-> (t/Option Project)])

(t/ann ^:no-check get-all-projects
       [SQLiteConnection :-> (t/Seqable Project)])

(t/ann ^:no-check update-project
       [SQLiteConnection t/Int ProjectUpdate :-> Project])

(t/ann ^:no-check delete-project
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-project
  "Creates a new project and returns the created project with its ID.

  The project-data map should contain:
  - :name (required) - Project name
  - :path (required) - Filesystem path to the project
  - :description (optional) - Project description

  Returns the complete Project record with generated ID and timestamps."
  [conn project-data]
  (let [sql-map {:insert-into :project
                 :values [project-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-project-by-id
  "Retrieves a project by its ID.

  Returns the Project record if found, nil otherwise."
  [conn project-id]
  (let [sql-map {:select [:*]
                 :from [:project]
                 :where [:= :id project-id]}]
    (db/execute-one conn sql-map)))

(defn get-project-by-path
  "Retrieves a project by its filesystem path.

  Returns the Project record if found, nil otherwise."
  [conn path]
  (let [sql-map {:select [:*]
                 :from [:project]
                 :where [:= :path path]}]
    (db/execute-one conn sql-map)))

(defn get-all-projects
  "Retrieves all projects.

  Returns a sequence of Project records."
  [conn]
  (let [sql-map {:select [:*]
                 :from [:project]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn update-project
  "Updates an existing project and returns the updated record.

  The project-data map can contain any of:
  - :name - New project name
  - :path - New filesystem path
  - :description - New description

  Returns the updated Project record with new timestamp."
  [conn project-id project-data]
  (let [sql-map {:update :project
                 :set project-data
                 :where [:= :id project-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-project
  "Deletes a project by its ID.

  Note: This will cascade delete all associated plans, files, etc.

  Returns the number of rows deleted (0 or 1)."
  [conn project-id]
  (let [sql-map {:delete-from :project
                 :where [:= :id project-id]}]
    (db/execute-one conn sql-map)))
