;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.system
  "This is the main namespace for the **cubane FO** (cl)JS system declaration"

  (:require
    [com.stuartsierra.component :refer [system-map using] :as component]
    [cubanostack.app.ui :as ui]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.config :as config]
    [cubanostack.components.renderer :as renderer]
    [cubanostack.components.router :as router]
    [cubanostack.components.sente :as sente]
    [cubanostack.components.state :as state]
    [cubanostack.components.wrapper-manager :as wrapper-manager]
    [cubanostack.dashboard.module :as dashboard]
    [cubanostack.notification.module :as notification]
    [cubanostack.settings.wrapper.module :as setting-wrapper]
    [cubanostack.settings.router.module :as setting-router]
    [cubanostack.user.login.module :as user-login-module]
    [cubanostack.user.login.rest :as user-login-rest]
    [cubanostack.user.module :as user-module]
    [cubanostack.user.rest :as user-rest]))


(defn system-definition []
  (system-map
    :State  (state/new-state
              {:text "Hello Cubane!"
               :notifications {(gensym) {:content "Bienvenu sur Cubane!"
                                         :action  #(js/console.debug "action!")
                                         :label   "Do it"}}})
    :Config (using
               (config/new-config)
               [:State])
    :WrapperManager (using
                      (wrapper-manager/new-wrapperManager)
                      [:State])
    :Bus      (using
                (bus/new-bus)
                [:WrapperManager])
    :Renderer (using
                (renderer/new-renderer ui/NotFoundComponent)
                [:WrapperManager :State :Bus])
    :Router   (using
                (router/new-router)
                [:State :Bus :WrapperManager])
    :Sente    (using
                (sente/new-sente)
                [:Bus :WrapperManager])
    :NotificationManager (using
                           (notification/new-notification)
                           [:Bus :WrapperManager])
    :Dashboard    (using
                    (dashboard/new-dashboard)
                    [:Bus :WrapperManager
                     :Router])
    :WrapperSetting (using
                      (setting-wrapper/new-wrapperSetting)
                      [:Bus :WrapperManager
                       :Dashboard])
    :RouterSetting (using
                     (setting-router/new-routerSetting)
                     [:Bus :WrapperManager
                      :Dashboard])


    :UserRest      (using
                     (user-rest/new-rest)
                     [:Bus :State])
    :UserModule    (using
                     (user-module/new-userModule)
                     [:Bus :WrapperManager :UserRest
                      :Router])

    :UserLoginRest   (using
                       (user-login-rest/new-rest)
                       [:Bus :State])
    :UserLoginModule (using
                       (user-login-module/new-userLoginModule)
                       [:Bus :WrapperManager :UserLoginRest
                        :Router])))
