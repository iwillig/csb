(ns csb.parser-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [csb.parser :as parser]))

(deftest parse-simple-expressions-test
  (testing "Parse simple arithmetic expressions"
    (testing "Given a simple arithmetic expression (+ 1 2)"
      (let [ast (parser/parse-string "(+ 1 2)")]
        (testing "When parsing the expression"
          (testing "Then the AST version should be [1 0 0]"
            (is (= [1 0 0] (:clojure-ast-version ast))))
          (testing "Then the form count should be 1"
            (is (= 1 (:form-count (parser/ast-summary ast)))))
          (testing "Then the tokens should be ['+' 1 2]"
            (is (= ["+" 1 2] (map #(second (:c %))
                                  (get-in ast [:forms 0 :c]))))))))

    (testing "Given a map literal {:name \"Alice\" :age 30}"
      (let [ast (parser/parse-string "{:name \"Alice\" :age 30}")]
        (testing "When parsing the expression"
          (testing "Then the form type should be 'map'"
            (is (= "map" (get-in ast [:forms 0 :t]))))
          (testing "Then the extracted keywords should be [\":name\" \":age\"]"
            (is (= [":name" ":age"] (parser/extract-keywords ast)))))))

    (testing "Given a vector [1 2 3]"
      (let [ast (parser/parse-string "[1 2 3]")]
        (testing "When parsing the expression"
          (testing "Then the form type should be 'vector'"
            (is (= "vector" (get-in ast [:forms 0 :t]))))
          (testing "Then the number of children should be 3"
            (is (= 3 (count (get-in ast [:forms 0 :c]))))))))

    (testing "Given a set #{1 2 3}"
      (let [ast (parser/parse-string "#{1 2 3}")]
        (testing "When parsing the expression"
          (testing "Then the form type should be 'set'"
            (is (= "set" (get-in ast [:forms 0 :t])))))))))

(deftest parse-function-definitions-test
  (testing "Parse function definitions"
    (testing "Given a simple function definition (defn add [x y] (+ x y))"
      (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
            fns (parser/find-function-defs ast)]
        (testing "When finding function definitions"
          (testing "Then there should be exactly 1 function"
            (is (= 1 (count fns))))
          (testing "Then the function name should be 'add'"
            (is (= "add" (:name (first fns)))))
          (testing "Then the function should have parameters"
            (is (some? (:params (first fns)))))
          (testing "Then the function body should have 1 element"
            (is (= 1 (count (:body (first fns)))))))))

    (testing "Given a function with docstring (defn greet \"Says hello\" [name] (str \"Hello, \" name))"
      (let [ast (parser/parse-string "(defn greet \"Says hello\" [name] (str \"Hello, \" name))")
            fns (parser/find-function-defs ast)]
        (testing "When finding function definitions"
          (testing "Then the function name should be 'greet'"
            (is (= "greet" (:name (first fns))))))))

    (testing "Given multiple function definitions"
      (let [code "(defn add [x y] (+ x y))
                  (defn sub [x y] (- x y))"
            ast (parser/parse-string code)
            fns (parser/find-function-defs ast)]
        (testing "When parsing the code"
          (testing "Then there should be exactly 2 functions"
            (is (= 2 (count fns))))
          (testing "Then the function names should be [\"add\" \"sub\"]"
            (is (= ["add" "sub"] (map :name fns)))))))))

