(ns workout-tracker.core
  (:require
    [cheshire.core :as json]
    [compojure.core :refer :all]
    [org.httpkit.server :refer [run-server]]
    [ring.middleware.params :as rmp]))

(defroutes app
  (GET "/" [] (slurp "front/dist/index.html"))
  (GET "/main.js" [] (slurp "front/dist/main.js"))
  (POST "/add-workout" {body :body}
        (let [workout (json/parse-string body)]
           (db/add-workout workout)))
  (POST "/get-workouts" {body :body}
        (let [params (json/parse-string body)]
          (db/get-workouts params))))
