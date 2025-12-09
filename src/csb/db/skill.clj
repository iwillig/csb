(ns csb.db.skill
  "Database operations for skill entities"
  (:require
   [csb.db :as db]
   [typed.clojure :as t])
  (:import
   (org.sqlite
    SQLiteConnection)))

;; ============================================================================
;; Type Definitions
;; ============================================================================

(t/defalias NewSkill
  "Data required to create a new skill.

   Required fields:
   - :name - The name of the skill (String)
   - :content - The full content/documentation of the skill (String)

   Optional fields:
   - :description - Brief description of the skill (String or nil)"
  (t/HMap :mandatory {:name t/Str
                      :content t/Str}
          :optional {:description (t/Option t/Str)}))

(t/defalias SkillUpdate
  "Data for updating an existing skill. All fields are optional."
  (t/HMap :optional {:name t/Str
                     :content t/Str
                     :description (t/Option t/Str)}))

(t/defalias Skill
  "Complete skill record as returned from database.

   Fields:
   - :id - Unique identifier (Integer)
   - :name - Skill name (String)
   - :content - Full skill content/documentation (String)
   - :description - Brief description (String or nil)
   - :created_at - Creation timestamp (String)
   - :updated_at - Last update timestamp (String)"
  (t/HMap :mandatory {:id t/Int
                      :name t/Str
                      :content t/Str
                      :created_at t/Str
                      :updated_at t/Str}
          :optional {:description (t/Option t/Str)}))

;; ============================================================================
;; Function Annotations
;; ============================================================================

(t/ann ^:no-check create-skill
       [SQLiteConnection NewSkill :-> Skill])

(t/ann get-skill-by-id
       [SQLiteConnection t/Int :-> (t/Option Skill)])

(t/ann get-skill-by-name
       [SQLiteConnection t/Str :-> (t/Option Skill)])

(t/ann get-all-skills
       [SQLiteConnection :-> (t/Seqable Skill)])

(t/ann ^:no-check update-skill
       [SQLiteConnection t/Int SkillUpdate :-> Skill])

(t/ann delete-skill
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-skill
  "Creates a new skill and returns the created skill with its ID.

   The skill-data map should contain:
   - :name (required) - Skill name
   - :content (required) - Full skill content/documentation
   - :description (optional) - Brief description

   Returns the complete Skill record with generated ID and timestamps."
  [conn skill-data]
  (let [sql-map {:insert-into :plan_skill
                 :values [skill-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-skill-by-id
  "Retrieves a skill by its ID.

   Returns the Skill record if found, nil otherwise."
  [conn skill-id]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :where [:= :id skill-id]}]
    (db/execute-one conn sql-map)))

(defn get-skill-by-name
  "Retrieves a skill by its name.

   Returns the Skill record if found, nil otherwise."
  [conn skill-name]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :where [:= :name skill-name]}]
    (db/execute-one conn sql-map)))

(defn get-all-skills
  "Retrieves all skills.

   Returns a sequence of Skill records ordered by name."
  [conn]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :order-by [[:name :asc]]}]
    (db/execute-many conn sql-map)))

(defn update-skill
  "Updates an existing skill and returns the updated record.

   The skill-data map can contain any of:
   - :name - New skill name
   - :content - New content
   - :description - New description

   Returns the updated Skill record with new timestamp."
  [conn skill-id skill-data]
  (let [sql-map {:update :plan_skill
                 :set skill-data
                 :where [:= :id skill-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-skill
  "Deletes a skill by its ID.

   Returns the number of rows deleted (0 or 1)."
  [conn skill-id]
  (let [sql-map {:delete-from :plan_skill
                 :where [:= :id skill-id]}]
    (db/execute-one conn sql-map)))
