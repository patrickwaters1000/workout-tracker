(ns workout-tracker.metrics)

(defn load-metric [{:keys [name value unit]}]
  (case name
    "time" (case unit
             "minutes" value
             "seconds" (/ value 60.0))
    "distance" (case unit
                 "miles" value
                 "meters" (/ value 1609.3))
    "heart-beats" value))

(defn load-metrics [metrics]
  (reduce (fn [acc metric]
            (let [k (keyword (:name metric))
                  v (load-metric metric)]
              (assoc acc k v)))
          {}
          metrics))

(defn format-time [t-min & {:keys [show-tenths]}]
  (let [tenths (Math/round (* t-min 600.0))
        hours (-> tenths (quot 36000))
        minutes (-> tenths (mod 36000) (quot 600))
        seconds (-> tenths (mod 600) (quot 10))
        frac (mod tenths 10)]
    (if show-tenths
      (cond
        (pos? hours) (format "%d:%02d:%02d.%d" hours minutes seconds frac)
        (pos? minutes) (format "%d:%02d.%d" minutes seconds frac)
        :else (format "%d.%d" seconds frac))
      (cond
        (pos? hours) (format "%d:%02d:%02d" hours minutes seconds)
        (pos? minutes) (format "%d:%02d" minutes seconds)
        :else (format "%d" seconds)))))
