(ns csb.db.task-test
  "Tests for task database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.task :as db.task]
   [csb.db.plan :as db.plan]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Test Data Helpers
;; ============================================================================

(defn create-test-plan
  "Creates a test plan and returns it."
  ([]
   (create-test-plan "Test Plan" "Test context"))
  ([name context]
   (db.plan/create-plan
    test-helper/*connection*
    {:name name
     :context context})))

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-task-with-required-fields
  (t/testing "Creating a task with only required fields"
    (let [plan (create-test-plan)
          task-data {:plan_id (:id plan)
                     :name "My First Task"
                     :context "This is the context for my task"}
          created-task (db.task/create-task test-helper/*connection* task-data)]

      ;; Verify task was created with ID
      (t/is (integer? (:id created-task)))
      (t/is (pos? (:id created-task)))

      ;; Verify required fields
      (t/is (= "My First Task" (:name created-task)))
      (t/is (= "This is the context for my task" (:context created-task)))
      (t/is (= (:id plan) (:plan_id created-task)))

      ;; Verify defaults (SQLite stores booleans as 0/1)
      (t/is (zero? (:completed created-task)))
      (t/is (nil? (:parent_id created-task)))

      ;; Verify timestamps
      (t/is (string? (:created_at created-task)))
      (t/is (string? (:updated_at created-task)))
      (t/is (= (:created_at created-task) (:updated_at created-task))))))

(t/deftest test-create-task-with-parent
  (t/testing "Creating a subtask with a parent task"
    (let [plan (create-test-plan)
          parent-task (db.task/create-task
                       test-helper/*connection*
                       {:plan_id (:id plan)
                        :name "Parent Task"
                        :context "Parent context"})
          child-task (db.task/create-task
                      test-helper/*connection*
                      {:plan_id (:id plan)
                       :name "Child Task"
                       :context "Child context"
                       :parent_id (:id parent-task)})]

      (t/is (= (:id parent-task) (:parent_id child-task)))
      (t/is (= "Child Task" (:name child-task)))
      (t/is (= (:id plan) (:plan_id child-task))))))

(t/deftest test-create-task-with-completed-true
  (t/testing "Creating a task that is already completed"
    (let [plan (create-test-plan)
          task-data {:plan_id (:id plan)
                     :name "Already Done"
                     :context "This task is pre-completed"
                     :completed true}
          created-task (db.task/create-task test-helper/*connection* task-data)]

      ;; SQLite stores true as 1
      (t/is (pos? (:completed created-task))))))

(t/deftest test-get-task-by-id-exists
  (t/testing "Retrieving an existing task by ID"
    (let [plan (create-test-plan)
          created-task (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Find Me"
                         :context "I should be findable"})
          retrieved-task (db.task/get-task-by-id
                          test-helper/*connection*
                          (:id created-task))]

      (t/is (not (nil? retrieved-task)))
      (t/is (= (:id created-task) (:id retrieved-task)))
      (t/is (= "Find Me" (:name retrieved-task)))
      (t/is (= "I should be findable" (:context retrieved-task))))))

(t/deftest test-get-task-by-id-not-exists
  (t/testing "Retrieving a non-existent task returns nil"
    (let [retrieved-task (db.task/get-task-by-id
                          test-helper/*connection*
                          99999)]
      (t/is (nil? retrieved-task)))))

(t/deftest test-get-tasks-by-plan-id-empty
  (t/testing "Getting tasks for a plan with no tasks"
    (let [plan (create-test-plan)
          tasks (db.task/get-tasks-by-plan-id
                 test-helper/*connection*
                 (:id plan))]

      (t/is (empty? tasks)))))

(t/deftest test-get-tasks-by-plan-id-multiple
  (t/testing "Getting tasks for a plan with multiple tasks"
    (let [plan (create-test-plan)
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Task 1"
              :context "First task"})
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Task 2"
              :context "Second task"})
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Task 3"
              :context "Third task"})
          tasks (db.task/get-tasks-by-plan-id
                 test-helper/*connection*
                 (:id plan))]

      (t/is (= 3 (count tasks)))
      (t/is (= #{"Task 1" "Task 2" "Task 3"}
               (set (map :name tasks)))))))

(t/deftest test-get-child-tasks-empty
  (t/testing "Getting child tasks when there are none"
    (let [plan (create-test-plan)
          parent-task (db.task/create-task
                       test-helper/*connection*
                       {:plan_id (:id plan)
                        :name "Parent"
                        :context "No children"})
          children (db.task/get-child-tasks
                    test-helper/*connection*
                    (:id parent-task))]

      (t/is (empty? children)))))

(t/deftest test-get-child-tasks-multiple
  (t/testing "Getting multiple child tasks"
    (let [plan (create-test-plan)
          parent (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Parent"
                   :context "Has children"})
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Child 1"
              :context "First child"
              :parent_id (:id parent)})
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Child 2"
              :context "Second child"
              :parent_id (:id parent)})
          children (db.task/get-child-tasks
                    test-helper/*connection*
                    (:id parent))]

      (t/is (= 2 (count children)))
      (t/is (= #{"Child 1" "Child 2"}
               (set (map :name children))))
      (t/is (every? #(= (:id parent) (:parent_id %)) children)))))

(t/deftest test-get-root-tasks
  (t/testing "Getting only root-level tasks (no parent)"
    (let [plan (create-test-plan)
          root1 (db.task/create-task
                 test-helper/*connection*
                 {:plan_id (:id plan)
                  :name "Root 1"
                  :context "First root"})
          root2 (db.task/create-task
                 test-helper/*connection*
                 {:plan_id (:id plan)
                  :name "Root 2"
                  :context "Second root"})
          ;; Create child tasks
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Child 1"
              :context "Child of root 1"
              :parent_id (:id root1)})
          _ (db.task/create-task
             test-helper/*connection*
             {:plan_id (:id plan)
              :name "Child 2"
              :context "Child of root 2"
              :parent_id (:id root2)})
          root-tasks (db.task/get-root-tasks
                      test-helper/*connection*
                      (:id plan))]

      (t/is (= 2 (count root-tasks)))
      (t/is (= #{"Root 1" "Root 2"}
               (set (map :name root-tasks))))
      (t/is (every? #(nil? (:parent_id %)) root-tasks)))))

(t/deftest test-update-task-name
  (t/testing "Updating a task's name"
    (let [plan (create-test-plan)
          created-task (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Old Name"
                         :context "Original context"})
          updated-task (db.task/update-task
                        test-helper/*connection*
                        (:id created-task)
                        {:name "New Name"})]

      (t/is (= (:id created-task) (:id updated-task)))
      (t/is (= "New Name" (:name updated-task)))
      (t/is (= "Original context" (:context updated-task))))))

(t/deftest test-update-task-completed
  (t/testing "Updating a task's completion status"
    (let [plan (create-test-plan)
          created-task (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Task"
                         :context "To be completed"})
          _ (t/is (zero? (:completed created-task)))
          updated-task (db.task/update-task
                        test-helper/*connection*
                        (:id created-task)
                        {:completed true})]

      (t/is (pos? (:completed updated-task))))))

(t/deftest test-mark-completed
  (t/testing "Marking a task as completed"
    (let [plan (create-test-plan)
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "To Do"
                 :context "Needs completion"})
          _ (t/is (zero? (:completed task)))
          completed-task (db.task/mark-completed
                          test-helper/*connection*
                          (:id task))]

      (t/is (pos? (:completed completed-task))))))

(t/deftest test-mark-incomplete
  (t/testing "Marking a task as incomplete"
    (let [plan (create-test-plan)
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "Done"
                 :context "Already completed"
                 :completed true})
          _ (t/is (pos? (:completed task)))
          incomplete-task (db.task/mark-incomplete
                           test-helper/*connection*
                           (:id task))]

      (t/is (zero? (:completed incomplete-task))))))

(t/deftest test-delete-task
  (t/testing "Deleting a task"
    (let [plan (create-test-plan)
          created-task (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Doomed Task"
                         :context "Will be deleted"})
          task-id (:id created-task)
          _ (db.task/delete-task test-helper/*connection* task-id)
          retrieved-task (db.task/get-task-by-id
                          test-helper/*connection*
                          task-id)]

      (t/is (nil? retrieved-task)))))

(t/deftest test-delete-task-cascades-to-children
  (t/testing "Deleting a parent task deletes all children"
    ;; Enable foreign keys for cascade delete
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (let [plan (create-test-plan)
          parent (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Parent"
                   :context "Will be deleted with children"})
          child1 (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Child 1"
                   :context "Will be cascade deleted"
                   :parent_id (:id parent)})
          child2 (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Child 2"
                   :context "Will be cascade deleted"
                   :parent_id (:id parent)})
          ;; Delete parent
          _ (db.task/delete-task test-helper/*connection* (:id parent))
          ;; Check parent is gone
          retrieved-parent (db.task/get-task-by-id
                            test-helper/*connection*
                            (:id parent))
          ;; Check children are gone
          retrieved-child1 (db.task/get-task-by-id
                            test-helper/*connection*
                            (:id child1))
          retrieved-child2 (db.task/get-task-by-id
                            test-helper/*connection*
                            (:id child2))]

      (t/is (nil? retrieved-parent))
      (t/is (nil? retrieved-child1))
      (t/is (nil? retrieved-child2)))))

;; ============================================================================
;; Database Constraint Tests
;; ============================================================================

(t/deftest test-invalid-plan-reference
  (t/testing "Creating a task with invalid plan_id should fail"
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (t/is (thrown? org.sqlite.SQLiteException
                   (db.task/create-task
                    test-helper/*connection*
                    {:plan_id 99999
                     :name "Invalid Plan Task"
                     :context "References non-existent plan"})))))

(t/deftest test-invalid-parent-task-reference
  (t/testing "Creating a task with invalid parent_id should fail"
    (let [plan (create-test-plan)]
      (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
      (t/is (thrown? org.sqlite.SQLiteException
                     (db.task/create-task
                      test-helper/*connection*
                      {:plan_id (:id plan)
                       :name "Invalid Parent Task"
                       :context "References non-existent parent"
                       :parent_id 99999}))))))

;; ============================================================================
;; Full-Text Search Tests
;; ============================================================================

(t/deftest test-task-fts-insert-trigger
  (t/testing "FTS table is populated when task is created"
    (let [plan (create-test-plan)
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "FTS Test Task"
                 :context "This is searchable context with uniquetaskword12345"})
          fts-results (db/execute-many
                       test-helper/*connection*
                       {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'uniquetaskword12345'"})]

      (t/is (= 1 (count fts-results)))
      (t/is (= (:id task) (:rowid (first fts-results)))))))

(t/deftest test-task-fts-update-trigger
  (t/testing "FTS table is updated when task context changes"
    (let [plan (create-test-plan)
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "FTS Update Test"
                 :context "Original searchtaskoriginal7890"})
          ;; Verify original is searchable
          original-search (db/execute-many
                           test-helper/*connection*
                           {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'searchtaskoriginal7890'"})
          _ (t/is (= 1 (count original-search)))

          ;; Update the task
          _ (db.task/update-task
             test-helper/*connection*
             (:id task)
             {:context "Updated searchtaskupdated6789"})

          ;; Old content should not be found
          old-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'searchtaskoriginal7890'"})

          ;; New content should be found
          new-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'searchtaskupdated6789'"})]

      (t/is (empty? old-search))
      (t/is (= 1 (count new-search)))
      (t/is (= (:id task) (:rowid (first new-search)))))))

(t/deftest test-task-fts-delete-trigger
  (t/testing "FTS table entry is removed when task is deleted"
    (let [plan (create-test-plan)
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "FTS Delete Test"
                 :context "Will be deleted searchtaskdelete4567"})
          task-id (:id task)

          ;; Verify it's searchable
          before-delete (db/execute-many
                         test-helper/*connection*
                         {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'searchtaskdelete4567'"})
          _ (t/is (= 1 (count before-delete)))

          ;; Delete the task
          _ (db.task/delete-task test-helper/*connection* task-id)

          ;; Should no longer be searchable
          after-delete (db/execute-many
                        test-helper/*connection*
                        {:raw "SELECT rowid FROM task_fts WHERE task_fts MATCH 'searchtaskdelete4567'"})]

      (t/is (empty? after-delete)))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-task-hierarchy
  (t/testing "Building a task hierarchy with multiple levels"
    (let [plan (create-test-plan)
          ;; Create root task
          root (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "Root Task"
                 :context "Top level"})
          ;; Create level 1 children
          child1 (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Child 1"
                   :context "Level 1"
                   :parent_id (:id root)})
          child2 (db.task/create-task
                  test-helper/*connection*
                  {:plan_id (:id plan)
                   :name "Child 2"
                   :context "Level 1"
                   :parent_id (:id root)})
          ;; Create level 2 children (grandchildren of root)
          _grandchild1 (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Grandchild 1"
                         :context "Level 2"
                         :parent_id (:id child1)})
          _grandchild2 (db.task/create-task
                        test-helper/*connection*
                        {:plan_id (:id plan)
                         :name "Grandchild 2"
                         :context "Level 2"
                         :parent_id (:id child2)})

          ;; Verify hierarchy
          root-tasks (db.task/get-root-tasks test-helper/*connection* (:id plan))
          level1-children (db.task/get-child-tasks test-helper/*connection* (:id root))
          child1-children (db.task/get-child-tasks test-helper/*connection* (:id child1))
          child2-children (db.task/get-child-tasks test-helper/*connection* (:id child2))]

      ;; Verify root level
      (t/is (= 1 (count root-tasks)))
      (t/is (= "Root Task" (:name (first root-tasks))))

      ;; Verify level 1
      (t/is (= 2 (count level1-children)))
      (t/is (= #{"Child 1" "Child 2"} (set (map :name level1-children))))

      ;; Verify level 2
      (t/is (= 1 (count child1-children)))
      (t/is (= "Grandchild 1" (:name (first child1-children))))
      (t/is (= 1 (count child2-children)))
      (t/is (= "Grandchild 2" (:name (first child2-children)))))))

(t/deftest test-task-completion-workflow
  (t/testing "Complete workflow of task creation and completion"
    (let [plan (create-test-plan)
          ;; Create task
          task (db.task/create-task
                test-helper/*connection*
                {:plan_id (:id plan)
                 :name "Workflow Task"
                 :context "Testing complete workflow"})
          _ (t/is (zero? (:completed task)))

          ;; Mark as completed
          completed (db.task/mark-completed test-helper/*connection* (:id task))
          _ (t/is (pos? (:completed completed)))

          ;; Mark as incomplete again
          incomplete (db.task/mark-incomplete test-helper/*connection* (:id task))
          _ (t/is (zero? (:completed incomplete)))

          ;; Final state check
          final-task (db.task/get-task-by-id test-helper/*connection* (:id task))]

      (t/is (zero? (:completed final-task)))
      (t/is (= "Workflow Task" (:name final-task))))))
