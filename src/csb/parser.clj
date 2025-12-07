(ns csb.parser
  "Clojure code parser that generates a Pandoc-inspired AST.

  This namespace provides functions to parse Clojure source code into a structured
  Abstract Syntax Tree (AST) format inspired by Pandoc's design. The AST includes
  position information for every node, making it suitable for IDE tools, linters,
  formatters, and code analysis tools.

  Example usage:

    (require '[csb.parser :as parser])

    ;; Parse a simple expression
    (parser/parse-string \"(+ 1 2)\")

    ;; Parse a file
    (parser/parse-file \"src/myapp/core.clj\")

    ;; Get AST summary
    (parser/ast-summary (parser/parse-string \"(defn add [x y] (+ x y))\"))"
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as p]
   [typed.clojure :as t])
  (:import
   (rewrite_clj.node.protocols
    Node)))

;; Type Definitions

(t/defalias Position
  "Position information for a node"
  (t/HMap :optional {:start (t/HMap :mandatory {:row t/Int :col t/Int})
                     :end (t/HMap :mandatory {:row t/Int :col t/Int})}))

(t/defalias ASTNode
  "AST node structure"
  (t/HMap :mandatory {:t t/Str}
          :optional {:pos (t/U nil Position)
                     :c (t/U (t/Vec t/Any) nil)}))

(t/defalias AST
  "Top-level AST structure"
  (t/HMap :mandatory {:clojure-ast-version (t/Vec t/Int)
                      :meta t/Any
                      :forms (t/Vec ASTNode)}))

;; Function Annotations

(t/ann ^:no-check token-type [t/Str :-> t/Str])
(t/ann ^:no-check token-value [t/Str t/Str :-> t/Any])
(t/ann ^:no-check make-position [(t/U nil (t/Map t/Keyword t/Any)) :-> (t/U nil Position)])
(t/ann ^:no-check node->ast [Node :-> (t/U nil ASTNode)])
(t/ann ^:no-check parse-string
       (t/IFn [t/Str :-> AST]
              [t/Str t/Any :-> AST]))
(t/ann ^:no-check parse-file [t/Str :-> AST])
(t/ann ^:no-check ast->json
       (t/IFn [AST :-> t/Str]
              [AST (t/HMap :optional {:indent t/Bool}) :-> t/Str]))
(t/ann ^:no-check ast-summary [AST :-> (t/HMap :mandatory {:version (t/Vec t/Int)
                                                           :metadata t/Any
                                                           :form-count t/Int
                                                           :form-types (t/Map t/Str t/Int)
                                                           :total-nodes t/Int})])
(t/ann ^:no-check find-nodes [AST [ASTNode :-> t/Any] :-> (t/Seqable ASTNode)])
(t/ann ^:no-check find-by-type [AST t/Str :-> (t/Seqable ASTNode)])
(t/ann ^:no-check extract-symbols [AST :-> (t/Seqable t/Str)])
(t/ann ^:no-check extract-keywords [AST :-> (t/Seqable t/Str)])
(t/ann ^:no-check find-function-defs [AST :-> (t/Seqable (t/HMap :mandatory {:name t/Str
                                                                             :pos (t/U nil Position)
                                                                             :params (t/U nil ASTNode)
                                                                             :body (t/Vec ASTNode)}))])
(t/ann ^:no-check node-count-by-type [AST :-> (t/Map t/Str t/Int)])
(t/ann ^:no-check pprint-ast [AST :-> nil])

;; AST Node Construction

(defn- token-type
  "Determine the semantic type of a token string."
  [s]
  (cond
    (re-matches #":\S+" s) "keyword"
    (re-matches #"-?\d+\.?\d*" s) "number"
    (re-matches #"\".*\"" s) "string"
    (re-matches #"true|false" s) "boolean"
    (re-matches #"nil" s) "nil"
    :else "symbol"))

(defn- token-value
  "Extract the semantic value from a token string."
  [s token-type]
  (case token-type
    "keyword" s
    "number" (if (re-find #"\." s)
               (parse-double s)
               (parse-long s))
    "string" (subs s 1 (dec (count s)))
    "boolean" (= "true" s)
    "nil" nil
    "symbol" s))

(defn- make-position
  "Create a position map from rewrite-clj metadata."
  [pos]
  (when pos
    {:start {:row (:row pos)
             :col (:col pos)}
     :end {:row (:end-row pos)
           :col (:end-col pos)}}))

(defn node->ast
  "Convert a rewrite-clj node to AST format.

  Returns a map with:
  - :t (type) - Node type as string
  - :pos (position) - Source location with start/end row/col
  - :c (content) - Node content, varies by type

  For tokens, :c is a vector like [\"symbol\" \"defn\"] or [\"number\" 42].
  For collections, :c is a vector of child AST nodes."
  [node]
  (let [tag (n/tag node)
        pos (meta node)
        base {:t (name tag)
              :pos (make-position pos)}]
    (case tag
      ;; Atoms/Tokens
      :token
      (let [s (n/string node)
            ttype (token-type s)
            tval (token-value s ttype)]
        (assoc base :c [ttype tval]))

      ;; Collections
      (:list :vector :map :set)
      (assoc base :c (->> (n/children node)
                          (remove #(#{:whitespace :newline :comma} (n/tag %)))
                          (mapv node->ast)))

      ;; Meta (metadata attached to forms)
      :meta
      (assoc base :c (->> (n/children node)
                          (remove #(#{:whitespace :newline} (n/tag %)))
                          (mapv node->ast)))

      ;; Comments
      :comment
      (assoc base :c [(n/string node)])

      ;; Ignored nodes (whitespace, newlines, commas)
      (:whitespace :newline :comma)
      nil

      ;; Default case
      (assoc base :c [(n/string node)]))))

;; Public API

(defn parse-string
  "Parse Clojure code string into AST format.

  Returns a map with:
  - :clojure-ast-version - Semantic version [major minor patch]
  - :meta - Optional metadata map
  - :forms - Vector of top-level form AST nodes

  Example:
    (parse-string \"(defn add [x y] (+ x y))\")
    => {:clojure-ast-version [1 0 0]
        :meta {}
        :forms [{:t \"list\" :pos {...} :c [...]}]}"
  ([code]
   (parse-string code {}))
  ([code metadata]
   {:clojure-ast-version [1 0 0]
    :meta metadata
    :forms (if (empty? code)
             []
             ;; Parse all top-level forms
             (let [forms-node (p/parse-string-all code)]
               (->> (n/children forms-node)
                    (remove #(#{:whitespace :newline :comment} (n/tag %)))
                    (keep node->ast)
                    vec)))}))

(defn parse-file
  "Parse a Clojure source file into AST format.

  Automatically adds file path to metadata.

  Example:
    (parse-file \"src/myapp/core.clj\")
    => {:clojure-ast-version [1 0 0]
        :meta {:file \"src/myapp/core.clj\" :size 1234}
        :forms [...]}"
  [filepath]
  (let [file (io/file filepath)
        code (slurp file)
        metadata {:file (.getPath file)
                  :size (count code)}]
    (parse-string code metadata)))

(defn ast->json
  "Convert AST to JSON string.

  Options:
  - :indent (boolean) - Pretty print with indentation (default false)

  Example:
    (ast->json (parse-string \"(+ 1 2)\") {:indent true})"
  ([ast]
   (ast->json ast {}))
  ([ast {:keys [indent] :or {indent false}}]
   (json/write-str ast :indent indent)))

;; AST Analysis Functions

(defn ast-summary
  "Generate a summary of the AST.

  Returns:
  - :version - AST format version
  - :metadata - File metadata
  - :form-count - Number of top-level forms
  - :form-types - Frequency map of form types
  - :total-nodes - Total node count in tree

  Example:
    (ast-summary (parse-string \"(defn add [x y] (+ x y))\"))
    => {:version [1 0 0]
        :form-count 1
        :form-types {\"list\" 1}
        :total-nodes 8}"
  [ast]
  {:version (:clojure-ast-version ast)
   :metadata (:meta ast)
   :form-count (count (:forms ast))
   :form-types (frequencies (map :t (:forms ast)))
   :total-nodes (reduce + (map #(count (tree-seq map? :c %)) (:forms ast)))})

(defn find-nodes
  "Find all nodes in AST matching a predicate.

  Predicate receives a node map and should return truthy if it matches.

  Example:
    ;; Find all symbols
    (find-nodes ast (fn [node]
                      (and (= \"token\" (:t node))
                           (= \"symbol\" (first (:c node))))))

    ;; Find all function definitions
    (find-nodes ast (fn [node]
                      (and (= \"list\" (:t node))
                           (= [\"symbol\" \"defn\"] (get-in node [:c 0 :c])))))"
  [ast pred]
  (letfn [(walk [node]
            (cond
              (not (map? node)) []
              (pred node) (cons node (when (vector? (:c node))
                                       (mapcat walk (:c node))))
              (vector? (:c node)) (mapcat walk (:c node))
              :else []))]
    (mapcat walk (:forms ast))))

(defn find-by-type
  "Find all nodes of a specific type.

  Example:
    (find-by-type ast \"list\")   ;; All lists
    (find-by-type ast \"vector\") ;; All vectors"
  [ast node-type]
  (find-nodes ast #(= node-type (:t %))))

(defn extract-symbols
  "Extract all symbol names from AST.

  Returns a sequence of symbol strings.

  Example:
    (extract-symbols (parse-string \"(defn add [x y] (+ x y))\"))
    => (\"defn\" \"add\" \"x\" \"y\" \"+\" \"x\" \"y\")"
  [ast]
  (->> (find-nodes ast (fn [node]
                         (and (= "token" (:t node))
                              (= "symbol" (first (:c node))))))
       (map #(second (:c %)))))

(defn extract-keywords
  "Extract all keyword names from AST.

  Returns a sequence of keyword strings (including :).

  Example:
    (extract-keywords (parse-string \"{:name \\\"Alice\\\" :age 30}\"))
    => (\":name\" \":age\")"
  [ast]
  (->> (find-nodes ast (fn [node]
                         (and (= "token" (:t node))
                              (= "keyword" (first (:c node))))))
       (map #(second (:c %)))))

(defn find-function-defs
  "Find all function definitions in AST.

  Returns sequence of maps with:
  - :name - Function name
  - :pos - Source position
  - :params - Parameter vector AST
  - :body - Function body forms

  Example:
    (find-function-defs (parse-string \"(defn add [x y] (+ x y))\"))
    => [{:name \"add\" :pos {...} :params {...} :body [...]}]"
  [ast]
  (->> (find-nodes ast (fn [node]
                         (and (= "list" (:t node))
                              (seq (:c node))
                              (= ["symbol" "defn"] (get-in node [:c 0 :c])))))
       (map (fn [node]
              (let [children (:c node)
                    name-node (second children)
                    ;; Handle optional docstring
                    has-docstring? (and (>= (count children) 3)
                                        (= "token" (:t (nth children 2)))
                                        (= "string" (first (:c (nth children 2)))))
                    params-idx (if has-docstring? 3 2)
                    params-node (nth children params-idx nil)
                    body-nodes (drop (inc params-idx) children)]
                {:name (second (:c name-node))
                 :pos (:pos node)
                 :params params-node
                 :body (vec body-nodes)})))))

(defn node-count-by-type
  "Count nodes by type.

  Returns a map of {node-type count}.

  Example:
    (node-count-by-type (parse-string \"(defn add [x y] (+ x y))\"))
    => {\"list\" 2 \"token\" 5 \"vector\" 1}"
  [ast]
  (let [count-node (fn count-node [node]
                     (if (map? node)
                       (let [self {(:t node) 1}
                             children (when (vector? (:c node))
                                        (apply merge-with + (map count-node (:c node))))]
                         (merge-with + self (or children {})))
                       {}))]
    (apply merge-with + (map count-node (:forms ast)))))

;; Pretty Printing

(defn pprint-ast
  "Pretty print AST to stdout.

  Prints the AST in a human-readable format with indentation.

  Example:
    (pprint-ast (parse-string \"(+ 1 2)\"))"
  [ast]
  (pprint/pprint ast))

(comment
  ;; Example usage

  ;; Parse simple expressions
  (parse-string "(+ 1 2)")
  (parse-string "{:name \"Alice\" :age 30}")
  (parse-string "[1 2 3]")

  ;; Parse function definition
  (def fn-ast (parse-string "(defn add [x y] (+ x y))"))

  ;; Get summary
  (ast-summary fn-ast)

  ;; Find all symbols
  (extract-symbols fn-ast)

  ;; Find function definitions
  (find-function-defs fn-ast)

  ;; Count nodes by type
  (node-count-by-type fn-ast)

  ;; Convert to JSON
  (println (ast->json fn-ast {:indent true}))

  ;; Parse a file
  (def file-ast (parse-file "src/csb/main.clj"))
  (ast-summary file-ast)
  (find-function-defs file-ast)

  ;; Find all lists
  (find-by-type fn-ast "list")

  ;; Find all vectors
  (find-by-type fn-ast "vector")

  ;; Extract keywords from a map
  (extract-keywords (parse-string "{:name \"Alice\" :age 30 :admin true}"))

  )
