(ns workout-tracker.utils)

(defn get-unique [xs]
  (when (not= 1 (count xs))
    (throw (Exception. (format "There are %d elements." (count xs)))))
  (first xs))

(defn map-vals [f m]
  (reduce-kv (fn [acc k v]
               (assoc acc k (f v)))
             {}
             m))
