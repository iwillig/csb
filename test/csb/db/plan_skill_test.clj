(ns csb.db.plan-skill-test
  "Tests for plan skill database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.plan-skill :as db.plan-skill]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-plan-skill-with-required-fields
  (t/testing "Creating a plan skill with only required fields"
    (t/testing "Given skill data with required fields"
      (let [skill-data {:name "HTTP Servers"
                        :content "Guide to HTTP server libraries in Clojure"}
            created-skill (db.plan-skill/create-plan-skill test-helper/*connection* skill-data)]
        (t/testing "When the plan skill is created"
          (t/testing "Then it should be created with an ID"
            (t/is (integer? (:id created-skill)))
            (t/is (pos? (:id created-skill))))
          (t/testing "Then the required fields should be set correctly"
            (t/is (= "HTTP Servers" (:name created-skill)))
            (t/is (= "Guide to HTTP server libraries in Clojure" (:content created-skill))))
          (t/testing "Then optional fields should be nil"
            (t/is (nil? (:description created-skill))))
          (t/testing "Then timestamps should be set correctly"
            (t/is (string? (:created_at created-skill)))
            (t/is (string? (:updated_at created-skill)))
            (t/is (= (:created_at created-skill) (:updated_at created-skill)))))))))

(t/deftest test-create-plan-skill-with-description
  (t/testing "Creating a plan skill with description"
    (let [skill-data {:name "Malli"
                      :content "Detailed guide on Malli data validation library"
                      :description "Data validation using Malli schemas"}
          created-skill (db.plan-skill/create-plan-skill test-helper/*connection* skill-data)]

      (t/is (= "Malli" (:name created-skill)))
      (t/is (= "Data validation using Malli schemas" (:description created-skill)))
      (t/is (= "Detailed guide on Malli data validation library" (:content created-skill))))))

(t/deftest test-get-plan-skill-by-id-exists
  (t/testing "Retrieving an existing plan skill by ID"
    (t/testing "Given an existing plan skill"
      (let [created-skill (db.plan-skill/create-plan-skill
                           test-helper/*connection*
                           {:name "Testing"
                            :content "How to test Clojure applications"})
            retrieved-skill (db.plan-skill/get-plan-skill-by-id
                             test-helper/*connection*
                             (:id created-skill))]
        (t/testing "When retrieving the plan skill by ID"
          (t/testing "Then it should return the plan skill"
            (t/is (not (nil? retrieved-skill))))
          (t/testing "Then the retrieved plan skill should have the correct ID"
            (t/is (= (:id created-skill) (:id retrieved-skill))))
          (t/testing "Then the retrieved plan skill should have the correct name"
            (t/is (= "Testing" (:name retrieved-skill))))
          (t/testing "Then the retrieved plan skill should have the correct content"
            (t/is (= "How to test Clojure applications" (:content retrieved-skill)))))))))

(t/deftest test-get-plan-skill-by-id-not-exists
  (t/testing "Retrieving a non-existent plan skill returns nil"
    (let [retrieved-skill (db.plan-skill/get-plan-skill-by-id
                           test-helper/*connection*
                           99999)]
      (t/is (nil? retrieved-skill)))))

(t/deftest test-get-plan-skill-by-name-exists
  (t/testing "Retrieving a plan skill by name"
    (let [created-skill (db.plan-skill/create-plan-skill
                         test-helper/*connection*
                         {:name "REPL Driven Development"
                          :content "Guide to REPL-driven workflow"})
          retrieved-skill (db.plan-skill/get-plan-skill-by-name
                           test-helper/*connection*
                           "REPL Driven Development")]

      (t/is (not (nil? retrieved-skill)))
      (t/is (= (:id created-skill) (:id retrieved-skill)))
      (t/is (= "REPL Driven Development" (:name retrieved-skill))))))

(t/deftest test-get-plan-skill-by-name-not-exists
  (t/testing "Retrieving a non-existent plan skill by name returns nil"
    (let [retrieved-skill (db.plan-skill/get-plan-skill-by-name
                           test-helper/*connection*
                           "NonExistent")]
      (t/is (nil? retrieved-skill)))))

(t/deftest test-get-all-plan-skills-empty
  (t/testing "Getting all plan skills when there are none"
    (let [skills (db.plan-skill/get-all-plan-skills test-helper/*connection*)]
      (t/is (empty? skills)))))

(t/deftest test-get-all-plan-skills-multiple
  (t/testing "Getting all plan skills"
    (let [_ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "Skill B"
              :content "Content B"})
          _ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "Skill A"
              :content "Content A"})
          _ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "Skill C"
              :content "Content C"})
          skills (db.plan-skill/get-all-plan-skills test-helper/*connection*)]

      (t/is (= 3 (count skills)))
      ;; Should be ordered by name
      (t/is (= ["Skill A" "Skill B" "Skill C"]
               (map :name skills))))))

(t/deftest test-update-plan-skill-name
  (t/testing "Updating a plan skill's name"
    (let [created-skill (db.plan-skill/create-plan-skill
                         test-helper/*connection*
                         {:name "Old Name"
                          :content "Original content"})
          updated-skill (db.plan-skill/update-plan-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:name "New Name"})]

      (t/is (= (:id created-skill) (:id updated-skill)))
      (t/is (= "New Name" (:name updated-skill)))
      (t/is (= "Original content" (:content updated-skill))))))

(t/deftest test-update-plan-skill-content
  (t/testing "Updating a plan skill's content"
    (let [created-skill (db.plan-skill/create-plan-skill
                         test-helper/*connection*
                         {:name "Skill Name"
                          :content "Old content"})
          updated-skill (db.plan-skill/update-plan-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:content "New content"})]

      (t/is (= "Skill Name" (:name updated-skill)))
      (t/is (= "New content" (:content updated-skill))))))

(t/deftest test-update-plan-skill-description
  (t/testing "Updating a plan skill's description"
    (let [created-skill (db.plan-skill/create-plan-skill
                         test-helper/*connection*
                         {:name "Skill"
                          :content "Content"})
          updated-skill (db.plan-skill/update-plan-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:description "New description"})]

      (t/is (= "New description" (:description updated-skill))))))

(t/deftest test-delete-plan-skill
  (t/testing "Deleting a plan skill"
    (let [created-skill (db.plan-skill/create-plan-skill
                         test-helper/*connection*
                         {:name "Doomed Skill"
                          :content "Will be deleted"})
          skill-id (:id created-skill)
          _ (db.plan-skill/delete-plan-skill test-helper/*connection* skill-id)
          retrieved-skill (db.plan-skill/get-plan-skill-by-id
                           test-helper/*connection*
                           skill-id)]

      (t/is (nil? retrieved-skill)))))

;; ============================================================================
;; Full-Text Search Tests
;; ============================================================================

(t/deftest test-plan-skill-fts-insert-trigger
  (t/testing "FTS table is populated when plan skill is created"
    (let [skill (db.plan-skill/create-plan-skill
                 test-helper/*connection*
                 {:name "FTS Test"
                  :content "This is searchable content with uniqueskillword12345"})
          fts-results (db/execute-many
                       test-helper/*connection*
                       {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'uniqueskillword12345'"})]

      (t/is (= 1 (count fts-results)))
      (t/is (= (:id skill) (:rowid (first fts-results)))))))

(t/deftest test-search-plan-skills
  (t/testing "Searching plan skills using full-text search"
    (let [_ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "HTTP Servers"
              :content "Guide to http-kit and other HTTP server libraries"})
          _ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "Database"
              :content "Guide to next.jdbc and database access"})
          _ (db.plan-skill/create-plan-skill
             test-helper/*connection*
             {:name "HTTP Clients"
              :content "Guide to clj-http and HTTP client libraries"})
          ;; Search for "HTTP"
          http-results (db.plan-skill/search-plan-skills
                        test-helper/*connection*
                        "HTTP")
          ;; Search for "database"
          db-results (db.plan-skill/search-plan-skills
                      test-helper/*connection*
                      "database")]

      ;; Should find 2 skills with "HTTP"
      (t/is (= 2 (count http-results)))
      (t/is (= #{"HTTP Servers" "HTTP Clients"}
               (set (map :name http-results))))

      ;; Should find 1 skill with "database"
      (t/is (= 1 (count db-results)))
      (t/is (= "Database" (:name (first db-results)))))))

(t/deftest test-plan-skill-fts-update-trigger
  (t/testing "FTS table is updated when plan skill content changes"
    (let [skill (db.plan-skill/create-plan-skill
                 test-helper/*connection*
                 {:name "FTS Update Test"
                  :content "Original searchskilloriginal7890"})
          ;; Verify original is searchable
          original-search (db/execute-many
                           test-helper/*connection*
                           {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'searchskilloriginal7890'"})
          _ (t/is (= 1 (count original-search)))

          ;; Update the skill
          _ (db.plan-skill/update-plan-skill
             test-helper/*connection*
             (:id skill)
             {:content "Updated searchskillupdated6789"})

          ;; Old content should not be found
          old-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'searchskilloriginal7890'"})

          ;; New content should be found
          new-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'searchskillupdated6789'"})]

      (t/is (empty? old-search))
      (t/is (= 1 (count new-search)))
      (t/is (= (:id skill) (:rowid (first new-search)))))))

(t/deftest test-plan-skill-fts-delete-trigger
  (t/testing "FTS table entry is removed when plan skill is deleted"
    (let [skill (db.plan-skill/create-plan-skill
                 test-helper/*connection*
                 {:name "FTS Delete Test"
                  :content "Will be deleted searchskilldelete4567"})
          skill-id (:id skill)

          ;; Verify it's searchable
          before-delete (db/execute-many
                         test-helper/*connection*
                         {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'searchskilldelete4567'"})
          _ (t/is (= 1 (count before-delete)))

          ;; Delete the skill
          _ (db.plan-skill/delete-plan-skill test-helper/*connection* skill-id)

          ;; Should no longer be searchable
          after-delete (db/execute-many
                        test-helper/*connection*
                        {:raw "SELECT rowid FROM plan_skill_fts WHERE plan_skill_fts MATCH 'searchskilldelete4567'"})]

      (t/is (empty? after-delete)))))
