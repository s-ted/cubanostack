;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.notification.module
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.bus :as bus]
    [cubanostack.components.wrapper-manager :as wm]
    [cubanostack.wrapper.core :as w]))


(defprotocol NotificationManager
  (add-notification [this notification])
  (remove-all-notifications [this])
  (remove-notification [this id]))

(defrecord Module [Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (wm/register WrapperManager :notification/add ::add-item
                 (w/handler #(add-notification this %)))
    (wm/register WrapperManager :notification/remove ::remove-item
                 (w/handler #(remove-notification this (:id %))))
    (wm/register WrapperManager :notification/remove-all ::remove-all
                 (w/handler #(remove-all-notifications this)))
    this)

  (stop [this]
    (wm/unregister WrapperManager :notification/remove-all ::remove-all)
    (wm/unregister WrapperManager :notification/remove ::remove-item)
    (wm/unregister WrapperManager :notification/add ::add-item)
    this)

  NotificationManager

  (add-notification [this {:keys [id]
                           :or   {id (gensym)}
                           :as notification}]
    (doto Bus
      (bus/send! :state/store! {:id-path [:notifications id]
                                :value   notification})
      (bus/send! :state/store! {:id-path [:ui-options :show-notifications?]
                                :value   true})
      (bus/send! :renderer))
    this)

  (remove-notification [this id]
    (bus/send! Bus :state/delete! {:id-path [:notifications id]})
    this)

  (remove-all-notifications [this]
    (doto Bus
      (bus/send! :state/delete! {:id-path [:notifications]})
      (bus/send! :state/store! {:id-path [:ui-options :show-notifications?]
                                :value   false})
      (bus/send! :renderer))
    this))

(defn new-notification
  ([]
    (map->Module {})))
