;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.core
    "This is the main namespace for the **Cubane FO** (cl)JS application")

(comment
  (ns cubanostack.core
    "This is the main namespace for the **Cubane FO** (cl)JS application"

    (:require
      [com.stuartsierra.component :refer [system-map using] :as component]
      [cubanostack.system :as s]
      [cubanostack.components.bus :as bus]))


  (defonce system (atom nil))

  (defn- init []
    (reset! system
            (s/system-definition)))

  (defn start []
    (swap! system component/start))

  (defn stop []
    (swap! system
           (fn [s]
             (when s
               (component/stop s)))))

  (defn
    ^{:export true
      :doc    "This is the **main entry point** of the javascript application."
      :added  "1.0"}
    main

    []

    (try
      (init)
      (start)

      (doto (:Bus @system)
        (bus/send! :route-path! {:path (str js/window.location.pathname js/window.location.search)})
        (bus/send! :renderer {:route-changed? true}))

      (catch :default e
        (.error js/console (clj->js e)))))


  (when (nil? @system)
    (do
      (enable-console-print!)
      (main))))
