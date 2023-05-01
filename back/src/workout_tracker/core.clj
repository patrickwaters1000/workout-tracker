(ns workout-tracker.core
  (:require
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.walk :refer [keywordize-keys]]
    [compojure.core :refer :all]
    [org.httpkit.server :refer [run-server]]
    [ring.middleware.params :as rmp]
    [stencil.core :as stencil]
    [workout-tracker.db :as db]
    [workout-tracker.runs :as runs]))

;; date (subs (str (t/now)) 0 10)

(def workouts-data (atom nil))

;;(defn get-workouts-table []
;;  (let [template (slurp (io/resource "html/table.mustache"))
;;        table-body (runs/get-run-workouts-table @workouts-data)
;;        params {:table {:headers runs/workouts-table-columns
;;                        :rows (for [row table-body]
;;                                {:cells row})}}]
;;    (stencil/render-string template params)))
;;

(comment
  (get-workouts-table)
  ;;
  )

(defroutes app
  (GET "/" [] (slurp "resources/html/index.html"))
  (GET "/index.js" [] (slurp "resources/js/index.js"))
  (GET "/runs" []
    (json/generate-string
      {:runs (runs/get-run-workouts @workouts-data)}))
  (GET "/running-distance-plot" []
    (json/generate-string
      (runs/get-distance-plot @workouts-data)))
  (POST "/add-run" {body :body}
    (let [m (keywordize-keys (json/parse-string (slurp body)))]
      (println m)
      (runs/add-run! db/db m)
      (reset! workouts-data (db/get-data db/db))
      (json/generate-string
        {:runs (runs/get-run-workouts @workouts-data)}))))

(defn -main [& _]
  (println "Ready!")
  (reset! workouts-data (db/get-data db/db))
  (run-server (rmp/wrap-params app)
              {:port 3000}))

;;(POST "/add-workout" {body :body}
;;      (let [workout (json/parse-string body)]
;;        (db/add-workout workout)))
;;(POST "/get-workouts" {body :body}
;;      (let [params (json/parse-string body)]
;;        (db/get-workouts params)))
