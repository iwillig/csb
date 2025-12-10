(ns csb.db.skill
  "Database operations for skill entities.
   
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
       [SQLiteConnection NewSkill :-> (types/Result Skill)])

(t/ann ^:no-check get-skill-by-id
       [SQLiteConnection t/Int :-> (types/Result (t/Option Skill))])

(t/ann ^:no-check get-skill-by-name
       [SQLiteConnection t/Str :-> (types/Result (t/Option Skill))])

(t/ann ^:no-check get-all-skills
       [SQLiteConnection :-> (types/Result (t/Seqable Skill))])

(t/ann ^:no-check update-skill
       [SQLiteConnection t/Int SkillUpdate :-> (types/Result Skill)])

(t/ann ^:no-check delete-skill
       [SQLiteConnection t/Int :-> (types/Result t/Any)])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-skill
  "Creates a new skill and returns the created skill with its ID.

   The skill-data map should contain:
   - :name (required) - Skill name
   - :content (required) - Full skill content/documentation
   - :description (optional) - Brief description

   Returns Result<Skill> - the created record or Failure on database error."
  [conn skill-data]
  (db/execute-one conn {:insert-into :plan_skill
                        :values [skill-data]
                        :returning [:*]}))

(defn get-skill-by-id
  "Retrieves a skill by its ID.

   Returns Result<Skill | nil> - the record, nil if not found, or Failure."
  [conn skill-id]
  (db/execute-one conn {:select [:*]
                        :from [:plan_skill]
                        :where [:= :id skill-id]}))

(defn get-skill-by-name
  "Retrieves a skill by its name.

   Returns Result<Skill | nil> - the record, nil if not found, or Failure."
  [conn skill-name]
  (db/execute-one conn {:select [:*]
                        :from [:plan_skill]
                        :where [:= :name skill-name]}))

(defn get-all-skills
  "Retrieves all skills.

   Returns Result<Seq<Skill>> - sequence of records or Failure."
  [conn]
  (db/execute-many conn {:select [:*]
                         :from [:plan_skill]
                         :order-by [[:name :asc]]}))

(defn update-skill
  "Updates an existing skill and returns the updated record.

   The skill-data map can contain any of:
   - :name - New skill name
   - :content - New content
   - :description - New description

   Returns Result<Skill> - the updated record or Failure."
  [conn skill-id skill-data]
  (db/execute-one conn {:update :plan_skill
                        :set skill-data
                        :where [:= :id skill-id]
                        :returning [:*]}))

(defn delete-skill
  "Deletes a skill by its ID.

   Returns Result<{:next.jdbc/update-count n}> - update count or Failure."
  [conn skill-id]
  (db/execute-one conn {:delete-from :plan_skill
                        :where [:= :id skill-id]}))
