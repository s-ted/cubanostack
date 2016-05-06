;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.login.module
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [com.stuartsierra.component :as c]
    [cljs.core.async :as async]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.router :as router]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.rest :as rest]
    [cubanostack.user.login.ui :as ui]
    [cubanostack.wrapper.core :as w]
    [cubanostack.local-storage :as lstorage]))


(defn -set-current-user-info [info Bus]
  (lstorage/set-item-in-local-storage! :current-user info)
  (bus/send! Bus :renderer))


(defn- -login! [{:keys [entity]} Bus UserLoginRest]
  (go
    (-> (rest/create! UserLoginRest entity)
        async/<!
        (-set-current-user-info Bus))))



(defrecord Renderer [route]
  w/Wrapper

  (before [this payload]
    payload)

  (after [this response {:keys [route-changed? state Bus]}]
    (if (= route
           (get-in state [:route-info :handler]))
      (assoc-in response
                [:template-args :center] (ui/UI state Bus))
      response)))

(defrecord Module [id route ->Wrapper Bus WrapperManager UserLoginRest]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :user/login! id
                 (w/handler #(-login! % Bus UserLoginRest)))
    (wm/register WrapperManager :render-state id
                 (->Wrapper id UserLoginRest))
    (bus/send! Bus :router/add-route {:route   route
                                      :handler id})
    this)

  (stop [this]
    (bus/send! Bus :router/remove-route {:route route})
    (wm/unregister WrapperManager :render-state id)
    (wm/unregister WrapperManager :user/login! id)
    this))



(defn new-userLoginModule
  ([]
   (new-userLoginModule ["login"]))

  ([route]
   (map->Module {:id :user/login :route route :->Wrapper ->Renderer})))
