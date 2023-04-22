(ns workout-tracker.runs-test
  (:require
    [clojure.test :refer [is deftest]]
    [workout-tracker.runs :as r]))

(deftest getting-run-workouts-table-row
  (is (= ["2023-04-15" "5.00" "10:00" "165" "1.00 mi @ 6:00\n1.00 mi @ 6:30"]
         (#'r/get-run-workouts-row
          {:date "2023-04-15"
           :type "run"
           :note nil
           :exercises
             [{:type "run"
               :metrics {:distance 5.0 :time 50 :heart-beats 8250}}
              {:type "interval"
               :metrics {:distance 1.0 :time 6.0}}
              {:type "interval"
               :metrics {:distance 1.0 :time 6.5}}]}))))
