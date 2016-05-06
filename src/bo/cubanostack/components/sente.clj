;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.sente
  (:require [com.stuartsierra.component :as c]
            [cubanostack.components.bus :as bus]
            [cubanostack.components.wrapper-manager :as wm]
            [cubanostack.wrapper.core :as w]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defprotocol SenteCommunicator
  (send! [this uid event-type payload])
  (ajax-get-or-ws-handshake-fn [this])
  (ajax-post-fn [this])
  (connected-uids [this]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn- event-msg-handler [Bus]
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  (fn [{:as ev-msg :keys [id ?data event]}]
    (-event-msg-handler ev-msg Bus)))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]} Bus]
  (bus/send! Bus (keyword (str "sente." (namespace id)) (name id))
             ev-msg))



(defrecord SenteCommunicator* [path
                               ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids
                               Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (if (::sente-router this)
      this
      (let [{:keys [ch-recv ajax-get-or-ws-handshake-fn ajax-post-fn]
             :as sente-info}
            (sente/make-channel-socket! sente-web-server-adapter {})

            new-this (-> this
                         (merge sente-info)
                         (assoc ::sente-router
                                (sente/start-server-chsk-router!
                                  ch-recv (event-msg-handler Bus))))]

        (wm/register WrapperManager :>client! :sente
                     (w/handler
                       (fn [{:keys [uid topic payload]}]
                         (send! new-this uid topic payload))))

        (bus/send! Bus :router/add-route {:route   [path :get]
                                          :handler ajax-get-or-ws-handshake-fn})

        (bus/send! Bus :router/add-route {:route   [path :post]
                                          :handler ajax-post-fn})

        new-this)))

  (stop [this]
    (bus/send! Bus :router/remove-route {:route [path :post]})

    (bus/send! Bus :router/remove-route {:route [path :get]})

    (wm/unregister WrapperManager :>client! :sente)

    (when-let [stop-f (::sente-router this)]
      (stop-f))

    (assoc this
           :connected-uids nil
           ::sente-router nil))

  SenteCommunicator

  (send! [this uid event-type payload]
    (send-fn uid [event-type payload]))

  (connected-uids [this]
    @connected-uids))


(defn new-senteCommunicator []
  (map->SenteCommunicator* {:path "chsk"}))
