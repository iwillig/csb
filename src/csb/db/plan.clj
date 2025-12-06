(ns csb.db.plan
  "Database operations for plan entities"
  (:require
   [csb.db :as db]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

(comment
  (type SQLiteConnection))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewPlan
  "Data required to create a new plan.

  Required fields:
  - :name - The name of the plan (String)
  - :context - The context/description of the plan (String)

  Optional fields:
  - :project_id - ID of the associated project (Integer or nil)
  - :plan_state_id - State of the plan (String, defaults to 'created')"
  (t/HMap :mandatory {:name t/Str
                      :context t/Str}
          :optional {:project_id (t/Option t/Int)
                     :plan_state_id t/Str}))

(t/defalias PlanUpdate
  "Data for updating an existing plan. All fields are optional."
  (t/HMap :optional {:name t/Str
                     :context t/Str
                     :project_id (t/Option t/Int)
                     :plan_state_id t/Str}))

(t/defalias Plan
  "Complete plan record as returned from database.

  Fields:
  - :id - Unique identifier (Integer)
  - :name - Plan name (String)
  - :context - Plan context/description (String)
  - :plan_state_id - Current state (String)
  - :created_at - Creation timestamp (String)
  - :updated_at - Last update timestamp (String)
  - :project_id - Associated project ID (Integer or nil)"
  (t/HMap :mandatory {:id t/Int
                      :name t/Str
                      :context t/Str
                      :plan_state_id t/Str
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:project_id (t/Option t/Int)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-plan
       [SQLiteConnection NewPlan :-> Plan])

(t/ann ^:no-check get-plan-by-id
       [SQLiteConnection t/Int :-> (t/Option Plan)])

(t/ann ^:no-check get-plans-by-project-id
       [SQLiteConnection t/Int :-> (t/Seqable Plan)])

(t/ann ^:no-check update-plan
       [SQLiteConnection t/Int PlanUpdate :-> Plan])

(t/ann ^:no-check delete-plan
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-plan
  "Creates a new plan and returns the created plan with its ID.

  The plan-data map should contain:
  - :name (required) - Plan name
  - :context (required) - Plan description/context
  - :project_id (optional) - Associated project ID
  - :plan_state_id (optional) - Initial state (defaults to 'created')

  Returns the complete Plan record with generated ID and timestamps."
  [conn plan-data]
  (let [sql-map {:insert-into :plan
                 :values [plan-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-plan-by-id
  "Retrieves a plan by its ID.

  Returns the Plan record if found, nil otherwise."
  [conn plan-id]
  (let [sql-map {:select [:*]
                 :from [:plan]
                 :where [:= :id plan-id]}]
    (db/execute-one conn sql-map)))

(defn get-plans-by-project-id
  "Retrieves all plans associated with a project.

  Returns a sequence of Plan records."
  [conn project-id]
  (let [sql-map {:select [:*]
                 :from [:plan]
                 :where [:= :project_id project-id]}]
    (db/execute-many conn sql-map)))

(defn update-plan
  "Updates an existing plan and returns the updated record.

  The plan-data map can contain any of:
  - :name - New plan name
  - :context - New context
  - :project_id - New project association
  - :plan_state_id - New state

  Returns the updated Plan record with new timestamp."
  [conn plan-id plan-data]
  (let [sql-map {:update :plan
                 :set plan-data
                 :where [:= :id plan-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-plan
  "Deletes a plan by its ID.

  Returns the number of rows deleted (0 or 1)."
  [conn plan-id]
  (let [sql-map {:delete-from :plan
                 :where [:= :id plan-id]}]
    (db/execute-one conn sql-map)))
