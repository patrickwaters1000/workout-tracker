(ns workout-tracker.db
  (:require
    [clj-time.core :as t]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [stencil.core :as stencil]
    [workout-tracker.metrics :as m]))

(def db {:user "pwaters"
         :dbtype "postgresql"
         :dbname "workout_tracker"})

(defn insert-workout! [db row]
  (let [{:keys [type date]} row
        template (slurp (io/resource "sql/insert_workout.sql"))
        sql (stencil/render-string template {:DATE date
                                             :TYPE type})]
    (jdbc/execute! db sql)
    (->> (jdbc/query db "SELECT max(id) AS id FROM workouts")
         first
         :id)))

(defn insert-exercise! [db row]
  (let [{:keys [workout-id
                type]} row
        template (slurp (io/resource "sql/insert_exercise.sql"))
        sql (stencil/render-string template
                                   {:WORKOUT_ID workout-id
                                    :TYPE type})]
    (jdbc/execute! db sql)
    (->> (jdbc/query db "SELECT max(id) AS id FROM exercises")
         first
         :id)))

(defn insert-metric! [db row]
  (let [{:keys [exercise-id
                name
                value
                unit]} row
        template (slurp (io/resource "sql/insert_metric.sql"))
        sql (stencil/render-string template
                                   {:EXERCISE_ID exercise-id
                                    :NAME name
                                    :VALUE value
                                    :UNIT unit})]
    (try (jdbc/execute! db sql)
         (catch Exception e
           (throw (Exception. sql))))))

(defn assoc-metrics [exercise-id->metrics exercise]
  (let [exercise-id (:id exercise)
        metrics (m/load-metrics (get exercise-id->metrics exercise-id))]
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