(deftest parse-multiple-forms-test
  (testing "Parse multiple forms in a namespace"
    (testing "Given a namespace with multiple forms"
      (let [code "(ns myapp.core)

                  (defn greet [name]
                    (str \"Hello, \" name))

                  (def users
                    [{:name \"Alice\" :age 30}
                     {:name \"Bob\" :age 25}])"
            ast (parser/parse-string code)]
        (testing "When parsing the namespace"
          (testing "Then there should be more than 1 form"
            (is (> (:form-count (parser/ast-summary ast)) 1)))
          (testing "Then there should be exactly 1 function definition"
            (is (= 1 (count (parser/find-function-defs ast))))))))))

(deftest extract-symbols-test
  (testing "Extract symbols from function definitions"
    (testing "Given a function definition (defn add [x y] (+ x y))"
      (let [ast (parser/parse-string "(defn add [x y] (+ x y))")]
        (testing "When extracting symbols"
          (testing "Then all symbols should be extracted correctly"
            (is (= ["defn" "add" "x" "y" "+" "x" "y"]
                   (parser/extract-symbols ast)))))))))

(deftest extract-keywords-test
  (testing "Extract keywords from map literals"
    (testing "Given a map with keywords {:name \"Alice\" :age 30 :admin true}"
      (let [ast (parser/parse-string "{:name \"Alice\" :age 30 :admin true}")]
        (testing "When extracting keywords"
          (testing "Then all keywords should be extracted correctly"
            (is (= [":name" ":age" ":admin"]
                   (parser/extract-keywords ast)))))))))

(deftest node-count-test
  (testing "Count nodes by type in AST"
    (testing "Given a function definition (defn add [x y] (+ x y))"
      (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
            counts (parser/node-count-by-type ast)]
        (testing "When counting nodes by type"
          (testing "Then there should be 2 list nodes"
            (is (= 2 (get counts "list"))))
          (testing "Then there should be 1 vector node"
            (is (= 1 (get counts "vector"))))
          (testing "Then there should be at least 5 token nodes"
            (is (>= (get counts "token") 5))))))))

(deftest position-tracking-test
  (testing "Track position information in nodes"
    (testing "Given a simple expression (+ 1 2)"
      (let [ast (parser/parse-string "(+ 1 2)")
            form (first (:forms ast))]
        (testing "When getting position information"
          (testing "Then the form should have position information"
            (is (some? (:pos form))))
          (testing "Then the start row should be 1"
            (is (= 1 (get-in form [:pos :start :row]))))
          (testing "Then the start column should be 1"
            (is (= 1 (get-in form [:pos :start :col])))))))))

(deftest json-output-test
  (testing "Convert AST to JSON"
    (testing "Given an AST from parsing (+ 1 2)"
      (let [ast (parser/parse-string "(+ 1 2)")
            json (parser/ast->json ast)]
        (testing "When converting to JSON"
          (testing "Then the result should be a string"
            (is (string? json)))
          (testing "Then the JSON should contain 'clojure-ast-version'"
            (is (.contains json "clojure-ast-version"))))))

    (testing "Given an AST from parsing (+ 1 2) with indentation"
      (let [ast (parser/parse-string "(+ 1 2)")
            json (parser/ast->json ast {:indent true})]
        (testing "When converting to indented JSON"
          (testing "Then the result should contain newlines"
            (is (.contains json "\n"))))))))

(deftest token-types-test
  (testing "Recognize different token types"
    (testing "Given a number token 42"
      (let [ast (parser/parse-string "42")]
        (testing "When parsing the number"
          (testing "Then the token should be recognized as a number"
            (is (= ["number" 42] (get-in ast [:forms 0 :c])))))))

    (testing "Given a string token \"hello\""
      (let [ast (parser/parse-string "\"hello\"")]
        (testing "When parsing the string"
          (testing "Then the token should be recognized as a string"
            (is (= ["string" "hello"] (get-in ast [:forms 0 :c])))))))

    (testing "Given a keyword token :name"
      (let [ast (parser/parse-string ":name")]
        (testing "When parsing the keyword"
          (testing "Then the token should be recognized as a keyword"
            (is (= ["keyword" ":name"] (get-in ast [:forms 0 :c])))))))

    (testing "Given a boolean token true"
      (let [ast (parser/parse-string "true")]
        (testing "When parsing the boolean"
          (testing "Then the token should be recognized as a boolean"
            (is (= ["boolean" true] (get-in ast [:forms 0 :c])))))))

    (testing "Given a nil token nil"
      (let [ast (parser/parse-string "nil")]
        (testing "When parsing nil"
          (testing "Then the token should be recognized as nil"
            (is (= ["nil" nil] (get-in ast [:forms 0 :c])))))))))

(deftest find-nodes-test
  (testing "Find nodes of specific types"
    (testing "Given a function definition with lists and vectors"
      (let [ast (parser/parse-string "(defn add [x y] (+ x y))")
            lists (parser/find-by-type ast "list")]
        (testing "When finding all list nodes"
          (testing "Then there should be 2 list nodes"
            (is (= 2 (count lists))))))

      (let [ast (parser/parse-string "(defn add [x y] [1 2 3])")
            vectors (parser/find-by-type ast "vector")]
        (testing "When finding all vector nodes"
          (testing "Then there should be 2 vector nodes"
            (is (= 2 (count vectors)))))))))
