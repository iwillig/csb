(ns csb.db.plan-test
  "Tests for plan database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.plan :as db.plan]
   [failjure.core :as f]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Test Data Helpers
;; ============================================================================

(defn get-plan-states
  "Returns all available plan states."
  []
  (db/execute-many
   test-helper/*connection*
   {:select [:*]
    :from [:plan_state]}))

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-plan-with-required-fields
  (t/testing "Creating a plan with only required fields"
    (let [plan-data {:name "My First Plan"
                     :context "This is the context for my plan"}
          created-plan (db.plan/create-plan test-helper/*connection* plan-data)]

      ;; Verify plan was created with ID
      (t/is (integer? (:id created-plan)))
      (t/is (pos? (:id created-plan)))

      ;; Verify required fields
      (t/is (= "My First Plan" (:name created-plan)))
      (t/is (= "This is the context for my plan" (:context created-plan)))

      ;; Verify defaults
      (t/is (= "created" (:plan_state_id created-plan)))
      (t/is (nil? (:project_id created-plan)))

      ;; Verify timestamps
      (t/is (string? (:created_at created-plan)))
      (t/is (string? (:updated_at created-plan)))
      (t/is (= (:created_at created-plan) (:updated_at created-plan))))))

(t/deftest test-create-plan-with-project
  (t/testing "Creating a plan associated with a project"
    (let [project (test-helper/create-test-project)
          plan-data {:name "Project Plan"
                     :context "A plan for the project"
                     :project_id (:id project)}
          created-plan (db.plan/create-plan test-helper/*connection* plan-data)]

      (t/is (= (:id project) (:project_id created-plan)))
      (t/is (= "Project Plan" (:name created-plan)))
      (t/is (= "created" (:plan_state_id created-plan))))))

(t/deftest test-create-plan-with-custom-state
  (t/testing "Creating a plan with a custom plan state"
    (let [plan-data {:name "Researched Plan"
                     :context "Plan that starts in researched state"
                     :plan_state_id "researched"}
          created-plan (db.plan/create-plan test-helper/*connection* plan-data)]

      (t/is (= "researched" (:plan_state_id created-plan))))))

(t/deftest test-create-plan-with-all-fields
  (t/testing "Creating a plan with all optional fields"
    (let [project (test-helper/create-test-project)
          plan-data {:name "Complete Plan"
                     :context "Plan with all fields set"
                     :project_id (:id project)
                     :plan_state_id "file-change-created"}
          created-plan (db.plan/create-plan test-helper/*connection* plan-data)]

      (t/is (= "Complete Plan" (:name created-plan)))
      (t/is (= "Plan with all fields set" (:context created-plan)))
      (t/is (= (:id project) (:project_id created-plan)))
      (t/is (= "file-change-created" (:plan_state_id created-plan))))))

(t/deftest test-get-plan-by-id-exists
  (t/testing "Retrieving an existing plan by ID"
    (let [created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Find Me"
                         :context "I should be findable"})
          retrieved-plan (db.plan/get-plan-by-id
                          test-helper/*connection*
                          (:id created-plan))]

      (t/is (not (nil? retrieved-plan)))
      (t/is (= (:id created-plan) (:id retrieved-plan)))
      (t/is (= "Find Me" (:name retrieved-plan)))
      (t/is (= "I should be findable" (:context retrieved-plan))))))

(t/deftest test-get-plan-by-id-not-exists
  (t/testing "Retrieving a non-existent plan returns nil"
    (let [retrieved-plan (db.plan/get-plan-by-id
                          test-helper/*connection*
                          99999)]
      (t/is (nil? retrieved-plan)))))

(t/deftest test-get-plans-by-project-id-empty
  (t/testing "Getting plans for a project with no plans"
    (let [project (test-helper/create-test-project)
          plans (db.plan/get-plans-by-project-id
                 test-helper/*connection*
                 (:id project))]

      (t/is (empty? plans)))))

(t/deftest test-get-plans-by-project-id-single
  (t/testing "Getting plans for a project with one plan"
    (let [project (test-helper/create-test-project)
          _ (db.plan/create-plan
             test-helper/*connection*
             {:name "Solo Plan"
              :context "Only plan for this project"
              :project_id (:id project)})
          plans (db.plan/get-plans-by-project-id
                 test-helper/*connection*
                 (:id project))]

      (t/is (= 1 (count plans)))
      (t/is (= "Solo Plan" (:name (first plans)))))))

(t/deftest test-get-plans-by-project-id-multiple
  (t/testing "Getting plans for a project with multiple plans"
    (let [project (test-helper/create-test-project)
          _ (db.plan/create-plan
             test-helper/*connection*
             {:name "Plan 1"
              :context "First plan"
              :project_id (:id project)})
          _ (db.plan/create-plan
             test-helper/*connection*
             {:name "Plan 2"
              :context "Second plan"
              :project_id (:id project)})
          _ (db.plan/create-plan
             test-helper/*connection*
             {:name "Plan 3"
              :context "Third plan"
              :project_id (:id project)})
          plans (db.plan/get-plans-by-project-id
                 test-helper/*connection*
                 (:id project))]

      (t/is (= 3 (count plans)))
      (t/is (= #{"Plan 1" "Plan 2" "Plan 3"}
               (set (map :name plans)))))))

(t/deftest test-update-plan-name
  (t/testing "Updating a plan's name"
    (let [created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Old Name"
                         :context "Original context"})
          _original-updated-at (:updated_at created-plan)
          ;; Small delay to ensure timestamp changes
          _ (Thread/sleep 10)
          updated-plan (db.plan/update-plan
                        test-helper/*connection*
                        (:id created-plan)
                        {:name "New Name"})]

      (t/is (= (:id created-plan) (:id updated-plan)))
      (t/is (= "New Name" (:name updated-plan)))
      (t/is (= "Original context" (:context updated-plan)))
      ;; Note: updated_at trigger updates happen after RETURNING clause
      ;; so we verify by re-fetching
      (let [refetched (db.plan/get-plan-by-id
                       test-helper/*connection*
                       (:id updated-plan))]
        (t/is (= "New Name" (:name refetched)))))))

(t/deftest test-update-plan-context
  (t/testing "Updating a plan's context"
    (let [created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Plan Name"
                         :context "Old context"})
          updated-plan (db.plan/update-plan
                        test-helper/*connection*
                        (:id created-plan)
                        {:context "New context"})]

      (t/is (= "Plan Name" (:name updated-plan)))
      (t/is (= "New context" (:context updated-plan))))))

(t/deftest test-update-plan-state
  (t/testing "Updating a plan's state"
    (let [created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "State Change Plan"
                         :context "Testing state transitions"})
          _ (t/is (= "created" (:plan_state_id created-plan)))
          updated-plan (db.plan/update-plan
                        test-helper/*connection*
                        (:id created-plan)
                        {:plan_state_id "researched"})]

      (t/is (= "researched" (:plan_state_id updated-plan))))))

(t/deftest test-update-plan-project
  (t/testing "Updating a plan's project association"
    (let [project1 (test-helper/create-test-project "Project 1" "/path/1")
          project2 (test-helper/create-test-project "Project 2" "/path/2")
          created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Moving Plan"
                         :context "Will move between projects"
                         :project_id (:id project1)})
          _ (t/is (= (:id project1) (:project_id created-plan)))
          updated-plan (db.plan/update-plan
                        test-helper/*connection*
                        (:id created-plan)
                        {:project_id (:id project2)})]

      (t/is (= (:id project2) (:project_id updated-plan))))))

(t/deftest test-update-plan-multiple-fields
  (t/testing "Updating multiple fields at once"
    (let [project (test-helper/create-test-project)
          created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Original"
                         :context "Original context"})
          updated-plan (db.plan/update-plan
                        test-helper/*connection*
                        (:id created-plan)
                        {:name "Updated"
                         :context "Updated context"
                         :project_id (:id project)
                         :plan_state_id "completed"})]

      (t/is (= "Updated" (:name updated-plan)))
      (t/is (= "Updated context" (:context updated-plan)))
      (t/is (= (:id project) (:project_id updated-plan)))
      (t/is (= "completed" (:plan_state_id updated-plan))))))

(t/deftest test-delete-plan
  (t/testing "Deleting a plan"
    (let [created-plan (db.plan/create-plan
                        test-helper/*connection*
                        {:name "Doomed Plan"
                         :context "Will be deleted"})
          plan-id (:id created-plan)
          _ (db.plan/delete-plan test-helper/*connection* plan-id)
          retrieved-plan (db.plan/get-plan-by-id
                          test-helper/*connection*
                          plan-id)]

      (t/is (nil? retrieved-plan)))))

(t/deftest test-delete-non-existent-plan
  (t/testing "Deleting a non-existent plan returns update-count of 0"
    (let [result (db.plan/delete-plan test-helper/*connection* 99999)]
      (t/is (= 0 (:next.jdbc/update-count result))))))

;; ============================================================================
;; Database Constraint Tests
;; ============================================================================

(t/deftest test-invalid-plan-state-reference
  (t/testing "Creating a plan with invalid plan_state_id should fail"
    ;; Enable foreign key constraints
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (let [result (db.plan/create-plan
                  test-helper/*connection*
                  {:name "Invalid State Plan"
                   :context "Uses non-existent state"
                   :plan_state_id "non-existent-state"})]
      (t/is (f/failed? result))
      (t/is (instance? org.sqlite.SQLiteException (f/message result))))))

(t/deftest test-invalid-project-reference
  (t/testing "Creating a plan with invalid project_id should fail"
    ;; Enable foreign key constraints
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (let [result (db.plan/create-plan
                  test-helper/*connection*
                  {:name "Invalid Project Plan"
                   :context "References non-existent project"
                   :project_id 99999})]
      (t/is (f/failed? result))
      (t/is (instance? org.sqlite.SQLiteException (f/message result))))))

(t/deftest test-plan-states-are-prepopulated
  (t/testing "Verify all expected plan states exist"
    (let [states (get-plan-states)
          state-ids (set (map :id states))]

      (t/is (= 7 (count states)))
      (t/is (contains? state-ids "created"))
      (t/is (contains? state-ids "researched"))
      (t/is (contains? state-ids "file-change-created"))
      (t/is (contains? state-ids "file-changes-reviewed"))
      (t/is (contains? state-ids "file-changes-applied"))
      (t/is (contains? state-ids "results-reviewed"))
      (t/is (contains? state-ids "completed")))))

;; ============================================================================
;; Full-Text Search Tests
;; ============================================================================

(t/deftest test-plan-fts-insert-trigger
  (t/testing "FTS table is populated when plan is created"
    (let [plan (db.plan/create-plan
                test-helper/*connection*
                {:name "FTS Test Plan"
                 :context "This is searchable context with uniqueword12345"})
          ;; Query the FTS table directly using raw SQL for MATCH
          fts-results (db/execute-many
                       test-helper/*connection*
                       {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'uniqueword12345'"})]

      (t/is (= 1 (count fts-results)))
      (t/is (= (:id plan) (:rowid (first fts-results)))))))

(t/deftest test-plan-fts-update-trigger
  (t/testing "FTS table is updated when plan context changes"
    (let [plan (db.plan/create-plan
                test-helper/*connection*
                {:name "FTS Update Test"
                 :context "Original searchableoriginal7890"})
          ;; Verify original is searchable
          original-search (db/execute-many
                           test-helper/*connection*
                           {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'searchableoriginal7890'"})
          _ (t/is (= 1 (count original-search)))

          ;; Update the plan
          _ (db.plan/update-plan
             test-helper/*connection*
             (:id plan)
             {:context "Updated searchableupdated6789"})

          ;; Old content should not be found
          old-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'searchableoriginal7890'"})

          ;; New content should be found
          new-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'searchableupdated6789'"})]

      (t/is (empty? old-search))
      (t/is (= 1 (count new-search)))
      (t/is (= (:id plan) (:rowid (first new-search)))))))

(t/deftest test-plan-fts-delete-trigger
  (t/testing "FTS table entry is removed when plan is deleted"
    (let [plan (db.plan/create-plan
                test-helper/*connection*
                {:name "FTS Delete Test"
                 :context "Will be deleted searchabledelete4567"})
          plan-id (:id plan)

          ;; Verify it's searchable
          before-delete (db/execute-many
                         test-helper/*connection*
                         {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'searchabledelete4567'"})
          _ (t/is (= 1 (count before-delete)))

          ;; Delete the plan
          _ (db.plan/delete-plan test-helper/*connection* plan-id)

          ;; Should no longer be searchable
          after-delete (db/execute-many
                        test-helper/*connection*
                        {:raw "SELECT rowid FROM plan_fts WHERE plan_fts MATCH 'searchabledelete4567'"})]

      (t/is (empty? after-delete)))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-plan-lifecycle
  (t/testing "Complete plan lifecycle through states"
    (let [project (test-helper/create-test-project)
          ;; Create plan
          plan (db.plan/create-plan
                test-helper/*connection*
                {:name "Lifecycle Plan"
                 :context "Testing full lifecycle"
                 :project_id (:id project)})
          _ (t/is (= "created" (:plan_state_id plan)))

          ;; Transition to researched
          plan (db.plan/update-plan
                test-helper/*connection*
                (:id plan)
                {:plan_state_id "researched"})
          _ (t/is (= "researched" (:plan_state_id plan)))

          ;; Transition to file-change-created
          plan (db.plan/update-plan
                test-helper/*connection*
                (:id plan)
                {:plan_state_id "file-change-created"})
          _ (t/is (= "file-change-created" (:plan_state_id plan)))

          ;; Transition to completed
          plan (db.plan/update-plan
                test-helper/*connection*
                (:id plan)
                {:plan_state_id "completed"})
          _ (t/is (= "completed" (:plan_state_id plan)))

          ;; Verify final state
          final-plan (db.plan/get-plan-by-id
                      test-helper/*connection*
                      (:id plan))]

      (t/is (= "completed" (:plan_state_id final-plan)))
      (t/is (= "Lifecycle Plan" (:name final-plan))))))

(t/deftest test-multiple-plans-same-project
  (t/testing "A project can have multiple plans"
    (let [project (test-helper/create-test-project)
          plan1 (db.plan/create-plan
                 test-helper/*connection*
                 {:name "Feature A"
                  :context "Implement feature A"
                  :project_id (:id project)})
          plan2 (db.plan/create-plan
                 test-helper/*connection*
                 {:name "Feature B"
                  :context "Implement feature B"
                  :project_id (:id project)})
          plan3 (db.plan/create-plan
                 test-helper/*connection*
                 {:name "Bugfix C"
                  :context "Fix bug C"
                  :project_id (:id project)})
          all-plans (db.plan/get-plans-by-project-id
                     test-helper/*connection*
                     (:id project))]

      (t/is (= 3 (count all-plans)))
      (t/is (= #{(:id plan1) (:id plan2) (:id plan3)}
               (set (map :id all-plans))))
      (t/is (every? #(= (:id project) (:project_id %)) all-plans)))))

(t/deftest test-plan-without-project
  (t/testing "Plans can exist without being associated to a project"
    (let [plan (db.plan/create-plan
                test-helper/*connection*
                {:name "Standalone Plan"
                 :context "Not associated with any project"})]

      (t/is (nil? (:project_id plan)))
      (t/is (= "Standalone Plan" (:name plan))))))
