;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.nav.ui
  (:require
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))


(defn- nav! [handler Bus]
  (bus/send! Bus :route! {:handler handler}))

(ui/defcomponent Nav
  [route-info Bus]

  [:ReactBootstrap/Nav {:bsStyle   :tabs
                        :activeKey (:handler route-info)}
   [:ReactBootstrap/NavItem {:eventKey :home
                             :onClick (h% (nav! :home Bus))} "Home"]
   [:ReactBootstrap/NavItem {:eventKey :cubanostack.user.module/user
                             :onClick (h% (nav! :cubanostack.user.module/user Bus))} "User"]])
