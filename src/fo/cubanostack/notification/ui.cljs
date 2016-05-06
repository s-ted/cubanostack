;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.notification.ui
  (:require
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))

(defn- -show-notification! [state Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path [:ui-options :show-notifications?]
                              :value   state})
    (bus/send! :renderer)))

(defn- -dissmiss-all-notifications! [Bus]
  (-show-notification! false Bus)
  (doto Bus
    (bus/send! :notification/remove-all)
    (bus/send! :renderer)))

(defn- -dissmiss-notification! [id Bus]
  (doto Bus
    (bus/send! :notification/remove {:id id})
    (bus/send! :renderer)))


(ui/defcomponent UI
  [[ui-options notifications] Bus]

  (let [show-notifications? (:show-notifications? ui-options)
        notifications       (reverse notifications)]
    [:ReactBootstrap/NavItem {:onClick (h% (-show-notification! (not show-notifications?) Bus))}
     [:ReactBootstrap/Glyphicon {:glyph :bell}]
     (when notifications
       [:ReactBootstrap/Badge nil (count notifications)])
     (when (and (not (empty? notifications))
                show-notifications?)
       [:ReactBootstrap/Popover {:id           "notifications"
                                 :placement    :bottom
                                 :positionTop  44
                                 :positionLeft -116}
        [:ul.list-group
         (map (fn [[id {:keys [content action label]
                        :or {label "Action"}}]]
                [:li.list-group-item {:key id}
                 [:ReactBootstrap/Row
                  [:ReactBootstrap/Col {:xs 12} content]]
                 [:ReactBootstrap/Row
                  [:ReactBootstrap/Col.text-right {:xs 12}
                   [:ReactBootstrap/ButtonGroup
                    (when action
                      [:ReactBootstrap/Button {:bsStyle :primary
                                               :onClick (h%
                                                          (.preventDefault event)
                                                          (.stopPropagation event)
                                                          (action))}
                       label])
                    [:ReactBootstrap/Button {:bsStyle :default
                                             :onClick (h%
                                                        (.preventDefault event)
                                                        (.stopPropagation event)
                                                        (-dissmiss-notification! id Bus))}
                     "Dismiss"]]]]])
              notifications)
         [:ReactBootstrap/Button.pull-right {:bsStyle :default
                                             :onClick (h%
                                                        (.preventDefault event)
                                                        (.stopPropagation event)
                                                        (-dissmiss-all-notifications! Bus))}
          [:ReactBootstrap/Glyphicon {:glyph :fullscreen}]]]])]))
