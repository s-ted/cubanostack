;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.test-core
  (:require
    clojure.walk
    [clojure.data.json :as json]
    [com.stuartsierra.component :as component]
    [peridot.core :as peridot]
    [cubanostack.app :as app]
    [cubanostack.components.handler :as handler]))


(def ^:private test-config nil)

(let [s (atom nil)]
  (defn reuse-system []
    (swap! s
           (fn [system]
             (if system
               system
               (-> (app/system-test test-config)
                   component/start))))))

(defn new-system []
  (-> (app/system-test test-config)
      component/start))

(defn reuse-handler []
  (-> (reuse-system)
      :Handler
      handler/handler))

(defn new-handler []
  (-> (new-system)
      :Handler
      handler/handler))






(defn parse-json-response [response]
  (-> response
      (get-in [:response :body])

      json/read-str
      clojure.walk/keywordize-keys))


(defn entities->ids [handler listing-url]
  (-> handler
      peridot/session
      (peridot/request listing-url)
      parse-json-response
      (#(map :_id %))))
