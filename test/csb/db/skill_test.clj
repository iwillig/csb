(ns csb.db.skill-test
  "Tests for skill database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db.skill :as db.skill]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-skill-with-required-fields
  (t/testing "Creating a skill with only required fields"
    (t/testing "Given skill data with required fields"
      (let [skill-data {:name "HTTP Servers"
                        :content "Guide to HTTP server libraries in Clojure"}
            created-skill (db.skill/create-skill test-helper/*connection* skill-data)]
        (t/testing "When the skill is created"
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

(t/deftest test-create-skill-with-description
  (t/testing "Creating a skill with a description"
    (let [skill-data {:name "Web Frameworks"
                      :content "Guide to web frameworks in Clojure"
                      :description "Popular Clojure web frameworks"}
          created-skill (db.skill/create-skill test-helper/*connection* skill-data)]
      
      (t/is (= "Web Frameworks" (:name created-skill)))
      (t/is (= "Guide to web frameworks in Clojure" (:content created-skill)))
      (t/is (= "Popular Clojure web frameworks" (:description created-skill))))))

(t/deftest test-create-skill-with-all-fields
  (t/testing "Creating a skill with all fields set"
    (let [skill-data {:name "Database Libraries"
                      :content "Guide to database libraries in Clojure"
                      :description "Database connectivity libraries"}
          created-skill (db.skill/create-skill test-helper/*connection* skill-data)]
      
      (t/is (= "Database Libraries" (:name created-skill)))
      (t/is (= "Guide to database libraries in Clojure" (:content created-skill)))
      (t/is (= "Database connectivity libraries" (:description created-skill))))))

(t/deftest test-get-skill-by-id-exists
  (t/testing "Retrieving an existing skill by ID"
    (t/testing "Given an existing skill"
      (let [created-skill (db.skill/create-skill
                           test-helper/*connection*
                           {:name "Findable Skill"
                            :content "Skill that should be found"})
            retrieved-skill (db.skill/get-skill-by-id
                             test-helper/*connection*
                             (:id created-skill))]
        (t/testing "When retrieving the skill by ID"
          (t/testing "Then it should return the skill"
            (t/is (not (nil? retrieved-skill))))
          (t/testing "Then the retrieved skill should have the correct ID"
            (t/is (= (:id created-skill) (:id retrieved-skill))))
          (t/testing "Then the retrieved skill should have the correct name"
            (t/is (= "Findable Skill" (:name retrieved-skill))))
          (t/testing "Then the retrieved skill should have the correct content"
            (t/is (= "Skill that should be found" (:content retrieved-skill)))))))))

(t/deftest test-get-skill-by-id-not-exists
  (t/testing "Retrieving a non-existent skill returns nil"
    (let [retrieved-skill (db.skill/get-skill-by-id
                           test-helper/*connection*
                           99999)]
      (t/is (nil? retrieved-skill)))))

(t/deftest test-get-skill-by-name-exists
  (t/testing "Retrieving a skill by name"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Named Skill"
                          :content "Skill with a specific name"})
          retrieved-skill (db.skill/get-skill-by-name
                           test-helper/*connection*
                           "Named Skill")]
      (t/is (not (nil? retrieved-skill)))
      (t/is (= (:id created-skill) (:id retrieved-skill)))
      (t/is (= "Named Skill" (:name retrieved-skill))))))

(t/deftest test-get-skill-by-name-not-exists
  (t/testing "Retrieving a non-existent skill by name returns nil"
    (let [retrieved-skill (db.skill/get-skill-by-name
                           test-helper/*connection*
                           "Non-existent Skill")]
      (t/is (nil? retrieved-skill)))))

(t/deftest test-get-all-skills-empty
  (t/testing "Getting all skills when there are none"
    (let [skills (db.skill/get-all-skills test-helper/*connection*)]
      (t/is (empty? skills)))))

(t/deftest test-get-all-skills-multiple
  (t/testing "Getting all skills"
    (let [_ (db.skill/create-skill
             test-helper/*connection*
             {:name "Skill 1"
              :content "Content 1"})
          _ (db.skill/create-skill
             test-helper/*connection*
             {:name "Skill 2"
              :content "Content 2"})
          _ (db.skill/create-skill
             test-helper/*connection*
             {:name "Skill 3"
              :content "Content 3"})
          skills (db.skill/get-all-skills test-helper/*connection*)]
      
      (t/is (= 3 (count skills)))
      (t/is (= #{"Skill 1" "Skill 2" "Skill 3"}
               (set (map :name skills)))))))

(t/deftest test-update-skill-name
  (t/testing "Updating a skill's name"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Old Name"
                          :content "Skill content"})
          updated-skill (db.skill/update-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:name "New Name"})]
      
      (t/is (= (:id created-skill) (:id updated-skill)))
      (t/is (= "New Name" (:name updated-skill)))
      (t/is (= "Skill content" (:content updated-skill))))))

(t/deftest test-update-skill-content
  (t/testing "Updating a skill's content"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Skill"
                          :content "Original content"})
          updated-skill (db.skill/update-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:content "Updated content"})]
      
      (t/is (= "Updated content" (:content updated-skill))))))
          
(t/deftest test-update-skill-description
  (t/testing "Updating a skill's description"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Skill"
                          :content "Skill content"
                          :description "Original description"})
          updated-skill (db.skill/update-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:description "Updated description"})]
      
      (t/is (= "Updated description" (:description updated-skill))))))
          
(t/deftest test-update-skill-multiple-fields
  (t/testing "Updating multiple fields at once"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Original"
                          :content "Original content"})
          updated-skill (db.skill/update-skill
                         test-helper/*connection*
                         (:id created-skill)
                         {:name "Updated"
                          :content "Updated content"
                          :description "Updated description"})]
      
      (t/is (= "Updated" (:name updated-skill)))
      (t/is (= "Updated content" (:content updated-skill)))
      (t/is (= "Updated description" (:description updated-skill))))))

(t/deftest test-delete-skill
  (t/testing "Deleting a skill"
    (let [created-skill (db.skill/create-skill
                         test-helper/*connection*
                         {:name "Doomed Skill"
                          :content "Skill to be deleted"})
          skill-id (:id created-skill)
          _ (db.skill/delete-skill test-helper/*connection* skill-id)
          retrieved-skill (db.skill/get-skill-by-id
                           test-helper/*connection*
                           skill-id)]
      
      (t/is (nil? retrieved-skill)))))

(t/deftest test-delete-non-existent-skill
  (t/testing "Deleting a non-existent skill returns update-count of 0"
    (let [result (db.skill/delete-skill test-helper/*connection* 99999)]
      (t/is (= 0 (:next.jdbc/update-count result))))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-skill-lifecycle
  (t/testing "Complete skill lifecycle"
    (let [;; Create
          skill (db.skill/create-skill
                 test-helper/*connection*
                 {:name "New Skill"
                  :content "Skill content"})
          _ (t/is (some? (:id skill)))
          
          ;; Read
          fetched (db.skill/get-skill-by-id
                   test-helper/*connection*
                   (:id skill))
          _ (t/is (= (:id skill) (:id fetched)))
          
          ;; Update
          updated (db.skill/update-skill
                   test-helper/*connection*
                   (:id skill)
                   {:name "Updated Skill"
                    :description "Now with description"})
          _ (t/is (= "Updated Skill" (:name updated)))
          _ (t/is (= "Now with description" (:description updated)))
          
          ;; Verify update persisted
          refetched (db.skill/get-skill-by-id
                     test-helper/*connection*
                     (:id skill))
          _ (t/is (= "Updated Skill" (:name refetched)))
          
          ;; Delete
          _ (db.skill/delete-skill test-helper/*connection* (:id skill))
          deleted (db.skill/get-skill-by-id
                   test-helper/*connection*
                   (:id skill))]
      
      (t/is (nil? deleted)))))