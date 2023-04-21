(ns workout-tracker.db-test
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.test :refer [is deftest]]
    [workout-tracker.db]))

(def test-db {:user "pwaters"
              :dbtype "postgresql"
              :dbname "workout_tracker_test"})

(defn setup! [db]
  (jdbc/execute! db (slurp (io/resource "sql/create_workouts.sql")))
  (jdbc/execute! db (slurp (io/resource "sql/create_exercises.sql")))
  (jdbc/execute! db (slurp (io/resource "sql/create_metrics.sql"))))

(defn tear-down! [db]
  (jdbc/execute! db "DROP TABLE IF EXISTS metrics;")
  (jdbc/execute! db "DROP TABLE IF EXISTS exercises;")
  (jdbc/execute! db "DROP TABLE IF EXISTS workouts;"))

(comment
  (tear-down! workout-tracker.db/db)
  (setup! workout-tracker.db/db)
  (workout-tracker.db/insert-workout! workout-tracker.db/db
                                      {:type "run"
                                       :date "2023-04-13"})
  (workout-tracker.db/insert-exercise! workout-tracker.db/db
                                       {:workout-id 1
                                        :type "run"})
  (workout-tracker.db/insert-metric! workout-tracker.db/db
                                     {:exercise-id 1
                                      :name "distance"
                                      :value 3.0
                                      :unit "miles"})
  (workout-tracker.db/insert-metric! workout-tracker.db/db
                                     {:exercise-id 1
                                      :name "time"
                                      :value 30.0
                                      :unit "minutes"})
  ;;
  )

(deftest inserting-and-selecting
  (setup! test-db)
  (workout-tracker.db/insert-workout! test-db
                                      {:type "run"
                                       :date "2023-04-13"})
  (workout-tracker.db/insert-exercise! test-db
                                       {:workout-id 1 :type "run"})
  (workout-tracker.db/insert-metric! test-db
                                     {:exercise-id 1
                                      :name "distance"
                                      :value 3.0
                                      :unit "miles"})
  (workout-tracker.db/insert-metric! test-db
                                     {:exercise-id 1
                                      :name "time"
                                      :value 30.0
                                      :unit "minutes"})
  (let [results (workout-tracker.db/get-data test-db)]
    (tear-down! test-db)
    (is (= [{:id 1
             :type "run"
             :date "2023-04-13"
             :note nil
             :exercises [{:id 1
                          :workout_id 1
                          :type "run"
                          :note nil
                          :metrics {:distance 3.0
                                    :time 30.0}}]}]
           results))))
