(ns csb.db.project-test
  "Tests for project database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.project :as db.project]
   [csb.db.plan :as db.plan]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-project-with-required-fields
  (t/testing "Creating a project with only required fields"
    (let [project-data {:name "My Project"
                        :path "/home/user/my-project"}
          created-project (db.project/create-project test-helper/*connection* project-data)]
      
      ;; Verify project was created with ID
      (t/is (integer? (:id created-project)))
      (t/is (pos? (:id created-project)))
      
      ;; Verify required fields
      (t/is (= "My Project" (:name created-project)))
      (t/is (= "/home/user/my-project" (:path created-project)))
      
      ;; Verify optional fields are nil
      (t/is (nil? (:description created-project)))
      
      ;; Verify timestamps
      (t/is (string? (:created_at created-project)))
      (t/is (string? (:updated_at created-project)))
      (t/is (= (:created_at created-project) (:updated_at created-project))))))

(t/deftest test-create-project-with-description
  (t/testing "Creating a project with a description"
    (let [project-data {:name "Documented Project"
                        :path "/projects/documented"
                        :description "A well-documented project"}
          created-project (db.project/create-project test-helper/*connection* project-data)]
      
      (t/is (= "Documented Project" (:name created-project)))
      (t/is (= "/projects/documented" (:path created-project)))
      (t/is (= "A well-documented project" (:description created-project))))))

(t/deftest test-create-project-with-all-fields
  (t/testing "Creating a project with all fields set"
    (let [project-data {:name "Complete Project"
                        :path "/complete/project/path"
                        :description "This project has everything"}
          created-project (db.project/create-project test-helper/*connection* project-data)]
      
      (t/is (= "Complete Project" (:name created-project)))
      (t/is (= "/complete/project/path" (:path created-project)))
      (t/is (= "This project has everything" (:description created-project))))))

(t/deftest test-get-project-by-id-exists
  (t/testing "Retrieving an existing project by ID"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Find Me"
                            :path "/findable/project"})
          retrieved-project (db.project/get-project-by-id
                             test-helper/*connection*
                             (:id created-project))]
      
      (t/is (not (nil? retrieved-project)))
      (t/is (= (:id created-project) (:id retrieved-project)))
      (t/is (= "Find Me" (:name retrieved-project)))
      (t/is (= "/findable/project" (:path retrieved-project))))))

(t/deftest test-get-project-by-id-not-exists
  (t/testing "Retrieving a non-existent project returns nil"
    (let [retrieved-project (db.project/get-project-by-id
                             test-helper/*connection*
                             99999)]
      (t/is (nil? retrieved-project)))))

(t/deftest test-get-project-by-path-exists
  (t/testing "Retrieving an existing project by path"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Path Project"
                            :path "/unique/path/to/project"})
          retrieved-project (db.project/get-project-by-path
                             test-helper/*connection*
                             "/unique/path/to/project")]
      
      (t/is (not (nil? retrieved-project)))
      (t/is (= (:id created-project) (:id retrieved-project)))
      (t/is (= "Path Project" (:name retrieved-project))))))

(t/deftest test-get-project-by-path-not-exists
  (t/testing "Retrieving a project by non-existent path returns nil"
    (let [retrieved-project (db.project/get-project-by-path
                             test-helper/*connection*
                             "/non/existent/path")]
      (t/is (nil? retrieved-project)))))

(t/deftest test-get-all-projects-empty
  (t/testing "Getting all projects when none exist"
    (let [projects (db.project/get-all-projects test-helper/*connection*)]
      (t/is (empty? projects)))))

(t/deftest test-get-all-projects-single
  (t/testing "Getting all projects with one project"
    (let [_ (db.project/create-project
             test-helper/*connection*
             {:name "Solo Project"
              :path "/solo"})
          projects (db.project/get-all-projects test-helper/*connection*)]
      
      (t/is (= 1 (count projects)))
      (t/is (= "Solo Project" (:name (first projects)))))))

(t/deftest test-get-all-projects-multiple
  (t/testing "Getting all projects with multiple projects"
    (let [_ (db.project/create-project
             test-helper/*connection*
             {:name "Project 1"
              :path "/project1"})
          _ (db.project/create-project
             test-helper/*connection*
             {:name "Project 2"
              :path "/project2"})
          _ (db.project/create-project
             test-helper/*connection*
             {:name "Project 3"
              :path "/project3"})
          projects (db.project/get-all-projects test-helper/*connection*)]
      
      (t/is (= 3 (count projects)))
      (t/is (= #{"Project 1" "Project 2" "Project 3"}
               (set (map :name projects)))))))

(t/deftest test-get-all-projects-ordered-by-created
  (t/testing "Projects are returned ordered by creation time"
    (let [p1 (db.project/create-project
              test-helper/*connection*
              {:name "First"
               :path "/first"})
          _ (Thread/sleep 10)
          p2 (db.project/create-project
              test-helper/*connection*
              {:name "Second"
               :path "/second"})
          _ (Thread/sleep 10)
          p3 (db.project/create-project
              test-helper/*connection*
              {:name "Third"
               :path "/third"})
          projects (db.project/get-all-projects test-helper/*connection*)]
      
      (t/is (= [(:id p1) (:id p2) (:id p3)]
               (map :id projects))))))

(t/deftest test-update-project-name
  (t/testing "Updating a project's name"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Old Name"
                            :path "/project/path"})
          updated-project (db.project/update-project
                           test-helper/*connection*
                           (:id created-project)
                           {:name "New Name"})]
      
      (t/is (= (:id created-project) (:id updated-project)))
      (t/is (= "New Name" (:name updated-project)))
      (t/is (= "/project/path" (:path updated-project))))))

(t/deftest test-update-project-path
  (t/testing "Updating a project's path"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Project"
                            :path "/old/path"})
          updated-project (db.project/update-project
                           test-helper/*connection*
                           (:id created-project)
                           {:path "/new/path"})]
      
      (t/is (= "Project" (:name updated-project)))
      (t/is (= "/new/path" (:path updated-project))))))

(t/deftest test-update-project-description
  (t/testing "Updating a project's description"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Project"
                            :path "/path"
                            :description "Old description"})
          updated-project (db.project/update-project
                           test-helper/*connection*
                           (:id created-project)
                           {:description "New description"})]
      
      (t/is (= "Project" (:name updated-project)))
      (t/is (= "New description" (:description updated-project))))))

(t/deftest test-update-project-add-description
  (t/testing "Adding a description to a project that didn't have one"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Project"
                            :path "/path"})
          _ (t/is (nil? (:description created-project)))
          updated-project (db.project/update-project
                           test-helper/*connection*
                           (:id created-project)
                           {:description "Now it has a description"})]
      
      (t/is (= "Now it has a description" (:description updated-project))))))

(t/deftest test-update-project-multiple-fields
  (t/testing "Updating multiple fields at once"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Original"
                            :path "/original"})
          updated-project (db.project/update-project
                           test-helper/*connection*
                           (:id created-project)
                           {:name "Updated"
                            :path "/updated"
                            :description "All fields changed"})]
      
      (t/is (= "Updated" (:name updated-project)))
      (t/is (= "/updated" (:path updated-project)))
      (t/is (= "All fields changed" (:description updated-project))))))

(t/deftest test-delete-project
  (t/testing "Deleting a project"
    (let [created-project (db.project/create-project
                           test-helper/*connection*
                           {:name "Doomed Project"
                            :path "/doomed"})
          project-id (:id created-project)
          _ (db.project/delete-project test-helper/*connection* project-id)
          retrieved-project (db.project/get-project-by-id
                             test-helper/*connection*
                             project-id)]
      
      (t/is (nil? retrieved-project)))))

(t/deftest test-delete-non-existent-project
  (t/testing "Deleting a non-existent project returns update-count of 0"
    (let [result (db.project/delete-project test-helper/*connection* 99999)]
      (t/is (= 0 (:next.jdbc/update-count result))))))

;; ============================================================================
;; Database Constraint Tests
;; ============================================================================

(t/deftest test-unique-path-constraint
  (t/testing "Cannot create two projects with the same path"
    (db.project/create-project
     test-helper/*connection*
     {:name "First Project"
      :path "/shared/path"})
    
    (t/is (thrown? org.sqlite.SQLiteException
                   (db.project/create-project
                    test-helper/*connection*
                    {:name "Second Project"
                     :path "/shared/path"})))))

(t/deftest test-update-to-duplicate-path-fails
  (t/testing "Cannot update a project to have the same path as another"
    (let [p1 (db.project/create-project
              test-helper/*connection*
              {:name "Project 1"
               :path "/path1"})
          _p2 (db.project/create-project
               test-helper/*connection*
               {:name "Project 2"
                :path "/path2"})]
      
      (t/is (thrown? org.sqlite.SQLiteException
                     (db.project/update-project
                      test-helper/*connection*
                      (:id p1)
                      {:path "/path2"}))))))

;; ============================================================================
;; Cascade Delete Tests
;; ============================================================================

(t/deftest test-delete-project-cascades-to-plans
  (t/testing "Deleting a project deletes its plans (CASCADE DELETE)"
    ;; Enable foreign key constraints for cascade delete
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    
    (let [project (db.project/create-project
                   test-helper/*connection*
                   {:name "Project with Plans"
                    :path "/project/plans"})
          plan1 (db.plan/create-plan
                 test-helper/*connection*
                 {:name "Plan 1"
                  :context "First plan"
                  :project_id (:id project)})
          plan2 (db.plan/create-plan
                 test-helper/*connection*
                 {:name "Plan 2"
                  :context "Second plan"
                  :project_id (:id project)})
          
          ;; Verify plans exist
          _ (t/is (some? (db.plan/get-plan-by-id
                          test-helper/*connection*
                          (:id plan1))))
          _ (t/is (some? (db.plan/get-plan-by-id
                          test-helper/*connection*
                          (:id plan2))))
          
          ;; Delete the project
          _ (db.project/delete-project test-helper/*connection* (:id project))
          
          ;; Plans should be cascade deleted
          deleted-plan1 (db.plan/get-plan-by-id
                         test-helper/*connection*
                         (:id plan1))
          deleted-plan2 (db.plan/get-plan-by-id
                         test-helper/*connection*
                         (:id plan2))]
      
      (t/is (nil? deleted-plan1))
      (t/is (nil? deleted-plan2)))))

(t/deftest test-delete-project-cascades-to-files
  (t/testing "Deleting a project deletes its files"
    ;; Enable foreign key constraints for cascade delete
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    
    (let [project (db.project/create-project
                   test-helper/*connection*
                   {:name "Project with Files"
                    :path "/project/files"})
          
          ;; Create files directly
          _ (db/execute-one
             test-helper/*connection*
             {:insert-into :file
              :values [{:project_id (:id project)
                        :path "src/core.clj"
                        :summary "Core namespace"}]})
          _ (db/execute-one
             test-helper/*connection*
             {:insert-into :file
              :values [{:project_id (:id project)
                        :path "src/util.clj"
                        :summary "Utilities"}]})
          
          ;; Verify files exist
          files-before (db/execute-many
                        test-helper/*connection*
                        {:select [:*]
                         :from [:file]
                         :where [:= :project_id (:id project)]})
          _ (t/is (= 2 (count files-before)))
          
          ;; Delete the project
          _ (db.project/delete-project test-helper/*connection* (:id project))
          
          ;; Files should be deleted
          files-after (db/execute-many
                       test-helper/*connection*
                       {:select [:*]
                        :from [:file]
                        :where [:= :project_id (:id project)]})]
      
      (t/is (empty? files-after)))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-project-lifecycle
  (t/testing "Complete project lifecycle"
    (let [;; Create
          project (db.project/create-project
                   test-helper/*connection*
                   {:name "New Project"
                    :path "/new/project"})
          _ (t/is (some? (:id project)))
          
          ;; Read
          fetched (db.project/get-project-by-id
                   test-helper/*connection*
                   (:id project))
          _ (t/is (= (:id project) (:id fetched)))
          
          ;; Update
          updated (db.project/update-project
                   test-helper/*connection*
                   (:id project)
                   {:name "Updated Project"
                    :description "Now with description"})
          _ (t/is (= "Updated Project" (:name updated)))
          _ (t/is (= "Now with description" (:description updated)))
          
          ;; Verify update persisted
          refetched (db.project/get-project-by-id
                     test-helper/*connection*
                     (:id project))
          _ (t/is (= "Updated Project" (:name refetched)))
          
          ;; Delete
          _ (db.project/delete-project test-helper/*connection* (:id project))
          deleted (db.project/get-project-by-id
                   test-helper/*connection*
                   (:id project))]
      
      (t/is (nil? deleted)))))

(t/deftest test-project-with-multiple-plans
  (t/testing "A project can have multiple plans"
    (let [project (db.project/create-project
                   test-helper/*connection*
                   {:name "Multi-Plan Project"
                    :path "/multi/plan"})
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

(t/deftest test-multiple-projects-independent
  (t/testing "Multiple projects are independent"
    (let [project1 (db.project/create-project
                    test-helper/*connection*
                    {:name "Project 1"
                     :path "/project1"})
          project2 (db.project/create-project
                    test-helper/*connection*
                    {:name "Project 2"
                     :path "/project2"})
          
          ;; Create plans for each project
          p1-plan (db.plan/create-plan
                   test-helper/*connection*
                   {:name "P1 Plan"
                    :context "Plan for project 1"
                    :project_id (:id project1)})
          p2-plan (db.plan/create-plan
                   test-helper/*connection*
                   {:name "P2 Plan"
                    :context "Plan for project 2"
                    :project_id (:id project2)})
          
          ;; Get plans for each project
          p1-plans (db.plan/get-plans-by-project-id
                    test-helper/*connection*
                    (:id project1))
          p2-plans (db.plan/get-plans-by-project-id
                    test-helper/*connection*
                    (:id project2))]
      
      ;; Each project has only its own plans
      (t/is (= 1 (count p1-plans)))
      (t/is (= 1 (count p2-plans)))
      (t/is (= (:id p1-plan) (:id (first p1-plans))))
      (t/is (= (:id p2-plan) (:id (first p2-plans)))))))
