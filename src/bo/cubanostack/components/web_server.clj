;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.web-server
  (:require [com.stuartsierra.component :as c]
            [cubanostack.components.handler :as handler]
            [org.httpkit.server :refer [run-server]]))

(defprotocol WebServer
  (server [this]))


(defrecord WebServer* [options Handler]
  c/Lifecycle

  (start [this]
    (let [handler (handler/handler Handler)
          server  (run-server handler options)]
      (assoc this ::server server)))

  (stop [this]
    (when-let [server (::server this)]
      (server)
      this))

  WebServer

  (server [this]
    (::server this)))

(defn new-web-server
  ([port]
   (new-web-server port nil {}))

  ([port handler]
   (new-web-server port handler {}))

  ([port handler options]
   (map->WebServer* {:options (merge {:port port}
                                     options)
                     :Handler handler})))
