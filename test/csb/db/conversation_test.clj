(ns csb.db.conversation-test
  "Tests for conversation database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.conversation :as db.conversation]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-conversation-with-required-fields
  (t/testing "Creating a conversation with only required fields"
    (t/testing "Given a project and conversation data with required fields"
      (let [project (test-helper/create-test-project)
            conversation-data {:project_id (:id project)}
            created-conversation (db.conversation/create-conversation
                                  test-helper/*connection*
                                  conversation-data)]
        (t/testing "When the conversation is created"
          (t/testing "Then it should be created with an ID"
            (t/is (integer? (:id created-conversation)))
            (t/is (pos? (:id created-conversation))))
          (t/testing "Then the required fields should be set correctly"
            (t/is (= (:id project) (:project_id created-conversation))))
          (t/testing "Then optional fields should be nil"
            (t/is (nil? (:name created-conversation))))
          (t/testing "Then timestamps should be set correctly"
            (t/is (string? (:created_at created-conversation)))
            (t/is (string? (:updated_at created-conversation)))
            (t/is (= (:created_at created-conversation) (:updated_at created-conversation)))))))))

(t/deftest test-create-conversation-with-name
  (t/testing "Creating a conversation with a name"
    (let [project (test-helper/create-test-project)
          conversation-data {:project_id (:id project)
                             :name "Implementation Discussion"}
          created-conversation (db.conversation/create-conversation
                                test-helper/*connection*
                                conversation-data)]

      (t/is (= "Implementation Discussion" (:name created-conversation)))
      (t/is (= (:id project) (:project_id created-conversation))))))

(t/deftest test-get-conversation-by-id-exists
  (t/testing "Retrieving an existing conversation by ID"
    (t/testing "Given an existing conversation"
      (let [project (test-helper/create-test-project)
            created-conversation (db.conversation/create-conversation
                                  test-helper/*connection*
                                  {:project_id (:id project)
                                   :name "Find Me"})
            retrieved-conversation (db.conversation/get-conversation-by-id
                                    test-helper/*connection*
                                    (:id created-conversation))]
        (t/testing "When retrieving the conversation by ID"
          (t/testing "Then it should return the conversation"
            (t/is (not (nil? retrieved-conversation))))
          (t/testing "Then the retrieved conversation should have the correct ID"
            (t/is (= (:id created-conversation) (:id retrieved-conversation))))
          (t/testing "Then the retrieved conversation should have the correct name"
            (t/is (= "Find Me" (:name retrieved-conversation)))))))))

(t/deftest test-get-conversation-by-id-not-exists
  (t/testing "Retrieving a non-existent conversation returns nil"
    (let [retrieved-conversation (db.conversation/get-conversation-by-id
                                   test-helper/*connection*
                                   99999)]
      (t/is (nil? retrieved-conversation)))))

(t/deftest test-get-conversations-by-project-id-empty
  (t/testing "Getting conversations for a project with no conversations"
    (let [project (test-helper/create-test-project)
          conversations (db.conversation/get-conversations-by-project-id
                         test-helper/*connection*
                         (:id project))]

      (t/is (empty? conversations)))))

(t/deftest test-get-conversations-by-project-id-multiple
  (t/testing "Getting conversations for a project with multiple conversations"
    (let [project (test-helper/create-test-project)
          _ (db.conversation/create-conversation
             test-helper/*connection*
             {:project_id (:id project)
              :name "Conversation 1"})
          _ (db.conversation/create-conversation
             test-helper/*connection*
             {:project_id (:id project)
              :name "Conversation 2"})
          _ (db.conversation/create-conversation
             test-helper/*connection*
             {:project_id (:id project)
              :name "Conversation 3"})
          conversations (db.conversation/get-conversations-by-project-id
                         test-helper/*connection*
                         (:id project))]

      (t/is (= 3 (count conversations)))
      (t/is (= #{"Conversation 1" "Conversation 2" "Conversation 3"}
               (set (map :name conversations)))))))

(t/deftest test-get-all-conversations
  (t/testing "Getting all conversations"
    (let [project1 (test-helper/create-test-project "Project 1" "/path/1")
          project2 (test-helper/create-test-project "Project 2" "/path/2")
          _ (db.conversation/create-conversation
             test-helper/*connection*
             {:project_id (:id project1)
              :name "Conv 1"})
          _ (db.conversation/create-conversation
             test-helper/*connection*
             {:project_id (:id project2)
              :name "Conv 2"})
          conversations (db.conversation/get-all-conversations test-helper/*connection*)]

      (t/is (= 2 (count conversations)))
      (t/is (= #{"Conv 1" "Conv 2"}
               (set (map :name conversations)))))))

(t/deftest test-update-conversation-name
  (t/testing "Updating a conversation's name"
    (let [project (test-helper/create-test-project)
          created-conversation (db.conversation/create-conversation
                                test-helper/*connection*
                                {:project_id (:id project)
                                 :name "Old Name"})
          updated-conversation (db.conversation/update-conversation
                                test-helper/*connection*
                                (:id created-conversation)
                                {:name "New Name"})]

      (t/is (= (:id created-conversation) (:id updated-conversation)))
      (t/is (= "New Name" (:name updated-conversation))))))

(t/deftest test-update-conversation-project
  (t/testing "Updating a conversation's project association"
    (let [project1 (test-helper/create-test-project "Project 1" "/path/1")
          project2 (test-helper/create-test-project "Project 2" "/path/2")
          created-conversation (db.conversation/create-conversation
                                test-helper/*connection*
                                {:project_id (:id project1)
                                 :name "Moving Conversation"})
          _ (t/is (= (:id project1) (:project_id created-conversation)))
          updated-conversation (db.conversation/update-conversation
                                test-helper/*connection*
                                (:id created-conversation)
                                {:project_id (:id project2)})]

      (t/is (= (:id project2) (:project_id updated-conversation))))))

(t/deftest test-delete-conversation
  (t/testing "Deleting a conversation"
    (let [project (test-helper/create-test-project)
          created-conversation (db.conversation/create-conversation
                                test-helper/*connection*
                                {:project_id (:id project)
                                 :name "Doomed Conversation"})
          conversation-id (:id created-conversation)
          _ (db.conversation/delete-conversation test-helper/*connection* conversation-id)
          retrieved-conversation (db.conversation/get-conversation-by-id
                                  test-helper/*connection*
                                  conversation-id)]

      (t/is (nil? retrieved-conversation)))))

;; ============================================================================
;; Database Constraint Tests
;; ============================================================================

(t/deftest test-invalid-project-reference
  (t/testing "Creating a conversation with invalid project_id should fail"
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (t/is (thrown? org.sqlite.SQLiteException
                   (db.conversation/create-conversation
                    test-helper/*connection*
                    {:project_id 99999})))))
