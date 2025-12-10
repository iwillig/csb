(ns csb.db
  "Database layer with Railway-Oriented Programming for error handling.
   
   All database operations return Result<T> - either a success value or a Failure.
   This eliminates exception-based error handling and makes failure paths explicit."
  (:require
   [csb.db.types :as types]
   [failjure.core :as f]
   [honey.sql :as honey.sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as honey.rs]
   [ragtime.next-jdbc :as ragtime-jdbc]
   [ragtime.repl :as ragtime-repl]
   [ragtime.reporter]
   [ragtime.strategy]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; Type annotations for external library functions
(t/ann ^:no-check ragtime.next-jdbc/sql-database
       (t/IFn [t/Any :-> t/Any]
              [t/Any t/Any :-> t/Any]))

(t/ann ^:no-check ragtime.next-jdbc/load-resources
       [t/Str :-> t/Any])

(t/ann ^:no-check ragtime.strategy/apply-new
       t/Any)

(t/ann ^:no-check next.jdbc/get-connection
       (t/IFn [t/Any :-> t/Any]
              [t/Any t/Any :-> t/Any]
              [t/Any t/Any t/Any :-> t/Any]
              [t/Any t/Any t/Any t/Any :-> t/Any]))

(t/ann ^:no-check ragtime.repl/migrate
       [t/Any :-> t/Any])

(t/ann ^:no-check ragtime.repl/rollback
       (t/IFn [t/Any :-> t/Any]
              [t/Any t/Any :-> t/Any]))

(t/ann ^:no-check ragtime.repl/migration-index
       t/Any)

;; Annotations for honey.sql and next.jdbc
(t/ann ^:no-check honey.sql/format
       [(t/Map t/Any t/Any) :-> (t/Vec t/Any)])

(t/ann ^:no-check next.jdbc/execute!
       (t/IFn [t/Any (t/Vec t/Any) :-> (t/Seqable t/Any)]
              [t/Any (t/Vec t/Any) t/Any :-> (t/Seqable t/Any)]))

(t/ann ^:no-check next.jdbc/execute-one!
       (t/IFn [t/Any (t/Vec t/Any) :-> t/Any]
              [t/Any (t/Vec t/Any) t/Any :-> t/Any]))

;; Annotation for result-set builder function
(t/ann ^:no-check next.jdbc.result-set/as-unqualified-maps
       t/Any)

;; Type annotations for our functions
(t/ann migration-config
       [SQLiteConnection :-> (t/Map t/Keyword t/Any)])

(t/ann memory-sqlite-database
       [:-> t/Any])

(t/ann file-sqlite-database
       [(t/Map t/Keyword t/Any) :-> t/Any])

(t/ann migrate
       [t/Any :-> t/Any])

(t/ann rollback
       [t/Any :-> t/Any])

(t/ann ^:no-check rollback-all
       [t/Any :-> nil])

(t/ann ragtime.reporter/silent
       [t/Any t/Any t/Any :-> nil])

(defn migration-config
  "Given a SQLite Connection
   Returns a Ragtime migration config"
  [^SQLiteConnection connection]
  {:datastore  (ragtime-jdbc/sql-database connection)
   :migrations (ragtime-jdbc/load-resources "migrations")
   :reporter   ragtime.reporter/silent
   :strategy   ragtime.strategy/apply-new})

(defn memory-sqlite-database
  "Returns an in memory database"
  []
  (next.jdbc/get-connection
   {:connection-uri "jdbc:sqlite::memory:"}))

(defn file-sqlite-database
  [{:keys [db_path]}]
  (next.jdbc/get-connection
   {:connection-uri (str "jdbc:sqlite:" db_path)}))

(defn migrate
  [config]
  (ragtime-repl/migrate config))

(defn rollback
  [config]
  (ragtime-repl/rollback config))

(defn rollback-all
  [config]
  (let [migration-count (count @ragtime-repl/migration-index)]
    (dotimes [_ migration-count]
      (ragtime-repl/rollback config))))

;; ============================================================================
;; Railway-Style Database Operations
;; ============================================================================

(t/ann ^:no-check execute-many
       (t/All [a] [SQLiteConnection (t/Map t/Keyword t/Any) :-> (types/Result (t/Seqable a))]))

(t/ann ^:no-check execute-one
       (t/All [a] [SQLiteConnection (t/Map t/Keyword t/Any) :-> (types/Result (t/Option a))]))

(defn execute-many
  "Execute SQL query returning multiple rows.
   
   Returns Result<Seq<a>> - sequence of rows or Failure on database error.
   
   Uses f/try* to catch any exceptions and convert them to Failure values,
   enabling Railway-Oriented error handling throughout the application.
   
   Example:
     (execute-many conn {:select [:*] :from [:project]})
     => [{:id 1 :name \"Project 1\"} {:id 2 :name \"Project 2\"}]
     
     (execute-many conn {:select [:*] :from [:nonexistent]})
     => #Failure{:message \"no such table: nonexistent\"}"
  [^SQLiteConnection conn sql-map]
  (f/try*
   (jdbc/execute!
    conn
    (honey.sql/format sql-map)
    {:builder-fn honey.rs/as-unqualified-maps})))

(defn execute-one
  "Execute SQL query returning single row.
   
   Returns Result<a | nil> - single row, nil if not found, or Failure on error.
   
   Uses f/try* to catch any exceptions and convert them to Failure values,
   enabling Railway-Oriented error handling throughout the application.
   
   Example:
     (execute-one conn {:select [:*] :from [:project] :where [:= :id 1]})
     => {:id 1 :name \"My Project\"}
     
     (execute-one conn {:select [:*] :from [:project] :where [:= :id 999]})
     => nil
     
     (execute-one conn {:select [:*] :from [:nonexistent]})
     => #Failure{:message \"no such table: nonexistent\"}"
  [^SQLiteConnection conn sql-map]
  (f/try*
   (jdbc/execute-one!
    conn
    (honey.sql/format sql-map)
    {:builder-fn honey.rs/as-unqualified-maps})))
