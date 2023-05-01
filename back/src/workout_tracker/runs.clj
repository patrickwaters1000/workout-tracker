(ns workout-tracker.runs
  (:require
    [clj-time.core :as t]
    [clj-time.periodic :as tp]
    [clojure.string :as string]
    [workout-tracker.db :as db]
    [workout-tracker.metrics :as m]
    [workout-tracker.utils :as u])
  (:import
    (org.joda.time DateTime)))

(defn- get-metric [metric exercise]
  (get-in exercise [:metrics metric]))

(defn- process-interval [exercise]
  (let [time (get-metric :time exercise)
        distance (get-metric :distance exercise)]
    (when (and time distance)
      {:distance distance
       :pace (/ time distance)})))

(defn- format-intervals [intervals]
  (map #(update % :pace m/format-time) intervals))

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
    {:date date
     :distance distance
     :pace (/ time distance)
     :heartRate heart-rate
     :intervals intervals}))

(defn get-run-workouts [workouts]
  (->> workouts
       (filter #(= "run" (:type %)))
       (sort-by :date)
       (map get-run-workouts-row)))

(defn week [date-str]
  (let [date-1 (DateTime. "2023-01-02") ;; First Monday of 2023.
        date-2 (DateTime. date-str)
        week-idx (if (t/before? date-1 date-2)
                   (t/in-weeks (t/interval date-1 date-2))
                   (- (inc (t/in-weeks (t/interval date-2 date-1)))))]
    (if (neg? week-idx)
      (t/minus date-1 (t/weeks (Math/abs week-idx)))
      (t/plus date-1 (t/weeks week-idx)))))

(defn smooth-date [row]
  (for [num-days [-2 1 0 1 2]
        :let [update-date-fn (if (pos? num-days)
                               #(t/plus % (t/days num-days))
                               #(t/minus % (t/days (Math/abs num-days))))]]
    (-> row
        (update :date update-date-fn)
        (update :distance / 5.0))))

(defn get-distance-plot [workouts]
  (let [week->distance (->> (get-run-workouts workouts)
                            (map (fn [row] (update row :date #(DateTime. %))))
                            (mapcat smooth-date)
                            (group-by (comp week :date))
                            (u/map-vals (fn [rows]
                                          (->> rows
                                               (map :distance)
                                               (reduce + 0.0)))))
        observed-weeks (sort (keys week->distance))
        dates (tp/periodic-seq (first observed-weeks)
                               (t/plus (last observed-weeks)
                                       (t/weeks 1))
                               (t/weeks 1))
        distances (map #(get week->distance % 0.0) dates)]
    {:date (map #(subs (str %) 0 10) dates) :distance distances}))

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
            interval-metrics))))

(comment
  (get-distance-plot (db/get-data db/db))
  (->> (db/get-data db/db)
       (get-run-workouts)
       (map (fn [row] (update row :date #(DateTime. %))))
       ;;(mapcat smooth-date)
       (group-by (comp week :date))
       (sort-by key))
  ;;
  )
