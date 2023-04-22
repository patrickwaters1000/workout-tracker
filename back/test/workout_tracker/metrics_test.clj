(ns workout-tracker.metrics-test
  (:require
    [clojure.test :refer [is deftest]]
    [workout-tracker.metrics :as m]))

(deftest formatting-times
  (is (= "6:30" (m/format-time 6.5)))
  (is (= "1:40:00" (m/format-time 100)))
  (is (= "6:30.0" (m/format-time 6.5 :show-tenths true))))
