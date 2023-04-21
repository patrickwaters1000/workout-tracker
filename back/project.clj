(defproject workout-tracker "0.1.0-SNAPSHOT"
  :description "App for recording runs and other workouts"
  :url "http://example.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.10.0"]
                 [compojure "1.1.8"]
                 [jakarta.xml.bind/jakarta.xml.bind-api "2.3.2"] ;; Apparently required for http-kit
                 [org.glassfish.jaxb/jaxb-runtime "2.3.2"] ;; Apparently required for http-kit
                 [org.postgresql/postgresql "42.2.10"]
                 [http-kit "2.1.16"]
                 [clj-time "0.15.2"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [stencil "0.5.0"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:handler workout-tracker.core/app}
  :repositories [["jitpack" "https://jitpack.io"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
