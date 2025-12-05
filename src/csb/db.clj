(ns csb.db
  "Namespace for the major DB databse"
  (:require
   [next.jdbc :as jdbc]
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
