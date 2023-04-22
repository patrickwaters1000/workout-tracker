(ns workout-tracker.utils)

(defn get-unique [xs]
  (when (not= 1 (count xs))
    (throw (Exception. (format "There are %d elements." (count xs)))))
  (first xs))
