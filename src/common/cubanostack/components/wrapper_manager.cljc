;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.wrapper-manager
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.state :as s]
    [cubanostack.wrapper.core :as w]))

(defprotocol WrapperManager
  (register [this workflow wrapper-id wrapper])
  (activate [this workflow wrapper-id])
  (deactivate [this workflow wrapper-id])
  (unregister [this workflow wrapper-id])

  (wrap-with [this workflow]
             [this workflow handler]))


(defrecord WrapperManager* [State]
  c/Lifecycle

  (start [this]
    (when-not (s/retrieve State [:workflows])
      (s/store! State [:workflows] {}))
    (-> this
        (#(register % :wrapper/deactivate ::deactivation
                    (w/handler
                      (fn [{:keys [workflow wrapper-id]}]
                        (deactivate % workflow wrapper-id)))))
        (#(register % :wrapper/activate ::activation
                    (w/handler
                      (fn [{:keys [workflow wrapper-id]}]
                        (activate % workflow wrapper-id)))))))

  (stop [this]
    (-> this
        (unregister :wrapper/activate ::activation)
        (unregister :wrapper/deactivate ::deactivation))
    (s/store! State [:workflows] nil)
    this)

  WrapperManager

  (register [this workflow wrapper-id wrapper]
    (s/store! State [:workflows workflow wrapper-id]
              {:active? true
               :wrapper wrapper})
    this)

  (activate [this workflow wrapper-id]
    (s/store! State [:workflows workflow wrapper-id :active?] true)
    this)

  (deactivate [this workflow wrapper-id]
    (s/store! State [:workflows workflow wrapper-id :active?] false)
    this)

  (unregister [this workflow wrapper-id]
    (s/delete! State [:workflows workflow wrapper-id])
    this)

  (wrap-with [this workflow]
    (wrap-with this workflow identity))

  (wrap-with [this workflow handler]
    (let [middlewares (->> [:workflows workflow]
                           (s/retrieve State)
                           vals
                           (filter :active?)
                           (map :wrapper))]
      (apply w/wrap-with handler middlewares))))

(defn new-wrapperManager []
  (map->WrapperManager* {}))
