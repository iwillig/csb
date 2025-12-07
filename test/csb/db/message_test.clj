(ns csb.db.message-test
  "Tests for message database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db :as db]
   [csb.db.message :as db.message]
   [csb.db.conversation :as db.conversation]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Test Data Helpers
;; ============================================================================

(defn create-test-conversation
  "Creates a test conversation and returns it."
  ([]
   (let [project (test-helper/create-test-project)]
     (create-test-conversation (:id project))))
  ([project-id]
   (db.conversation/create-conversation
    test-helper/*connection*
    {:project_id project-id
     :name "Test Conversation"})))

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-message-user
  (t/testing "Creating a user message"
    (let [conversation (create-test-conversation)
          message-data {:conversation_id (:id conversation)
                        :role "user"
                        :content "What is the best way to implement this feature?"}
          created-message (db.message/create-message test-helper/*connection* message-data)]

      ;; Verify message was created with ID
      (t/is (integer? (:id created-message)))
      (t/is (pos? (:id created-message)))

      ;; Verify required fields
      (t/is (= (:id conversation) (:conversation_id created-message)))
      (t/is (= "user" (:role created-message)))
      (t/is (= "What is the best way to implement this feature?" (:content created-message)))

      ;; Verify timestamps
      (t/is (string? (:created_at created-message)))
      (t/is (string? (:updated_at created-message)))
      (t/is (= (:created_at created-message) (:updated_at created-message))))))

(t/deftest test-create-message-assistant
  (t/testing "Creating an assistant message"
    (let [conversation (create-test-conversation)
          message-data {:conversation_id (:id conversation)
                        :role "assistant"
                        :content "I recommend using the component pattern."}
          created-message (db.message/create-message test-helper/*connection* message-data)]

      (t/is (= "assistant" (:role created-message)))
      (t/is (= "I recommend using the component pattern." (:content created-message))))))

(t/deftest test-get-message-by-id-exists
  (t/testing "Retrieving an existing message by ID"
    (let [conversation (create-test-conversation)
          created-message (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "user"
                            :content "Find this message"})
          retrieved-message (db.message/get-message-by-id
                             test-helper/*connection*
                             (:id created-message))]

      (t/is (not (nil? retrieved-message)))
      (t/is (= (:id created-message) (:id retrieved-message)))
      (t/is (= "Find this message" (:content retrieved-message))))))

(t/deftest test-get-message-by-id-not-exists
  (t/testing "Retrieving a non-existent message returns nil"
    (let [retrieved-message (db.message/get-message-by-id
                             test-helper/*connection*
                             99999)]
      (t/is (nil? retrieved-message)))))

(t/deftest test-get-messages-by-conversation-id-empty
  (t/testing "Getting messages for a conversation with no messages"
    (let [conversation (create-test-conversation)
          messages (db.message/get-messages-by-conversation-id
                    test-helper/*connection*
                    (:id conversation))]

      (t/is (empty? messages)))))

(t/deftest test-get-messages-by-conversation-id-ordered
  (t/testing "Getting messages ordered by creation time"
    (let [conversation (create-test-conversation)
          msg1 (db.message/create-message
                test-helper/*connection*
                {:conversation_id (:id conversation)
                 :role "user"
                 :content "First message"})
          _ (Thread/sleep 10)  ; Ensure different timestamps
          msg2 (db.message/create-message
                test-helper/*connection*
                {:conversation_id (:id conversation)
                 :role "assistant"
                 :content "Second message"})
          _ (Thread/sleep 10)
          msg3 (db.message/create-message
                test-helper/*connection*
                {:conversation_id (:id conversation)
                 :role "user"
                 :content "Third message"})
          messages (db.message/get-messages-by-conversation-id
                    test-helper/*connection*
                    (:id conversation))]

      (t/is (= 3 (count messages)))
      ;; Should be ordered by created_at ascending
      (t/is (= [(:id msg1) (:id msg2) (:id msg3)]
               (map :id messages)))
      (t/is (= ["First message" "Second message" "Third message"]
               (map :content messages))))))

(t/deftest test-get-messages-by-role-user
  (t/testing "Getting only user messages"
    (let [conversation (create-test-conversation)
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "user"
              :content "User message 1"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "assistant"
              :content "Assistant message"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "user"
              :content "User message 2"})
          user-messages (db.message/get-messages-by-role
                         test-helper/*connection*
                         (:id conversation)
                         "user")]

      (t/is (= 2 (count user-messages)))
      (t/is (every? #(= "user" (:role %)) user-messages))
      (t/is (= #{"User message 1" "User message 2"}
               (set (map :content user-messages)))))))

(t/deftest test-get-messages-by-role-assistant
  (t/testing "Getting only assistant messages"
    (let [conversation (create-test-conversation)
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "user"
              :content "User message"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "assistant"
              :content "Assistant message 1"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conversation)
              :role "assistant"
              :content "Assistant message 2"})
          assistant-messages (db.message/get-messages-by-role
                              test-helper/*connection*
                              (:id conversation)
                              "assistant")]

      (t/is (= 2 (count assistant-messages)))
      (t/is (every? #(= "assistant" (:role %)) assistant-messages))
      (t/is (= #{"Assistant message 1" "Assistant message 2"}
               (set (map :content assistant-messages)))))))

(t/deftest test-update-message-content
  (t/testing "Updating a message's content"
    (let [conversation (create-test-conversation)
          created-message (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "user"
                            :content "Old content"})
          updated-message (db.message/update-message
                           test-helper/*connection*
                           (:id created-message)
                           {:content "New content"})]

      (t/is (= (:id created-message) (:id updated-message)))
      (t/is (= "New content" (:content updated-message)))
      (t/is (= "user" (:role updated-message))))))

(t/deftest test-update-message-role
  (t/testing "Updating a message's role"
    (let [conversation (create-test-conversation)
          created-message (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "user"
                            :content "Message content"})
          updated-message (db.message/update-message
                           test-helper/*connection*
                           (:id created-message)
                           {:role "assistant"})]

      (t/is (= "assistant" (:role updated-message)))
      (t/is (= "Message content" (:content updated-message))))))

(t/deftest test-delete-message
  (t/testing "Deleting a message"
    (let [conversation (create-test-conversation)
          created-message (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "user"
                            :content "Doomed message"})
          message-id (:id created-message)
          _ (db.message/delete-message test-helper/*connection* message-id)
          retrieved-message (db.message/get-message-by-id
                             test-helper/*connection*
                             message-id)]

      (t/is (nil? retrieved-message)))))

;; ============================================================================
;; Database Constraint Tests
;; ============================================================================

(t/deftest test-invalid-conversation-reference
  (t/testing "Creating a message with invalid conversation_id should fail"
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (t/is (thrown? org.sqlite.SQLiteException
                   (db.message/create-message
                    test-helper/*connection*
                    {:conversation_id 99999
                     :role "user"
                     :content "Invalid conversation"})))))

(t/deftest test-invalid-role-value
  (t/testing "Creating a message with invalid role should fail"
    (let [conversation (create-test-conversation)]
      (t/is (thrown? org.sqlite.SQLiteException
                     (db.message/create-message
                      test-helper/*connection*
                      {:conversation_id (:id conversation)
                       :role "invalid-role"
                       :content "This should fail"}))))))

(t/deftest test-cascade-delete-on-conversation
  (t/testing "Deleting a conversation deletes all its messages"
    (db/execute-one test-helper/*connection* {:raw "PRAGMA foreign_keys = ON"})
    (let [conversation (create-test-conversation)
          msg1 (db.message/create-message
                test-helper/*connection*
                {:conversation_id (:id conversation)
                 :role "user"
                 :content "Message 1"})
          msg2 (db.message/create-message
                test-helper/*connection*
                {:conversation_id (:id conversation)
                 :role "assistant"
                 :content "Message 2"})
          ;; Delete conversation
          _ (db.conversation/delete-conversation test-helper/*connection* (:id conversation))
          ;; Check messages are gone
          retrieved-msg1 (db.message/get-message-by-id test-helper/*connection* (:id msg1))
          retrieved-msg2 (db.message/get-message-by-id test-helper/*connection* (:id msg2))]

      (t/is (nil? retrieved-msg1))
      (t/is (nil? retrieved-msg2)))))

;; ============================================================================
;; Full-Text Search Tests
;; ============================================================================

(t/deftest test-message-fts-insert-trigger
  (t/testing "FTS table is populated when message is created"
    (let [conversation (create-test-conversation)
          message (db.message/create-message
                   test-helper/*connection*
                   {:conversation_id (:id conversation)
                    :role "user"
                    :content "This is searchable content with uniquemessageword12345"})
          fts-results (db/execute-many
                       test-helper/*connection*
                       {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'uniquemessageword12345'"})]

      (t/is (= 1 (count fts-results)))
      (t/is (= (:id message) (:rowid (first fts-results)))))))

(t/deftest test-search-messages
  (t/testing "Searching messages using full-text search"
    (let [conv1 (create-test-conversation)
          conv2 (create-test-conversation)
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conv1)
              :role "user"
              :content "How do I implement HTTP server in Clojure?"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conv1)
              :role "assistant"
              :content "Use http-kit for HTTP server implementation"})
          _ (db.message/create-message
             test-helper/*connection*
             {:conversation_id (:id conv2)
              :role "user"
              :content "What database should I use?"})
          ;; Search for "HTTP"
          http-results (db.message/search-messages
                        test-helper/*connection*
                        "HTTP")
          ;; Search for "database"
          db-results (db.message/search-messages
                      test-helper/*connection*
                      "database")]

      ;; Should find 2 messages with "HTTP"
      (t/is (= 2 (count http-results)))
      (t/is (every? #(re-find #"(?i)http" (:content %)) http-results))

      ;; Should find 1 message with "database"
      (t/is (= 1 (count db-results)))
      (t/is (= "What database should I use?" (:content (first db-results)))))))

(t/deftest test-message-fts-update-trigger
  (t/testing "FTS table is updated when message content changes"
    (let [conversation (create-test-conversation)
          message (db.message/create-message
                   test-helper/*connection*
                   {:conversation_id (:id conversation)
                    :role "user"
                    :content "Original searchmessageoriginal7890"})
          ;; Verify original is searchable
          original-search (db/execute-many
                           test-helper/*connection*
                           {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'searchmessageoriginal7890'"})
          _ (t/is (= 1 (count original-search)))

          ;; Update the message
          _ (db.message/update-message
             test-helper/*connection*
             (:id message)
             {:content "Updated searchmessageupdated6789"})

          ;; Old content should not be found
          old-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'searchmessageoriginal7890'"})

          ;; New content should be found
          new-search (db/execute-many
                      test-helper/*connection*
                      {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'searchmessageupdated6789'"})]

      (t/is (empty? old-search))
      (t/is (= 1 (count new-search)))
      (t/is (= (:id message) (:rowid (first new-search)))))))

(t/deftest test-message-fts-delete-trigger
  (t/testing "FTS table entry is removed when message is deleted"
    (let [conversation (create-test-conversation)
          message (db.message/create-message
                   test-helper/*connection*
                   {:conversation_id (:id conversation)
                    :role "user"
                    :content "Will be deleted searchmessagedelete4567"})
          message-id (:id message)

          ;; Verify it's searchable
          before-delete (db/execute-many
                         test-helper/*connection*
                         {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'searchmessagedelete4567'"})
          _ (t/is (= 1 (count before-delete)))

          ;; Delete the message
          _ (db.message/delete-message test-helper/*connection* message-id)

          ;; Should no longer be searchable
          after-delete (db/execute-many
                        test-helper/*connection*
                        {:raw "SELECT rowid FROM message_fts WHERE message_fts MATCH 'searchmessagedelete4567'"})]

      (t/is (empty? after-delete)))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-conversation-message-flow
  (t/testing "Complete conversation with multiple messages"
    (let [conversation (create-test-conversation)
          ;; User asks a question
          user-msg-1 (db.message/create-message
                      test-helper/*connection*
                      {:conversation_id (:id conversation)
                       :role "user"
                       :content "How do I test Clojure code?"})
          ;; Assistant responds
          assistant-msg-1 (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "assistant"
                            :content "Use clojure.test for unit testing"})
          ;; User follows up
          user-msg-2 (db.message/create-message
                      test-helper/*connection*
                      {:conversation_id (:id conversation)
                       :role "user"
                       :content "What about integration tests?"})
          ;; Assistant responds again
          assistant-msg-2 (db.message/create-message
                           test-helper/*connection*
                           {:conversation_id (:id conversation)
                            :role "assistant"
                            :content "Use fixtures and test databases"})
          ;; Get all messages
          all-messages (db.message/get-messages-by-conversation-id
                        test-helper/*connection*
                        (:id conversation))]

      ;; Verify conversation flow
      (t/is (= 4 (count all-messages)))
      (t/is (= [(:id user-msg-1) (:id assistant-msg-1) 
                (:id user-msg-2) (:id assistant-msg-2)]
               (map :id all-messages)))
      (t/is (= ["user" "assistant" "user" "assistant"]
               (map :role all-messages))))))
