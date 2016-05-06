;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns user
  (:require
    [cubanostack.app :as app]
    [figwheel-sidecar.system :as figwheel]
    [com.stuartsierra.component :as component]
    [cubanostack.components.handler :as handler]
    [clojure.tools.namespace.repl :refer (refresh)]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def system nil)
(def ring-handler nil)


(defn init []
  (alter-var-root #'system
                  (constantly
                    (app/system-dev
                      :figwheel-system   (figwheel/figwheel-system (figwheel/fetch-config))
                      :css-watcher       (figwheel/css-watcher {:watch-paths ["resources/public/css"]})))))

(defn start []
  (alter-var-root #'system
                  component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s]
                    (when s
                      (component/stop s)))))

(defn go []
  (init)
  (start)
  (alter-var-root #'ring-handler
                  (constantly
                    (handler/handler (get-in system [:Handler])))))
(defn reset []
  (stop)
  (refresh :after 'user/go))



(defn run []
  (go))


(defn browser-repl []
  (figwheel/cljs-repl (:figwheel-system system)))
