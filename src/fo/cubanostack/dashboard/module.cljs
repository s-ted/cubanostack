;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.dashboard.module
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.dashboard.ui :as ui]
    [cubanostack.wrapper.core :as w]))


(defrecord Renderer [route]
  w/Wrapper

  (before [this payload]
    payload)

  (after [this response {:keys [state Bus]}]
    (if (= route
           (get-in state [:route-info :handler]))
      (-> response
          (assoc-in [:template] :dashboard)
          (assoc-in [:template-args :center] (ui/UI state Bus)))
      response)))


(defprotocol Dashboard
  (add-item [this item])
  (remove-item [this id]))

(defrecord Module [id route ->Wrapper Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :dashboard/add-item ::add-item
                 (w/handler #(add-item this %)))
    (wm/register WrapperManager :dashboard/remove-item ::remove-item
                 (w/handler #(remove-item this (:id %))))
    (wm/register WrapperManager :render-state id
                 (->Wrapper id))
    (bus/send! Bus :router/add-route {:route   route
                                      :handler id})
    this)

  (stop [this]
    (bus/send! Bus :router/remove-route {:route route})
    (wm/unregister WrapperManager :render-state id)
    (wm/unregister WrapperManager :dashboard/remove-item ::remove-item)
    (wm/unregister WrapperManager :dashboard/add-item ::add-item)
    this)

  Dashboard

  (add-item [this {:keys [id] :as item}]
    (bus/send! Bus :state/store! {:id-path [:dashboard/items id]
                                  :value   (select-keys item [:icon :label :handler])})
    this)

  (remove-item [this id]
    (bus/send! Bus :state/delete! {:id-path [:dashboard/items id]})
    this))

(defn new-dashboard
  ([]
   (new-dashboard ["dashboard"]))

  ([route]
    (map->Module {:id :dashboard :route route :->Wrapper ->Renderer})))
