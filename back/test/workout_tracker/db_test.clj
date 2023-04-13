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
  (jdbc/execute! db (slurp (io/resource "create_workouts.sql")))
  (jdbc/execute! db (slurp (io/resource "create_exercises.sql")))
  (jdbc/execute! db (slurp (io/resource "create_metrics.sql"))))

(defn tear-down! [db]
  (jdbc/execute! db "DROP TABLE IF EXISTS metrics;")
  (jdbc/execute! db "DROP TABLE IF EXISTS exercises;")
  (jdbc/execute! db "DROP TABLE IF EXISTS workouts;"))

(deftest inserting-and-selecting
  (setup! test-db)
  (workout-tracker.db/insert-workout! test-db)
  (workout-tracker.db/insert-exercise! test-db
                                       {:workout-id 1 :exercise-type "run"})
  (workout-tracker.db/insert-metric! test-db
                                     {:exercise-id 1
                                      :metric-name "distance"
                                      :value 3.0
                                      :unit "miles"})
  (workout-tracker.db/insert-metric! test-db
                                     {:exercise-id 1
                                      :metric-name "time"
                                      :value 30.0
                                      :unit "minutes"})
  (let [results (workout-tracker.db/get-data test-db)]
    (tear-down! test-db)
    (is (= [{:id 1
             :date "2023-04-13"
             :note nil
             :exercises [{:id 1
                          :workout_id 1
                          :type "run"
                          :note nil
                          :metrics [{:id 1
                                     :exercise_id 1
                                     :name "distance"
                                     :value 3.0
                                     :unit "miles"
                                     :note nil}
                                    {:id 2
                                     :exercise_id 1
                                     :name "time"
                                     :value 30.0
                                     :unit "minutes"
                                     :note nil}]}]}]
           results))))
