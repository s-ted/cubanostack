;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.login.service
  (:require [com.stuartsierra.component :as c]
            [liberator.core :refer [defresource]]
            [schema.coerce :as coerce]
            [schema-tools.core :as schema]
            [buddy.hashers :as hashers]
            [cubanostack.user.model :as model]
            [cubanostack.components.bus :as bus]
            [cubanostack.crud :as crud]
            [cubanostack.auth :as auth]
            [cubanostack.dal :as dal]))





(defn map->entity [patch? m]
  (-> m
      (schema/select-schema
        model/LoginWrite
        coerce/json-coercion-matcher)))

(defn entity->map [e]
  (-> e
      (schema/select-schema
        model/LoginRead
        coerce/json-coercion-matcher)))


(defresource list-resource [UserDal]
  (crud/default-collection-liberator-config
    map->entity
    entity->map
    UserDal)

  :allowed-methods [:post :options]
  :authorized?     (fn [ctx]
                     (let [{:keys [username password]} (:data ctx)
                           users (dal/find-all-deep UserDal {:username username})
                           user  (first users)]
                       [(hashers/check password (:password user)) {:user user}]))
  :post!           nil
  :handle-created  (fn [ctx]
                     (-> ctx :user
                         (dissoc :password)
                         (#(assoc % :token (auth/user->token %)))
                         entity->map)))



(defrecord Service* [route-path Bus UserDal]
  c/Lifecycle

  (start [this]
    (doto Bus
      (bus/send! :router/add-route {:route   (conj route-path "")
                                    :handler (list-resource UserDal)}))
    this)

  (stop [this]
    (doto Bus
      (bus/send! :router/remove-route {:route (conj route-path "")}))
    this))


(defn new-userLoginService []
  (map->Service* {:route-path ["app/" "user/" "login/"]}))
