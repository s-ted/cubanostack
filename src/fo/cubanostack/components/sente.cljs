;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.sente
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.sente  :as sente]
    [cubanostack.components.bus :as b]
    [cubanostack.wrapper.core :as w]
    [cubanostack.components.wrapper-manager :as wm]))





(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )


(defn event-msg-handler  [Bus]
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  (fn [{:as ev-msg :keys [id ?data event]}]
    (-event-msg-handler ev-msg Bus)))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]} Bus]
  (js/console.debug "Unhandled event:" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]} Bus]
  (comment
    (if (= ?data {:first-open? true})
      (js/console.debug "Channel socket successfully established!")
      (js/console.debug "Channel socket state change:" (clj->js ?data)))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [id ?data event]} Bus]
  (b/send! Bus :<server ?data))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]} Bus]
  (comment
    (let [[?uid ?csrf-token ?handshake-data] ?data]
      (js/console.debug "Handshake:" (clj->js ?data)))))





(defprotocol Sente
  (send! [this topic payload]))

(defrecord Sente* [Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (let [{:keys [ch-recv] :as new-this}
          (-> this
              (merge (sente/make-channel-socket! "/chsk" {:type :auto})))

          new-this (assoc new-this ::sente-router-stop!
                          (sente/start-client-chsk-router! ch-recv
                                                           (event-msg-handler Bus)))]
      (wm/register WrapperManager :>server! :sente
                   (w/handler #(send! new-this (:topic %) (:payload %))))
      new-this))

  (stop [this]
    (wm/unregister WrapperManager :>server! :sente)
    ((::sente-router-stop! this))
    this)

  Sente

  (send! [this topic payload]
    ((:send-fn this) [topic payload])))

(defn new-sente []
  (map->Sente* {}))
