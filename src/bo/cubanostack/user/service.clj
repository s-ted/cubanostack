;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.service
  (:require
    [com.stuartsierra.component :as c]
    [liberator.core :refer [defresource]]
    [schema.coerce :as coerce]
    [schema-tools.core :as schema]
    [buddy.hashers :as hashers]
    [cubanostack.auth :as auth]
    [cubanostack.user.model :as model]
    [cubanostack.components.bus :as bus]
    [cubanostack.crud :as crud]
    [cubanostack.dal :as dal]))





(defn map->entity [patch? m]
  (-> (if-let [clear-password (:clear-password m)]
        (-> m
            (dissoc :clear-password)
            (assoc :password (hashers/encrypt clear-password)))

        ; else
        m)
      (update :roles distinct)
      (schema/select-schema
        model/Persisted
        coerce/json-coercion-matcher)))

(defn entity->map [e]
  (-> e
      (schema/select-schema
        model/PrivateRead
        coerce/json-coercion-matcher)))

(defn- ctx->entity-id [ctx]
  (get-in ctx [:request :route-params :id]))


(defresource list-resource [UserDal]
  (crud/default-collection-liberator-config
    map->entity
    entity->map
    UserDal)

  :authorized? auth/authorized?
  :allowed?    (auth/any-granted? #{:admin}))

(defresource entity-resource [UserDal]
  (crud/default-item-liberator-config
    map->entity
    entity->map
    ctx->entity-id
    UserDal))



(defrecord Service* [route-path Bus UserDal]
  c/Lifecycle

  (start [this]
    (doto Bus
      (bus/send! :router/add-route {:route   (conj route-path "")
                                    :handler (list-resource UserDal)})
      (bus/send! :router/add-route {:route   (conj route-path [:id ""])
                                    :handler (entity-resource UserDal)}))
    this)

  (stop [this]
    (doto Bus
      (bus/send! :router/remove-route {:route (conj route-path [:id ""])})
      (bus/send! :router/remove-route {:route (conj route-path "")}))
    this))


(defn new-userService []
  (map->Service* {:route-path ["app/" "user/"]}))
