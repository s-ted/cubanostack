;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.server
  (:require
    [clojure.java.io :as io]

    [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
    [ring.middleware.logger :refer [wrap-with-logger]]
    [ring.middleware.conditional :refer [if-url-starts-with if-url-doesnt-start-with]]
    [ring.middleware.reload :refer [wrap-reload]]

    [buddy.auth.middleware :refer [wrap-authentication]]

    [liberator.dev :as liberator]
    [environ.core :as environ]

    [cubanostack.auth :as auth]))


(defn middlewares []
  (concat [#(if-url-starts-with % "/app"
                                (fn [%] (wrap-defaults % api-defaults)))

           #(if-url-doesnt-start-with % "/app"
                                      (fn [%] (wrap-defaults % (assoc site-defaults
                                                                      :proxy true))))]


          [#(wrap-authentication % auth/auth-backend)]

          (when-not (environ/env :dev?) [wrap-with-logger])

          (when (environ/env :dev?) [#(liberator/wrap-trace % :header)])

          (when (environ/env :dev?) [wrap-reload])))
