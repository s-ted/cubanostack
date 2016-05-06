;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.handler
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.middleware :as middleware]
    [cubanostack.components.router :as router]))

(defprotocol Handler
  (handler [this]))

(defrecord Handler* [Middleware Router]
  c/Lifecycle

  (start [this]
    (let [wrap-mw      (or (middleware/wrap-mw Middleware)
                           identity)
          ring-handler (wrap-mw (router/ring-handler Router))]
      (assoc this ::handler ring-handler)))

  (stop [this]
    (dissoc this ::handler))

  Handler

  (handler [this]
    (::handler this)))

(defn new-handler
  ([]
   (map->Handler* {})))
