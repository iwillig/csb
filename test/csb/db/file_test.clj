(ns csb.db.file-test
  "Tests for file database operations"
  (:require
   [clojure.test :as t]
   [csb.test-helpers :as test-helper]
   [csb.db.file :as db.file]))

(t/use-fixtures :each test-helper/use-sqlite-database)

;; ============================================================================
;; Basic CRUD Tests
;; ============================================================================

(t/deftest test-create-file-with-required-fields
  (t/testing "Creating a file with only required fields"
    (t/testing "Given a project and file data with required fields"
      (let [project (test-helper/create-test-project)
            file-data {:project_id (:id project)
                       :path "/src/core.clj"}
            created-file (db.file/create-file test-helper/*connection* file-data)]
        (t/testing "When the file is created"
          (t/testing "Then it should be created with an ID"
            (t/is (integer? (:id created-file)))
            (t/is (pos? (:id created-file))))
          (t/testing "Then the required fields should be set correctly"
            (t/is (= (:id project) (:project_id created-file)))
            (t/is (= "/src/core.clj" (:path created-file))))
          (t/testing "Then optional fields should be nil"
            (t/is (nil? (:summary created-file))))
          (t/testing "Then timestamps should be set correctly"
            (t/is (string? (:created_at created-file)))
            (t/is (string? (:updated_at created-file)))
            (t/is (= (:created_at created-file) (:updated_at created-file)))))))))

(t/deftest test-create-file-with-summary
  (t/testing "Creating a file with a summary"
    (let [project (test-helper/create-test-project)
          file-data {:project_id (:id project)
                     :path "/src/utils.clj"
                     :summary "Utility functions"}
          created-file (db.file/create-file test-helper/*connection* file-data)]

      (t/is (= (:id project) (:project_id created-file)))
      (t/is (= "/src/utils.clj" (:path created-file)))
      (t/is (= "Utility functions" (:summary created-file))))))

(t/deftest test-create-file-with-all-fields
  (t/testing "Creating a file with all fields set"
    (let [project (test-helper/create-test-project)
          file-data {:project_id (:id project)
                     :path "/src/complete.clj"
                     :summary "Complete file with everything"}
          created-file (db.file/create-file test-helper/*connection* file-data)]

      (t/is (= (:id project) (:project_id created-file)))
      (t/is (= "/src/complete.clj" (:path created-file)))
      (t/is (= "Complete file with everything" (:summary created-file))))))

(t/deftest test-get-file-by-id-exists
  (t/testing "Retrieving an existing file by ID"
    (t/testing "Given an existing file"
      (let [project (test-helper/create-test-project)
            created-file (db.file/create-file
                          test-helper/*connection*
                          {:project_id (:id project)
                           :path "/findable/file.clj"})
            retrieved-file (db.file/get-file-by-id
                            test-helper/*connection*
                            (:id created-file))]
        (t/testing "When retrieving the file by ID"
          (t/testing "Then it should return the file"
            (t/is (not (nil? retrieved-file))))
          (t/testing "Then the retrieved file should have the correct ID"
            (t/is (= (:id created-file) (:id retrieved-file))))
          (t/testing "Then the retrieved file should have the correct project ID"
            (t/is (= (:id project) (:project_id retrieved-file))))
          (t/testing "Then the retrieved file should have the correct path"
            (t/is (= "/findable/file.clj" (:path retrieved-file)))))))))

(t/deftest test-get-file-by-id-not-exists
  (t/testing "Retrieving a non-existent file returns nil"
    (let [retrieved-file (db.file/get-file-by-id
                          test-helper/*connection*
                          99999)]
      (t/is (nil? retrieved-file)))))

(t/deftest test-get-files-by-project-id-empty
  (t/testing "Getting files for a project with no files"
    (let [project (test-helper/create-test-project)
          files (db.file/get-files-by-project-id test-helper/*connection* (:id project))]
      (t/is (empty? files)))))

(t/deftest test-get-files-by-project-id-multiple
  (t/testing "Getting files for a project with multiple files"
    (let [project (test-helper/create-test-project)
          _ (db.file/create-file
             test-helper/*connection*
             {:project_id (:id project)
              :path "/project/file1.clj"})
          _ (db.file/create-file
             test-helper/*connection*
             {:project_id (:id project)
              :path "/project/file2.clj"})
          _ (db.file/create-file
             test-helper/*connection*
             {:project_id (:id project)
              :path "/project/file3.clj"})
          files (db.file/get-files-by-project-id test-helper/*connection* (:id project))]

      (t/is (= 3 (count files)))
      ;; Test that files are returned with their full paths as stored in database
      (t/is (= #{"/project/file1.clj" "/project/file2.clj" "/project/file3.clj"}
               (set (map :path files)))))))

(t/deftest test-update-file-path
  (t/testing "Updating a file's path"
    (let [project (test-helper/create-test-project)
          created-file (db.file/create-file
                        test-helper/*connection*
                        {:project_id (:id project)
                         :path "/old/path.clj"})
          updated-file (db.file/update-file
                        test-helper/*connection*
                        (:id created-file)
                        {:path "/new/path.clj"})]

      (t/is (= (:id created-file) (:id updated-file)))
      (t/is (= "/new/path.clj" (:path updated-file)))
      (t/is (= (:id project) (:project_id updated-file))))))

(t/deftest test-update-file-summary
  (t/testing "Updating a file's summary"
    (let [project (test-helper/create-test-project)
          created-file (db.file/create-file
                        test-helper/*connection*
                        {:project_id (:id project)
                         :path "/file.clj"
                         :summary "Original summary"})
          updated-file (db.file/update-file
                        test-helper/*connection*
                        (:id created-file)
                        {:summary "Updated summary"})]

      (t/is (= "Updated summary" (:summary updated-file))))))

(t/deftest test-update-file-multiple-fields
  (t/testing "Updating multiple fields at once"
    (let [project (test-helper/create-test-project)
          created-file (db.file/create-file
                        test-helper/*connection*
                        {:project_id (:id project)
                         :path "/original.clj"})
          updated-file (db.file/update-file
                        test-helper/*connection*
                        (:id created-file)
                        {:path "/updated.clj"
                         :summary "New summary"})]

      (t/is (= "/updated.clj" (:path updated-file)))
      (t/is (= "New summary" (:summary updated-file))))))

(t/deftest test-delete-file
  (t/testing "Deleting a file"
    (let [project (test-helper/create-test-project)
          created-file (db.file/create-file
                        test-helper/*connection*
                        {:project_id (:id project)
                         :path "/doomed/file.clj"})
          file-id (:id created-file)
          _ (db.file/delete-file test-helper/*connection* file-id)
          retrieved-file (db.file/get-file-by-id
                          test-helper/*connection*
                          file-id)]

      (t/is (nil? retrieved-file)))))

(t/deftest test-delete-non-existent-file
  (t/testing "Deleting a non-existent file returns update-count of 0"
    (let [result (db.file/delete-file test-helper/*connection* 99999)]
      (t/is (= 0 (:next.jdbc/update-count result))))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(t/deftest test-file-lifecycle
  (t/testing "Complete file lifecycle"
    (let [project (test-helper/create-test-project)
          ;; Create
          file (db.file/create-file
                test-helper/*connection*
                {:project_id (:id project)
                 :path "/new/file.clj"})
          _ (t/is (some? (:id file)))

          ;; Read
          fetched (db.file/get-file-by-id
                   test-helper/*connection*
                   (:id file))
          _ (t/is (= (:id file) (:id fetched)))

          ;; Update
          updated (db.file/update-file
                   test-helper/*connection*
                   (:id file)
                   {:path "/updated/file.clj"
                    :summary "Now with summary"})
          _ (t/is (= "/updated/file.clj" (:path updated)))
          _ (t/is (= "Now with summary" (:summary updated)))

          ;; Verify update persisted
          refetched (db.file/get-file-by-id
                     test-helper/*connection*
                     (:id file))
          _ (t/is (= "/updated/file.clj" (:path refetched)))

          ;; Delete
          _ (db.file/delete-file test-helper/*connection* (:id file))
          deleted (db.file/get-file-by-id
                   test-helper/*connection*
                   (:id file))]

      (t/is (nil? deleted)))))

(t/deftest test-file-with-project
  (t/testing "A file can be associated with a project"
    (let [project (test-helper/create-test-project)
          _file (db.file/create-file
                test-helper/*connection*
                {:project_id (:id project)
                 :path "/project/file.clj"})
          files (db.file/get-files-by-project-id
                 test-helper/*connection*
                 (:id project))]

      (t/is (= 1 (count files)))
      (t/is (= (:id project) (:project_id (first files))))
      (t/is (= "/project/file.clj" (:path (first files)))))))
