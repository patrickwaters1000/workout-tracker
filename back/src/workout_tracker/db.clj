(ns workout-tracker.db
  (:require
    [clj-time.core :as t]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [stencil.core :as stencil]))

(def db {:user "pwaters"
         :dbtype "postgresql"
         :dbname "workout_tracker"})

(defn insert-workout! [db]
  (let [date (subs (str (t/now)) 0 10)
        template (slurp (io/resource "insert_workout.sql"))
        sql (stencil/render-string template {:DATE date})]
    (jdbc/execute! db sql)))

(defn insert-exercise! [db row]
  (let [{:keys [workout-id
                exercise-type]} row
        template (slurp (io/resource "insert_exercise.sql"))
        sql (stencil/render-string template
                                   {:WORKOUT_ID workout-id
                                    :TYPE exercise-type})]
    (jdbc/execute! db sql)))

(defn insert-metric! [db row]
  (let [{:keys [exercise-id
                metric-name
                value
                unit]} row
        template (slurp (io/resource "insert_metric.sql"))
        sql (stencil/render-string template
                                   {:EXERCISE_ID exercise-id
                                    :NAME metric-name
                                    :VALUE value
                                    :UNIT unit})]
    (jdbc/execute! db sql)))

(defn assoc-metrics [exercise-id->metrics exercise]
  (let [exercise-id (:id exercise)
        metrics (get exercise-id->metrics exercise-id)]
    (assoc exercise :metrics metrics)))

(defn assoc-exercises [workout-id->exercises exercise-id->metrics workout]
  (let [workout-id (:id workout)
        exercises (->> (get workout-id->exercises workout-id)
                       (mapv (partial assoc-metrics exercise-id->metrics)))]
    (assoc workout :exercises exercises)))

(defn get-data [db]
  (let [workouts (jdbc/query db "SELECT * FROM workouts;")
        exercises (jdbc/query db "SELECT * FROM exercises;")
        metrics (jdbc/query db "SELECT * FROM metrics;")
        workout-id->exercises (group-by :workout_id exercises)
        exercise-id->metrics (group-by :exercise_id metrics)]
    (mapv (partial assoc-exercises
                   workout-id->exercises
                   exercise-id->metrics)
          workouts)))
