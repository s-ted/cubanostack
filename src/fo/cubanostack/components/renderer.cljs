;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.renderer
  (:require
    [com.stuartsierra.component :as c]
    [cublono-quiescent.interpreter :as cublono]
    [quiescent.core :as q]
    cljsjs.react-bootstrap
    [cubanostack.components.state :as s]
    [cubanostack.components.bus :as bus]
    [cubanostack.templates.ui :as ui]
    [cubanostack.wrapper.core :as w]
    [cubanostack.components.wrapper-manager :as wm]))


(defn- -get-template-by-handle [handle state]
  (get-in state [::templates handle]))

(defn- -render [{:keys [template template-args target-dom state Bus]}]
  (let [template-ui (or
                      (-get-template-by-handle template state)
                      (let [default-template-handle (::default-template state)]
                        (-get-template-by-handle default-template-handle state)))

        ui (-> (template-ui [template-args state] Bus)
               cublono/interpret)]
    (q/render ui target-dom)))

(defn- default-renderer-handler [default-ui-component]
  (fn [{:keys [state Bus] :as response}]
    (assoc response
           :template      :default
           :template-args {:center (default-ui-component state Bus)})))


(defprotocol Renderer
  (add-template [this handle ui-component])
  (set-default-template [this handle])

  (render [this]
          [this opts]
          [this target-dom-id opts]))


(defrecord Renderer* [default-ui-component State WrapperManager Bus]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :renderer :core-renderer
                 (w/handler (fn [opts] (render this opts))))
    (wm/register WrapperManager :renderer/add-template :core-renderer
                 (w/handler (fn [{:keys [handle ui-component]}]
                              (add-template this handle ui-component))))
    (wm/register WrapperManager :renderer/set-default-template :core-renderer
                 (w/handler (fn [{:keys [handle]}]
                              (set-default-template this handle))))
    (add-template this :dashboard ui/DashboardTemplate)
    (add-template this :default ui/DefaultTemplate)
    (set-default-template this :default)
    this)

  (stop [this]
    (wm/unregister WrapperManager :renderer :core-renderer)
    (wm/unregister WrapperManager :renderer/add-template :core-renderer)
    (wm/unregister WrapperManager :renderer/set-default-template :core-renderer)
    (add-template this :default nil)
    (add-template this :dashboard nil)
    (set-default-template this nil)
    this)

  Renderer

  (add-template [this handle ui-component]
    (s/update! State [::templates]
               (fn [v] (assoc v
                              handle
                              ui-component)))
    this)

  (set-default-template [this handle]
    (s/store! State [::default-template] handle)
    this)

  (render [this]
    (render this nil))

  (render [this opts]
    (render this "app" opts))

  (render [this target-dom-id opts]
    (let [target-dom (.getElementById js/document target-dom-id)

          handler    (wm/wrap-with WrapperManager
                                   :render-state
                                   (default-renderer-handler default-ui-component))

          renderable (handler (merge opts
                                     {:Bus        Bus
                                      :state      (s/snapshot State)
                                      :target-dom target-dom}))]
      (-render
        renderable))
    this))

(defn new-renderer [default-ui-component]
  (map->Renderer* {:default-ui-component default-ui-component}))
