(defproject cubane/stack "0.1.0-SNAPSHOT"
  :description        "The core stack of the Cubane project."
  :url                "http://github.com/s-ted/cubanostack"
  :author             "Sylvain Tedoldi"
  :license            {:name "Eclipse Public License"
                       :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies       [[org.clojure/clojure "1.8.0"]

                       [org.clojure/core.async "0.2.371"]

                       [clj-time "0.11.0"]
                       [cheshire "5.5.0"]                   ; JSON
                       [org.clojure/data.xml "0.0.8"]
                       [clj-http "2.0.0"]

                       [bidi "2.0.3"]                       ; routing lib

                       [com.draines/postal "1.11.3"]        ; for sending mail

                       [com.orientechnologies/orientdb-core "2.1.13"]

                       [liberator "0.14.1"]   ; easy data as resources exposition
                       [compojure "1.0.2"] ; this currently is needed for liberator

                       [buddy/buddy-hashers "0.11.0"]
                       [buddy/buddy-auth "0.9.0"]
                       [buddy/buddy-sign "0.9.0"]
                       [hiccup "1.0.5"]     ; easy HTML producer

                       [slingshot "0.12.2"] ; advanced exception handling

                       [environ "1.0.2"]

                       [com.taoensso/timbre "4.1.1"]        ; logging
                       [com.taoensso/tower "3.1.0-beta4"]   ; i18n
                       [com.taoensso/sente "1.8.0"]         ; Realtime web comms for Clojure/Script

                       [com.stuartsierra/component "0.3.1"]
                       [metosin/schema-tools "0.8.0"]

                       [ring "1.4.0"]
                       [ring/ring-defaults "0.1.5"]
                       [ring.middleware.logger "0.5.0"]
                       [ring.middleware.conditional "0.2.0" :exclusions [ring]]
                       [http-kit "2.1.19"]

                       ; cljs libs
                       [org.clojure/clojurescript "1.7.228" :scope "provided"]
                       [quiescent/quiescent "0.3.1"]
                       [cljs-http "0.1.39"]
                       [cljsjs/react-bootstrap "0.28.1-1" :exclusions [org.webjars.bower/jquery]]
                       [cubane/cublono-quiescent "0.1.0-SNAPSHOT"]]
  :plugins            [[lein-ring "0.9.6"]
                       [lein-cljsbuild "1.1.2"]
                       [lein-sassc "0.10.4"]
                       [lein-auto "0.1.1"]
                       [lein-environ "1.0.2"]]
  :source-paths       ["src/bo" "src/common" "src/fo"]
  :test-paths         ["test/bo"]
  :min-lein-version   "2.5.3"
  :clean-targets      ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :jar-exclusions     [#".*\.sw?$" #"resources/public/js/compiled/out/"]
  :uberjar-exclusions [#".*\.sw?$" #"resources/public/js/compiled/out/"]


  :main               cubanostack.main
  :repl-options       {:init-ns user
                       :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}


  :cljsbuild          {:builds
                       {:app
                        {:source-paths ["src/fo" "src/common"]

                         :figwheel {:on-jsload "cubanostack.figwheel/on-reload"}

                         :compiler {:main                 cubanostack.core
                                    :asset-path           "/js/compiled/out"
                                    :output-to            "resources/public/js/compiled/cubane.js"
                                    :output-dir           "resources/public/js/compiled/out"
                                    :source-map-timestamp true
                                    :optimizations        :none
                                    :parallel-build       true
                                    :pretty-print         true}}}}

  :figwheel           {:css-dirs       ["resources/public/css"]
                       :ring-handler   user/ring-handler
                       :server-logfile "log/figwheel.log"}

  :doo                {:build "test"}
  :codox              {:project  {:name "cubane"}
                       :defaults {:doc/format :markdown}
                       :language :clojurescript}

  :sassc              [{:src       "src/scss/style.scss"
                        :output-to "resources/public/css/style.css"}]

  :auto               {"sassc" {:file-pattern  #"\.(scss)$"}}

  :profiles           {:dev
                       {:dependencies [[midje "1.8.3"]
                                       [peridot "0.4.3"]
                                       [kerodon "0.7.0" :exclusions [peridot]]
                                       [org.clojure/test.check "0.9.0"]
                                       [figwheel "0.5.0-6"]
                                       [figwheel-sidecar "0.5.0-6"]
                                       [com.cemerick/piggieback "0.2.1"]
                                       [org.clojure/tools.nrepl "0.2.12"]]

                        :injections    [(use 'midje.repl)]
                        :source-paths  ["dev"]

                        :plugins       [[lein-figwheel "0.5.0-6"]
                                        [lein-doo "0.1.6"]
                                        [lein-midje "3.2"]]

                        :env           {:dev "true"}

                        :cljsbuild     {:builds
                                        {:test
                                         {:source-paths ["src/fo" "src/common" "test/fo"]
                                          :compiler
                                          {:output-to     "resources/public/js/compiled/testable.js"
                                           :main          cubanostack.test-runner
                                           :optimizations :none}}}}}

                       :uberjar
                       {:source-paths ^:replace ["src/bo" "src/common"]
                        :hooks                  [leiningen.cljsbuild
                                                 leiningen.sassc]
                        :omit-source            true
                        :aot                    :all
                        :cljsbuild              {:builds
                                                 {:app
                                                  {:source-paths ^:replace ["src/fo" "src/common"]
                                                   :compiler
                                                   {:optimizations :advanced
                                                    :pretty-print  false}}}}}})
