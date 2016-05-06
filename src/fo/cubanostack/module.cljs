;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.module
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.wrapper.core :as w]))


(defrecord Renderer [route payload->Ui]
  w/Wrapper

  (before [this payload]
    payload)

  (after [this response {:keys [state] :as payload}]
    (if (= route
           (get-in state [:route-info :handler]))
      (assoc-in response
                [:template-args :center] (payload->Ui payload))
      response)))


(defrecord Module [id route ->Wrapper WrapperManager Bus]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :render-state id
                 (->Wrapper id))
    (bus/send! Bus :router/add-route {:route   route
                                      :handler id})
    this)

  (stop [this]
    (bus/send! Bus :router/remove-route {:route route})
    (wm/unregister WrapperManager :render-state id)
    this))




(defn Wrapper->module [id route ->Wrapper]
  (map->Module {:id        id
                :route     route
                :->Wrapper ->Wrapper}))

(defn Ui->module [id route payload->Ui]
  (Wrapper->module id
                   route
                   #(map->Renderer {:route       %
                                    :payload->Ui payload->Ui})))


(defn ^:deprecated
  new-module
  [id route ->Wrapper]

  (Wrapper->module id route ->Wrapper))
