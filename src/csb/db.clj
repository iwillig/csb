(ns csb.db
  "Namespace for the major DB databse"
  (:require
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
       [t/Any (t/Vec t/Any) :-> (t/Seqable t/Any)])

(t/ann ^:no-check next.jdbc/execute-one!
       [t/Any (t/Vec t/Any) :-> t/Any])

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

(defn migration-config
  "Given a SQLite Connection
   Returns a Ragtime migration config"
  [^SQLiteConnection connection]
  {:datastore  (ragtime-jdbc/sql-database connection)
   :migrations (ragtime-jdbc/load-resources "migrations")
   ;; :reporter   ragtime.reporter/silent
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

(t/ann ^:no-check execute-many
       [SQLiteConnection t/Any :-> (t/Seqable t/Any)])

(t/ann ^:no-check execute-one
       [SQLiteConnection t/Any :-> t/Any])

(defn execute-many
  [^SQLiteConnection conn sql-map]
  (jdbc/execute!
   conn
   (honey.sql/format sql-map)
   {:builder-fn honey.rs/as-unqualified-maps}))

(defn execute-one
  [^SQLiteConnection conn sql-map]
  (jdbc/execute-one!
   conn
   (honey.sql/format sql-map)
   {:builder-fn honey.rs/as-unqualified-maps}))
