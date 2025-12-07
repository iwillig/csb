(ns csb.db.task
  "Database operations for task entities"
  (:require
   [csb.db :as db]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewTask
  "Data required to create a new task.

  Required fields:
  - :plan_id - ID of the associated plan (Integer)
  - :name - The name of the task (String)
  - :context - The context/description of the task (String)

  Optional fields:
  - :parent_id - ID of parent task for subtasks (Integer or nil)
  - :completed - Whether task is completed (Boolean, defaults to false)"
  (t/HMap :mandatory {:plan_id t/Int
                      :name t/Str
                      :context t/Str}
          :optional {:parent_id (t/Option t/Int)
                     :completed t/Bool}))

(t/defalias TaskUpdate
  "Data for updating an existing task. All fields are optional."
  (t/HMap :optional {:name t/Str
                     :context t/Str
                     :parent_id (t/Option t/Int)
                     :completed t/Bool}))

(t/defalias Task
  "Complete task record as returned from database.

  Fields:
  - :id - Unique identifier (Integer)
  - :plan_id - Associated plan ID (Integer)
  - :name - Task name (String)
  - :context - Task context/description (String)
  - :completed - Completion status (Boolean)
  - :created_at - Creation timestamp (String)
  - :updated_at - Last update timestamp (String)
  - :parent_id - Parent task ID (Integer or nil)"
  (t/HMap :mandatory {:id t/Int
                      :plan_id t/Int
                      :name t/Str
                      :context t/Str
                      :completed t/Bool
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:parent_id (t/Option t/Int)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-task
       [SQLiteConnection NewTask :-> Task])

(t/ann ^:no-check get-task-by-id
       [SQLiteConnection t/Int :-> (t/Option Task)])

(t/ann ^:no-check get-tasks-by-plan-id
       [SQLiteConnection t/Int :-> (t/Seqable Task)])

(t/ann ^:no-check get-child-tasks
       [SQLiteConnection t/Int :-> (t/Seqable Task)])

(t/ann ^:no-check get-root-tasks
       [SQLiteConnection t/Int :-> (t/Seqable Task)])

(t/ann ^:no-check update-task
       [SQLiteConnection t/Int TaskUpdate :-> Task])

(t/ann ^:no-check mark-completed
       [SQLiteConnection t/Int :-> Task])

(t/ann ^:no-check mark-incomplete
       [SQLiteConnection t/Int :-> Task])

(t/ann ^:no-check delete-task
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-task
  "Creates a new task and returns the created task with its ID.

  The task-data map should contain:
  - :plan_id (required) - Associated plan ID
  - :name (required) - Task name
  - :context (required) - Task description/context
  - :parent_id (optional) - Parent task ID for subtasks
  - :completed (optional) - Completion status (defaults to false)

  Returns the complete Task record with generated ID and timestamps."
  [conn task-data]
  (let [sql-map {:insert-into :task
                 :values [task-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-task-by-id
  "Retrieves a task by its ID.

  Returns the Task record if found, nil otherwise."
  [conn task-id]
  (let [sql-map {:select [:*]
                 :from [:task]
                 :where [:= :id task-id]}]
    (db/execute-one conn sql-map)))

(defn get-tasks-by-plan-id
  "Retrieves all tasks associated with a plan.

  Returns a sequence of Task records ordered by creation time."
  [conn plan-id]
  (let [sql-map {:select [:*]
                 :from [:task]
                 :where [:= :plan_id plan-id]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn get-child-tasks
  "Retrieves all child tasks (subtasks) of a given task.

  Returns a sequence of Task records ordered by creation time."
  [conn parent-task-id]
  (let [sql-map {:select [:*]
                 :from [:task]
                 :where [:= :parent_id parent-task-id]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn get-root-tasks
  "Retrieves all root-level tasks (tasks without parents) for a plan.

  Returns a sequence of Task records ordered by creation time."
  [conn plan-id]
  (let [sql-map {:select [:*]
                 :from [:task]
                 :where [:and
                         [:= :plan_id plan-id]
                         [:is :parent_id nil]]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn update-task
  "Updates an existing task and returns the updated record.

  The task-data map can contain any of:
  - :name - New task name
  - :context - New context
  - :parent_id - New parent task
  - :completed - New completion status

  Returns the updated Task record with new timestamp."
  [conn task-id task-data]
  (let [sql-map {:update :task
                 :set task-data
                 :where [:= :id task-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn mark-completed
  "Marks a task as completed.

  Returns the updated Task record."
  [conn task-id]
  (update-task conn task-id {:completed true}))

(defn mark-incomplete
  "Marks a task as incomplete.

  Returns the updated Task record."
  [conn task-id]
  (update-task conn task-id {:completed false}))

(defn delete-task
  "Deletes a task by its ID.

  Note: This will cascade delete all child tasks.

  Returns the number of rows deleted (0 or 1)."
  [conn task-id]
  (let [sql-map {:delete-from :task
                 :where [:= :id task-id]}]
    (db/execute-one conn sql-map)))
