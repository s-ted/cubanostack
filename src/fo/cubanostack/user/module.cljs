;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.module
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [com.stuartsierra.component :as c]
    [cljs.core.async :as async]
    [cubanostack.helper.listing :as listing]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.router :as router]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.rest :as rest]
    [cubanostack.user.ui :as ui]
    [cubanostack.wrapper.core :as w]))



(defn- -create! [{:keys [entity]} Bus UserRest]
  (go
    (async/<! (rest/create! UserRest entity))
    (listing/refresh-list Bus UserRest)))

(defn- -delete! [{:keys [id]} Bus UserRest]
  (go
    (async/<! (rest/delete! UserRest id))
    (listing/refresh-list Bus UserRest)))





(defrecord Renderer [route UserRest]
  w/Wrapper

  (before [this payload]
    payload)

  (after [this response {:keys [route-changed? state Bus]}]
    (if (= route
           (get-in state [:route-info :handler]))
      (do
        (when route-changed?
          (bus/send! Bus :user/refresh-list))
        (assoc-in response
                  [:template-args :center]
                  (ui/UI
                    [(get-in state ui/list-state-prefix)
                     (get-in state ui/item-state-prefix)]
                    Bus)))
      response)))

(defrecord Module [id route ->Wrapper Bus WrapperManager UserRest]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :user/refresh-list id
                 (w/handler (fn [_] (listing/refresh-list Bus UserRest))))
    (wm/register WrapperManager :user/create! id
                 (w/handler #(-create! % Bus UserRest)))
    (wm/register WrapperManager :user/delete! id
                 (w/handler #(-delete! % Bus UserRest)))
    (wm/register WrapperManager :render-state id
                 (->Wrapper id UserRest))
    (bus/send! Bus :router/add-route {:route   route
                                      :handler id})
    this)

  (stop [this]
    (bus/send! Bus :router/remove-route {:route route})
    (wm/unregister WrapperManager :render-state id)
    (wm/unregister WrapperManager :user/delete! id)
    (wm/unregister WrapperManager :user/create! id)
    (wm/unregister WrapperManager :user/refresh-list id)
    this))



(defn new-userModule
  ([]
   (new-userModule ["user"]))

  ([route]
   (map->Module {:id ::user :route route :->Wrapper ->Renderer})))
