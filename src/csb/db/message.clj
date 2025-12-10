(ns csb.db.message
  "Database operations for message entities.
   
   All operations return Result<T> for Railway-Oriented error handling.
   Failures propagate automatically through attempt-all pipelines."
  (:require
   [csb.db :as db]
   [csb.db.types :as types]
   [failjure.core :as f]
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
       [SQLiteConnection NewMessage :-> (types/Result Message)])

(t/ann ^:no-check get-message-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option Message))])

(t/ann ^:no-check get-messages-by-conversation-id
       [SQLiteConnection t/Int :-> (types/Result (t/Seqable Message))])

(t/ann ^:no-check get-messages-by-role
       [SQLiteConnection t/Int t/Str :-> (types/Result (t/Seqable Message))])

(t/ann ^:no-check search-messages
       [SQLiteConnection t/Str :-> (types/Result (t/Seqable Message))])

(t/ann ^:no-check update-message
       [SQLiteConnection t/Int MessageUpdate :-> (types/Result Message)])

(t/ann ^:no-check delete-message
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-message
  "Creates a new message and returns the created message with its ID.

   The message-data map should contain:
   - :conversation_id (required) - Associated conversation ID
   - :role (required) - Message role ('user' or 'assistant')
   - :content (required) - Message content

   Returns Result<Message> - the created record or Failure on database error."
  [conn message-data]
  (db/execute-one conn {:insert-into :message
                        :values [message-data]
                        :returning [:*]}))

(defn get-message-by-id
  "Retrieves a message by its ID.

   Returns Result<Message | nil> - the record, nil if not found, or Failure."
  [conn message-id]
  (db/execute-one conn {:select [:*]
                        :from [:message]
                        :where [:= :id message-id]}))

(defn get-messages-by-conversation-id
  "Retrieves all messages in a conversation.

   Returns Result<Seq<Message>> - sequence of records or Failure."
  [conn conversation-id]
  (db/execute-many conn {:select [:*]
                         :from [:message]
                         :where [:= :conversation_id conversation-id]
                         :order-by [[:created_at :asc]]}))

(defn get-messages-by-role
  "Retrieves all messages in a conversation filtered by role.

   role should be either 'user' or 'assistant'.

   Returns Result<Seq<Message>> - sequence of records or Failure."
  [conn conversation-id role]
  (db/execute-many conn {:select [:*]
                         :from [:message]
                         :where [:and
                                 [:= :conversation_id conversation-id]
                                 [:= :role role]]
                         :order-by [[:created_at :asc]]}))

(defn search-messages
  "Searches messages using full-text search on content.

   Returns Result<Seq<Message>> - sequence of matching records or Failure."
  [conn search-query]
  (f/try*
   (jdbc/execute!
    conn
    ["SELECT message.* FROM message 
       JOIN message_fts ON message.id = message_fts.rowid 
       WHERE message_fts MATCH ? 
       ORDER BY rank" search-query]
    {:builder-fn rs/as-unqualified-maps})))

(defn update-message
  "Updates an existing message and returns the updated record.

   The message-data map can contain any of:
   - :content - New message content
   - :role - New role ('user' or 'assistant')

   Returns Result<Message> - the updated record or Failure."
  [conn message-id message-data]
  (db/execute-one conn {:update :message
                        :set message-data
                        :where [:= :id message-id]
                        :returning [:*]}))

(defn delete-message
  "Deletes a message by its ID.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn message-id]
  (db/execute-one conn {:delete-from :message
                        :where [:= :id message-id]}))
