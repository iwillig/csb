(ns csb.parser-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [csb.parser :as parser]))

(deftest parse-simple-expressions-test
  (testing "Parse simple arithmetic"
    (let [ast (parser/parse-string "(+ 1 2)")]
      (is (= [1 0 0] (:clojure-ast-version ast)))
      (is (= 1 (:form-count (parser/ast-summary ast))))
      (is (= ["+" 1 2] (map #(second (:c %))
                             (get-in ast [:forms 0 :c]))))))
  
  (testing "Parse map literal"
    (let [ast (parser/parse-string "{:name \"Alice\" :age 30}")]
      (is (= "map" (get-in ast [:forms 0 :t])))
      (is (= [":name" ":age"] (parser/extract-keywords ast)))))
  
  (testing "Parse vector"
    (let [ast (parser/parse-string "[1 2 3]")]
      (is (= "vector" (get-in ast [:forms 0 :t])))
      (is (= 3 (count (get-in ast [:forms 0 :c]))))))
  
  (testing "Parse set"
    (let [ast (parser/parse-string "#{1 2 3}")]
      (is (= "set" (get-in ast [:forms 0 :t]))))))

(deftest parse-function-definitions-test
  (testing "Simple function definition"
    (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
          fns (parser/find-function-defs ast)]
      (is (= 1 (count fns)))
      (is (= "add" (:name (first fns))))
      (is (some? (:params (first fns))))
      (is (= 1 (count (:body (first fns)))))))
  
  (testing "Function with docstring"
    (let [ast (parser/parse-string "(defn greet \"Says hello\" [name] (str \"Hello, \" name))")
          fns (parser/find-function-defs ast)]
      (is (= "greet" (:name (first fns))))))
  
  (testing "Multiple function definitions"
    (let [code "(defn add [x y] (+ x y))
                (defn sub [x y] (- x y))"
          ast (parser/parse-string code)
          fns (parser/find-function-defs ast)]
      (is (= 2 (count fns)))
      (is (= ["add" "sub"] (map :name fns))))))

(deftest parse-multiple-forms-test
  (testing "Parse namespace with functions"
    (let [code "(ns myapp.core)
                
                (defn greet [name]
                  (str \"Hello, \" name))
                
                (def users
                  [{:name \"Alice\" :age 30}
                   {:name \"Bob\" :age 25}])"
          ast (parser/parse-string code)]
      (is (> (:form-count (parser/ast-summary ast)) 1))
      (is (= 1 (count (parser/find-function-defs ast)))))))

(deftest extract-symbols-test
  (testing "Extract all symbols"
    (let [ast (parser/parse-string "(defn add [x y] (+ x y))")]
      (is (= ["defn" "add" "x" "y" "+" "x" "y"]
             (parser/extract-symbols ast))))))

(deftest extract-keywords-test
  (testing "Extract keywords from map"
    (let [ast (parser/parse-string "{:name \"Alice\" :age 30 :admin true}")]
      (is (= [":name" ":age" ":admin"]
             (parser/extract-keywords ast))))))

(deftest node-count-test
  (testing "Count nodes by type"
    (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
          counts (parser/node-count-by-type ast)]
      (is (= 2 (get counts "list")))
      (is (= 1 (get counts "vector")))
      (is (>= (get counts "token") 5)))))

(deftest position-tracking-test
  (testing "Nodes have position information"
    (let [ast (parser/parse-string "(+ 1 2)")
          form (first (:forms ast))]
      (is (some? (:pos form)))
      (is (= 1 (get-in form [:pos :start :row])))
      (is (= 1 (get-in form [:pos :start :col]))))))

(deftest json-output-test
  (testing "Convert AST to JSON"
    (let [ast (parser/parse-string "(+ 1 2)")
          json (parser/ast->json ast)]
      (is (string? json))
      (is (.contains json "clojure-ast-version"))))
  
  (testing "Pretty-print JSON"
    (let [ast (parser/parse-string "(+ 1 2)")
          json (parser/ast->json ast {:indent true})]
      (is (.contains json "\n")))))

(deftest token-types-test
  (testing "Recognize number tokens"
    (let [ast (parser/parse-string "42")]
      (is (= ["number" 42] (get-in ast [:forms 0 :c])))))
  
  (testing "Recognize string tokens"
    (let [ast (parser/parse-string "\"hello\"")]
      (is (= ["string" "hello"] (get-in ast [:forms 0 :c])))))
  
  (testing "Recognize keyword tokens"
    (let [ast (parser/parse-string ":name")]
      (is (= ["keyword" ":name"] (get-in ast [:forms 0 :c])))))
  
  (testing "Recognize boolean tokens"
    (let [ast (parser/parse-string "true")]
      (is (= ["boolean" true] (get-in ast [:forms 0 :c])))))
  
  (testing "Recognize nil token"
    (let [ast (parser/parse-string "nil")]
      (is (= ["nil" nil] (get-in ast [:forms 0 :c]))))))

(deftest find-nodes-test
  (testing "Find all lists"
    (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
          lists (parser/find-by-type ast "list")]
      (is (= 2 (count lists)))))
  
  (testing "Find all vectors"
    (let [ast (parser/parse-string "(defn add [x y] [1 2 3])")
          vectors (parser/find-by-type ast "vector")]
      (is (= 2 (count vectors))))))
