(ns csb.db.conversation
  "Database operations for conversation entities.
   
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

(t/defalias NewConversation
  "Data required to create a new conversation.

   Required fields:
   - :project_id - ID of the associated project (Integer)

   Optional fields:
   - :name - Name/title of the conversation (String or nil)"
  (t/HMap :mandatory {:project_id t/Int}
          :optional {:name (t/Option t/Str)}))

(t/defalias ConversationUpdate
  "Data for updating an existing conversation. All fields are optional."
  (t/HMap :optional {:name (t/Option t/Str)
                     :project_id t/Int}))

(t/defalias Conversation
  "Complete conversation record as returned from database.

   Fields:
   - :id - Unique identifier (Integer)
   - :project_id - Associated project ID (Integer)
   - :name - Conversation name (String or nil)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :project_id t/Int
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:name (t/Option t/Str)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-conversation
       [SQLiteConnection NewConversation :-> (types/Result Conversation)])

(t/ann ^:no-check get-conversation-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option Conversation))])

(t/ann ^:no-check get-conversations-by-project-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable Conversation))])

(t/ann ^:no-check get-all-conversations
       [SQLiteConnection :-> (types/Result (t/Seqable Conversation))])

(t/ann ^:no-check update-conversation
       [SQLiteConnection t/Int ConversationUpdate :-> (types/Result Conversation)])

(t/ann ^:no-check delete-conversation
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-conversation
  "Creates a new conversation and returns the created conversation with its ID.

   The conversation-data map should contain:
   - :project_id (required) - Associated project ID
   - :name (optional) - Conversation name/title

   Returns Result<Conversation> - the created record or Failure on database error."
  [conn conversation-data]
  (db/execute-one conn {:insert-into :conversation
                        :values [conversation-data]
                        :returning [:*]}))

(defn get-conversation-by-id
  "Retrieves a conversation by its ID.

   Returns Result<Conversation | nil> - the record, nil if not found, or Failure."
  [conn conversation-id]
  (db/execute-one conn {:select [:*]
                        :from [:conversation]
                        :where [:= :id conversation-id]}))

(defn get-conversations-by-project-id
  "Retrieves all conversations associated with a project.

   Returns Result<Seq<Conversation>> - sequence of records or Failure."
  [conn project-id]
  (db/execute-many conn {:select [:*]
                         :from [:conversation]
                         :where [:= :project_id project-id]
                         :order-by [[:created_at :desc]]}))

(defn get-all-conversations
  "Retrieves all conversations.

   Returns Result<Seq<Conversation>> - sequence of records or Failure."
  [conn]
  (db/execute-many conn {:select [:*]
                         :from [:conversation]
                         :order-by [[:created_at :desc]]}))

(defn update-conversation
  "Updates an existing conversation and returns the updated record.

   The conversation-data map can contain any of:
   - :name - New conversation name
   - :project_id - New project association

   Returns Result<Conversation> - the updated record or Failure."
  [conn conversation-id conversation-data]
  (db/execute-one conn {:update :conversation
                        :set conversation-data
                        :where [:= :id conversation-id]
                        :returning [:*]}))

(defn delete-conversation
  "Deletes a conversation by its ID.

   Note: This will cascade delete all associated messages.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn conversation-id]
  (db/execute-one conn {:delete-from :conversation
                        :where [:= :id conversation-id]}))
