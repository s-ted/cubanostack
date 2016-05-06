;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.settings.wrapper.module
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.settings.wrapper.ui :as ui]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.wrapper.core :as w]))


(defrecord Renderer [route]
  w/Wrapper

  (before [this payload]
    payload)

  (after [this response {:keys [state Bus] :as payload}]
    (if (= route
           (get-in state [:route-info :handler]))
      (-> response
          (assoc :template :dashboard)
          (assoc-in [:template-args :center] (ui/UI state Bus)))
      response)))

(defrecord Module [id route ->Wrapper Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :render-state id
                 (->Wrapper id))
    (doto Bus
      (bus/send! :dashboard/add-item {:id      ::wrapper-settings
                                      :label   "Wrappers"
                                      :handler id
                                      :icon    :tasks})

      (bus/send! :router/add-route {:route   route
                                    :handler id}))
    this)

  (stop [this]
    (doto
      (bus/send! :dashboard/remove-item {:id ::wrapper-settings})
      (bus/send! :router/remove-route {:route route}))
    (wm/unregister WrapperManager :render-state id)
    this))

(defn new-wrapperSetting
  ([]
   (new-wrapperSetting ["settings" "/wrapper"]))

  ([route]
    (map->Module {:id :settings/wrapper :route route :->Wrapper ->Renderer})))
