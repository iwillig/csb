(ns csb.db.conversation
  "Database operations for conversation entities"
  (:require
   [csb.db :as db]
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
       [SQLiteConnection NewConversation :-> Conversation])

(t/ann ^:no-check get-conversation-by-id
       [SQLiteConnection t/Int :-> (t/Option Conversation)])

(t/ann ^:no-check get-conversations-by-project-id
       [SQLiteConnection t/Int :-> (t/Seqable Conversation)])

(t/ann ^:no-check get-all-conversations
       [SQLiteConnection :-> (t/Seqable Conversation)])

(t/ann ^:no-check update-conversation
       [SQLiteConnection t/Int ConversationUpdate :-> Conversation])

(t/ann ^:no-check delete-conversation
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-conversation
  "Creates a new conversation and returns the created conversation with its ID.

  The conversation-data map should contain:
  - :project_id (required) - Associated project ID
  - :name (optional) - Conversation name/title

  Returns the complete Conversation record with generated ID and timestamps."
  [conn conversation-data]
  (let [sql-map {:insert-into :conversation
                 :values [conversation-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-conversation-by-id
  "Retrieves a conversation by its ID.

  Returns the Conversation record if found, nil otherwise."
  [conn conversation-id]
  (let [sql-map {:select [:*]
                 :from [:conversation]
                 :where [:= :id conversation-id]}]
    (db/execute-one conn sql-map)))

(defn get-conversations-by-project-id
  "Retrieves all conversations associated with a project.

  Returns a sequence of Conversation records ordered by creation time."
  [conn project-id]
  (let [sql-map {:select [:*]
                 :from [:conversation]
                 :where [:= :project_id project-id]
                 :order-by [[:created_at :desc]]}]
    (db/execute-many conn sql-map)))

(defn get-all-conversations
  "Retrieves all conversations.

  Returns a sequence of Conversation records ordered by creation time (newest first)."
  [conn]
  (let [sql-map {:select [:*]
                 :from [:conversation]
                 :order-by [[:created_at :desc]]}]
    (db/execute-many conn sql-map)))

(defn update-conversation
  "Updates an existing conversation and returns the updated record.

  The conversation-data map can contain any of:
  - :name - New conversation name
  - :project_id - New project association

  Returns the updated Conversation record with new timestamp."
  [conn conversation-id conversation-data]
  (let [sql-map {:update :conversation
                 :set conversation-data
                 :where [:= :id conversation-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-conversation
  "Deletes a conversation by its ID.

  Note: This will cascade delete all associated messages.

  Returns the number of rows deleted (0 or 1)."
  [conn conversation-id]
  (let [sql-map {:delete-from :conversation
                 :where [:= :id conversation-id]}]
    (db/execute-one conn sql-map)))
