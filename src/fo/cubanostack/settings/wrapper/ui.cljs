;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.settings.wrapper.ui
  (:require
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))


(defn- -deactivate! [workflow wrapper-id Bus]
  (doto Bus
    (bus/send! :wrapper/deactivate {:workflow   workflow
                                    :wrapper-id wrapper-id})
    (bus/send! :renderer)))

(defn- -activate! [workflow wrapper-id Bus]
  (doto Bus
    (bus/send! :wrapper/activate {:workflow   workflow
                                  :wrapper-id wrapper-id})
    (bus/send! :renderer)))

(defn -panel-content [[wrapper-id wrapper-info] workflow Bus]
  [:ReactBootstrap/ListGroupItem {:key (str wrapper-id)}

   [:ReactBootstrap/ButtonGroup {:bsClass "pull-right"
                                 :bsSize  :small}

    (if (:active? wrapper-info)
      [:ReactBootstrap/Button {:bsStyle :warning
                               :onClick (h% (-deactivate! workflow wrapper-id Bus))}
       [:ReactBootstrap/Glyphicon {:glyph :ban-circle}]
       "Deactivate"]

      [:ReactBootstrap/Button {:bsStyle :success
                               :onClick (h% (-activate! workflow wrapper-id Bus))}
       [:ReactBootstrap/Glyphicon {:glyph :ok-circle}]
       "Activate"])]
   (name wrapper-id)])

(defn- -panelize [[workflow wrappers] Bus]
  [:ReactBootstrap/Panel {:key    (str workflow)
                          :header (str "Workflow " workflow)}
   [:ReactBootstrap/ListGroup nil
    (map #(-panel-content % workflow Bus)
         wrappers)]])

(ui/defcomponent UI
  [{:keys [workflows]} Bus]

  [:div nil
   (map #(-panelize % Bus)
        workflows)])
