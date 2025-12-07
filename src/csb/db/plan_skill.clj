(ns csb.db.plan-skill
  "Database operations for plan skill entities"
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

(t/defalias NewPlanSkill
  "Data required to create a new plan skill.

  Required fields:
  - :name - The name of the skill (String)
  - :content - The full content/documentation of the skill (String)

  Optional fields:
  - :description - Brief description of the skill (String or nil)"
  (t/HMap :mandatory {:name t/Str
                      :content t/Str}
          :optional {:description (t/Option t/Str)}))

(t/defalias PlanSkillUpdate
  "Data for updating an existing plan skill. All fields are optional."
  (t/HMap :optional {:name t/Str
                     :content t/Str
                     :description (t/Option t/Str)}))

(t/defalias PlanSkill
  "Complete plan skill record as returned from database.

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

(t/ann ^:no-check create-plan-skill
       [SQLiteConnection NewPlanSkill :-> PlanSkill])

(t/ann ^:no-check get-plan-skill-by-id
       [SQLiteConnection t/Int :-> (t/Option PlanSkill)])

(t/ann ^:no-check get-plan-skill-by-name
       [SQLiteConnection t/Str :-> (t/Option PlanSkill)])

(t/ann ^:no-check get-all-plan-skills
       [SQLiteConnection :-> (t/Seqable PlanSkill)])

(t/ann ^:no-check search-plan-skills
       [SQLiteConnection t/Str :-> (t/Seqable PlanSkill)])

(t/ann ^:no-check update-plan-skill
       [SQLiteConnection t/Int PlanSkillUpdate :-> PlanSkill])

(t/ann ^:no-check delete-plan-skill
       [SQLiteConnection t/Int :-> t/Any])

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-plan-skill
  "Creates a new plan skill and returns the created skill with its ID.

  The skill-data map should contain:
  - :name (required) - Skill name
  - :content (required) - Full skill content/documentation
  - :description (optional) - Brief description

  Returns the complete PlanSkill record with generated ID and timestamps."
  [conn skill-data]
  (let [sql-map {:insert-into :plan_skill
                 :values [skill-data]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn get-plan-skill-by-id
  "Retrieves a plan skill by its ID.

  Returns the PlanSkill record if found, nil otherwise."
  [conn skill-id]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :where [:= :id skill-id]}]
    (db/execute-one conn sql-map)))

(defn get-plan-skill-by-name
  "Retrieves a plan skill by its name.

  Returns the PlanSkill record if found, nil otherwise."
  [conn skill-name]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :where [:= :name skill-name]}]
    (db/execute-one conn sql-map)))

(defn get-all-plan-skills
  "Retrieves all plan skills.

  Returns a sequence of PlanSkill records ordered by name."
  [conn]
  (let [sql-map {:select [:*]
                 :from [:plan_skill]
                 :order-by [[:name :asc]]}]
    (db/execute-many conn sql-map)))

(defn search-plan-skills
  "Searches plan skills using full-text search on content.

  Returns a sequence of PlanSkill records matching the search query."
  [conn search-query]
  ;; Use next.jdbc directly for parameterized raw SQL
  (jdbc/execute!
   conn
   ["SELECT plan_skill.* FROM plan_skill 
     JOIN plan_skill_fts ON plan_skill.id = plan_skill_fts.rowid 
     WHERE plan_skill_fts MATCH ? 
     ORDER BY rank" search-query]
   {:builder-fn rs/as-unqualified-maps}))

(defn update-plan-skill
  "Updates an existing plan skill and returns the updated record.

  The skill-data map can contain any of:
  - :name - New skill name
  - :content - New content
  - :description - New description

  Returns the updated PlanSkill record with new timestamp."
  [conn skill-id skill-data]
  (let [sql-map {:update :plan_skill
                 :set skill-data
                 :where [:= :id skill-id]
                 :returning [:*]}]
    (db/execute-one conn sql-map)))

(defn delete-plan-skill
  "Deletes a plan skill by its ID.

  Note: This will fail if the skill is referenced by any plans
  (via plan_skills junction table) due to foreign key constraints.

  Returns the number of rows deleted (0 or 1)."
  [conn skill-id]
  (let [sql-map {:delete-from :plan_skill
                 :where [:= :id skill-id]}]
    (db/execute-one conn sql-map)))
