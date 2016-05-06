;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.app
  (:require
    [environ.core :as environ]
    [com.stuartsierra.component :refer [system-map using] :as component]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.config :as config]
    [cubanostack.components.handler :as handler]
    [cubanostack.components.middleware :as middleware]
    [cubanostack.components.orientdb :as orientdb]
    [cubanostack.components.router :as router]
    [cubanostack.components.sente :as sente]
    [cubanostack.components.state :as state]
    [cubanostack.components.web-server :as web-server]
    [cubanostack.components.wrapper-manager :as wrapper-manager]
    [cubanostack.sitemap.service :as sitemap-service]
    [cubanostack.user.service :as user-service]
    [cubanostack.user.dal :as user-dal]
    [cubanostack.user.login.service :as user-login-service]
    [cubanostack.server :as server]))


(defn system-prod [http-port]
  (system-map
    :State             (state/new-state)
    :Config            (using
                         (config/new-config)
                         [:State])
    :WrapperManager    (using
                         (wrapper-manager/new-wrapperManager)
                         [:State])
    :Bus               (using
                         (bus/new-bus)
                         [:WrapperManager])

    :GenericDal        (orientdb/new-dal
                         {:store (environ/env :orient-db-store)})

    :Router            (using
                         (router/new-router)
                         [:Bus :State :WrapperManager])
    :Middleware        (middleware/new-middleware
                         {:middleware (server/middlewares)})
    :Handler           (using
                         (handler/new-handler)
                         [:Router :Middleware])
    :WebServer         (using
                         (web-server/new-web-server http-port)
                         [:Handler])

    :SenteCommunicator (using
                         (sente/new-senteCommunicator)
                         [:Bus :WrapperManager
                          :Router])

    :SitemapService    (using
                         (sitemap-service/new-sitemapService)
                         [:Bus :Router])

    :UserDal           (using
                          (user-dal/new-dal)
                          [:GenericDal])
    :UserService       (using
                         (user-service/new-userService)
                         [:Bus :UserDal
                          :Router])
    :UserLoginService  (using
                          (user-login-service/new-userLoginService)
                          [:Bus :UserDal
                           :Router])))

(defn system-dev [& components]
  (apply system-map
         :State             (state/new-state)
         :Config            (using
                              (config/new-config)
                              [:State])
         :WrapperManager    (using
                              (wrapper-manager/new-wrapperManager)
                              [:State])
         :Bus               (using
                              (bus/new-bus)
                              [:WrapperManager])

         :GenericDal       (orientdb/new-dal
                              {:store (environ/env :orient-db-store)})

         :Router            (using
                              (router/new-router)
                              [:Bus :State :WrapperManager])
         :Middleware        (middleware/new-middleware
                              {:middleware (server/middlewares)})
         :Handler           (using
                              (handler/new-handler)
                              [:Router :Middleware])

         :SenteCommunicator (using
                              (sente/new-senteCommunicator)
                              [:Bus :WrapperManager
                               :Router])

         :SitemapService    (using
                              (sitemap-service/new-sitemapService)
                              [:Bus :Router])

         :UserDal           (using
                               (user-dal/new-dal)
                               [:GenericDal])
         :UserService       (using
                              (user-service/new-userService)
                              [:Bus :UserDal
                               :Router])
         :UserLoginService  (using
                               (user-login-service/new-userLoginService)
                               [:Bus :UserDal
                                :Router])

         components))

(defn system-test [config & components]
  (apply system-map
         :State             (state/new-state)
         :Config            (using
                              (config/new-config config)
                              [:State])
         :WrapperManager    (using
                              (wrapper-manager/new-wrapperManager)
                              [:State])
         :Bus               (using
                              (bus/new-bus)
                              [:WrapperManager])

         :GenericDal       (orientdb/new-dal
                              {:store "memory:test"
                               :empty-at-start? true})

         :Router            (using
                              (router/new-router)
                              [:Bus :State :WrapperManager])
         :Middleware        (middleware/new-middleware
                              {:middleware (server/middlewares)})
         :Handler           (using
                              (handler/new-handler)
                              [:Router :Middleware])

         :SenteCommunicator (using
                              (sente/new-senteCommunicator)
                              [:Bus :WrapperManager
                               :Router])

         :SitemapService    (using
                              (sitemap-service/new-sitemapService)
                              [:Bus :Router])

         :UserDal           (using
                              (user-dal/new-dal)
                              [:GenericDal])
         :UserService       (using
                              (user-service/new-userService)
                              [:Bus :UserDal
                               :Router])
         :UserLoginService  (using
                              (user-login-service/new-userLoginService)
                              [:Bus :UserDal
                               :Router])

         components))
