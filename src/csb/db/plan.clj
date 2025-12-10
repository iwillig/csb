(ns csb.db.plan
  "Database operations for plan entities.
   
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
       [SQLiteConnection NewPlan :-> (types/Result Plan)])

(t/ann ^:no-check get-plan-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option Plan))])

(t/ann ^:no-check get-plans-by-project-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable Plan))])

(t/ann ^:no-check update-plan
       [SQLiteConnection t/Int PlanUpdate :-> (types/Result Plan)])

(t/ann ^:no-check delete-plan
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

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

   Returns Result<Plan> - the created record or Failure on database error."
  [conn plan-data]
  (db/execute-one conn {:insert-into :plan
                        :values [plan-data]
                        :returning [:*]}))

(defn get-plan-by-id
  "Retrieves a plan by its ID.

   Returns Result<Plan | nil> - the record, nil if not found, or Failure."
  [conn plan-id]
  (db/execute-one conn {:select [:*]
                        :from [:plan]
                        :where [:= :id plan-id]}))

(defn get-plans-by-project-id
  "Retrieves all plans associated with a project.

   Returns Result<Seq<Plan>> - sequence of records or Failure."
  [conn project-id]
  (db/execute-many conn {:select [:*]
                         :from [:plan]
                         :where [:= :project_id project-id]}))

(defn update-plan
  "Updates an existing plan and returns the updated record.

   The plan-data map can contain any of:
   - :name - New plan name
   - :context - New context
   - :project_id - New project association
   - :plan_state_id - New state

   Returns Result<Plan> - the updated record or Failure."
  [conn plan-id plan-data]
  (db/execute-one conn {:update :plan
                        :set plan-data
                        :where [:= :id plan-id]
                        :returning [:*]}))

(defn delete-plan
  "Deletes a plan by its ID.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn plan-id]
  (db/execute-one conn {:delete-from :plan
                        :where [:= :id plan-id]}))
