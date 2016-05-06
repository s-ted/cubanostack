;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.dashboard.ui
  (:require
    [clojure.string :as str]
    cljs.pprint
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))


(defn- nav! [handler Bus]
  (bus/send! Bus :route! {:handler handler}))

(ui/defcomponent Menu
  [[handler items] Bus]

  [:div {:className "menu"}
   [:ReactBootstrap/Nav {:activeKey handler}

    [:ReactBootstrap/NavItem {:eventKey :dashboard
                              :onClick  (h%
                                          (nav! :dashboard Bus))}
     [:ReactBootstrap/Glyphicon {:glyph :dashboard}]
     "Dashboard"]

    (map (fn [[id {:keys [handler icon label] :as item}]]
           [:ReactBootstrap/NavItem {:key      handler
                                     :eventKey handler
                                     :onClick  (h%
                                                 (nav! handler Bus))}
            [:ReactBootstrap/Glyphicon {:glyph icon}]
            label])
         items)]])


(ui/defcomponent UI
  [state Bus]

  [:ReactBootstrap/Panel {:header "Current State"}])
