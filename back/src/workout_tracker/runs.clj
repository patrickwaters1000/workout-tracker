(ns workout-tracker.runs
  (:require
    [clojure.string :as string]
    [workout-tracker.db :as db]
    [workout-tracker.metrics :as m]
    [workout-tracker.utils :refer [get-unique]]))

(defn- get-metric [metric exercise]
  (get-in exercise [:metrics metric]))

(defn- process-interval [exercise]
  (let [time (get-metric :time exercise)
        distance (get-metric :distance exercise)]
    (when (and time distance)
      {:distance distance
       :pace (/ time distance)})))

(defn- format-intervals [intervals]
  (->> intervals
       (map (fn [{:keys [distance pace]}]
              (format "%.2f mi @ %s" distance (m/format-time pace))))
       (string/join ",")))

(defn- get-run-workouts-row [workout]
  (let [date (:date workout)
        runs (filter #(= "run" (:type %))
                     (:exercises workout))
        distance (->> runs (map #(get-metric :distance %)) (reduce + 0.0))
        time (->> runs (map #(get-metric :time %)) (reduce + 0.0))
        heart-beat-counts (->> runs (map #(get-metric :heart-beats %)))
        heart-beats (when (every? number? heart-beat-counts)
                      (reduce + 0.0 heart-beat-counts))
        heart-rate (when heart-beats
                     (Math/round (/ heart-beats time)))
        intervals (->> (:exercises workout)
                       (filter #(= "interval" (:type %)))
                       (map process-interval)
                       (remove nil?))]
    [date
     (format "%.2f" distance)
     (m/format-time (/ time distance))
     (if-not heart-rate "" (format "%d" heart-rate))
     (if (empty? intervals) "" (format-intervals intervals))]))

(defn get-run-workouts-table [workouts]
  (->> workouts
       (filter #(= "run" (:type %)))
       (sort-by :date)
       (map get-run-workouts-row)))

(defn valid-date? [date-str]
  (some? (re-matches #"\d{4,4}\-\d{2,2}\-\d{2,2}" date-str)))

(defn parse-pace [pace-str]
  (let [[_ minutes-str seconds-str] (re-matches #"(\d+):(.+)" pace-str)
        minutes (Double/parseDouble minutes-str)
        seconds (Double/parseDouble seconds-str)]
    (+ minutes (/ seconds 60.0))))

(defn- get-run-metrics [m]
  (let [distance (Double/parseDouble (:distance m))
        pace (parse-pace (:pace m))
        time (* pace distance)
        heart-beats (when-not (empty? (:heartRate m))
                      (-> (:heartRate m)
                          (Double/parseDouble)
                          (* time)))]
    (->> [{:name "distance" :unit "miles" :value distance}
          {:name "time" :unit "minutes" :value time}
          (when heart-beats
            {:name "heart-beats" :value heart-beats})]
         (remove nil?))))

(defn- add-interval! [db workout-id interval-metrics]
  (let [exercise-id (db/insert-exercise! db {:workout-id workout-id
                                             :type "interval"})]
    (->> interval-metrics
         (map #(assoc % :exercise-id exercise-id))
         (run! #(db/insert-metric! db %)))))

(defn add-run! [db m]
  {:pre [(valid-date? (:date m))]}
  (let [{:keys [date
                intervals]} m
        run-metrics (get-run-metrics m)
        ;; Do this before any insertions. If the input is invalid, we won't
        ;; insert anything.
        interval-metrics (mapv get-run-metrics intervals)
        workout-id (db/insert-workout! db {:type "run"
                                           :date date})
        run-exercise-id (db/insert-exercise! db {:workout-id workout-id
                                                 :type "run"})]
    (->> run-metrics
         (map #(assoc % :exercise-id run-exercise-id))
         (run! #(db/insert-metric! db %)))
    (when-not (empty? intervals)
      (run! #(add-interval! db workout-id %)
            intervals))))

(comment
  (db/get-data db/db)
  ;;
  )
