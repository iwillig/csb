(ns csb.db.message
  "Database operations for message entities"
  (:require
   [csb.db :as db]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewMessage
  "Data required to create a new message.

  Required fields:
  - :conversation_id - ID of the associated conversation (Integer)
  - :role - Message role: 'user' or 'assistant' (String)
  - :content - Message content (String)

  Note: role must be either 'user' or 'assistant' (enforced by database CHECK constraint)"
  (t/HMap :mandatory {:conversation_id t/Int
                      :role t/Str
                      :content t/Str}))

(t/defalias MessageUpdate
  "Data for updating an existing message. All fields are optional."
  (t/HMap :optional {:content t/Str
                     :role t/Str}))

(t/defalias Message
  "Complete message record as returned from database.

  Fields:
  - :id - Unique identifier (Integer)
  - :conversation_id - Associated conversation ID (Integer)
  - :role - Message role: 'user' or 'assistant' (String)
  - :content - Message content (String)
  - :created_at - Creation timestamp (String)
  - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :conversation_id t/Int
                      :role t/Str
                      :content t/Str
                      :created_at t/Str
                      :updated_at t/Str}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-message
       [SQLiteConnection NewMessage :-> Message])

(t/ann ^:no-check get-message-by-id
       [SQLiteConnection t/Int :-> (t/Option Message)])

(t/ann ^:no-check get-messages-by-conversation-id
       [SQLiteConnection t/Int :-> (t/Seqable Message)])

(t/ann ^:no-check get-messages-by-role
       [SQLiteConnection t/Int t/Str :-> (t/Seqable Message)])

(t/ann ^:no-check search-messages
       [SQLiteConnection t/Str :-> (t/Seqable Message)])

(t/ann ^:no-check update-message
       [SQLiteConnection t/Int MessageUpdate :-> Message])

(t/ann ^:no-check delete-message
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-message
  "Creates a new message and returns the created message with its ID.

  The message-data map should contain:
  - :conversation_id (required) - Associated conversation ID
  - :role (required) - Message role ('user' or 'assistant')
  - :content (required) - Message content

  Returns the complete Message record with generated ID and timestamps."
  [conn message-data]
  (let [sql-map {:insert-into :message
                 :values [message-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-message-by-id
  "Retrieves a message by its ID.

  Returns the Message record if found, nil otherwise."
  [conn message-id]
  (let [sql-map {:select [:*]
                 :from [:message]
                 :where [:= :id message-id]}]
    (db/execute-one conn sql-map)))

(defn get-messages-by-conversation-id
  "Retrieves all messages in a conversation.

  Returns a sequence of Message records ordered by creation time (oldest first)."
  [conn conversation-id]
  (let [sql-map {:select [:*]
                 :from [:message]
                 :where [:= :conversation_id conversation-id]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn get-messages-by-role
  "Retrieves all messages in a conversation filtered by role.

  role should be either 'user' or 'assistant'.

  Returns a sequence of Message records ordered by creation time."
  [conn conversation-id role]
  (let [sql-map {:select [:*]
                 :from [:message]
                 :where [:and
                         [:= :conversation_id conversation-id]
                         [:= :role role]]
                 :order-by [[:created_at :asc]]}]
    (db/execute-many conn sql-map)))

(defn search-messages
  "Searches messages using full-text search on content.

  Returns a sequence of Message records matching the search query."
  [conn search-query]
  ;; Use next.jdbc directly for parameterized raw SQL
  (jdbc/execute!
   conn
   ["SELECT message.* FROM message 
     JOIN message_fts ON message.id = message_fts.rowid 
     WHERE message_fts MATCH ? 
     ORDER BY rank" search-query]
   {:builder-fn rs/as-unqualified-maps}))

(defn update-message
  "Updates an existing message and returns the updated record.

  The message-data map can contain any of:
  - :content - New message content
  - :role - New role ('user' or 'assistant')

  Returns the updated Message record with new timestamp."
  [conn message-id message-data]
  (let [sql-map {:update :message
                 :set message-data
                 :where [:= :id message-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-message
  "Deletes a message by its ID.

  Returns the number of rows deleted (0 or 1)."
  [conn message-id]
  (let [sql-map {:delete-from :message
                 :where [:= :id message-id]}]
    (db/execute-one conn sql-map)))
