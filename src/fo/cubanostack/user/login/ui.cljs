;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.login.ui
  (:require
    [clojure.string :as str]
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))



(def ^:private item-state-prefix [::local-user])

(defn- -set-local-item [key-path v Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path (concat item-state-prefix key-path)
                              :value   v})
    (bus/send! :renderer)))

(defn- -login! [entity Bus]
  (doto Bus
    (bus/send! :user/login! {:entity entity})
    (bus/send! :state/delete! {:id-path item-state-prefix})
    (bus/send! :renderer)))

(ui/defcomponent UI
  [state
   Bus]

  (let [local-item (get-in state item-state-prefix)]

[:.login_box
  [:h1 "Sign In"]

  [:form.form-horizontal

    [:ReactBootstrap/FormGroup nil
     [:ReactBootstrap/InputGroup nil
      [:ReactBootstrap/InputGroup.Addon nil
       (ui/icon :user)]

      [:ReactBootstrap/FormControl
       {:type             "text"
        :value            (:username local-item)
        :onChange
        (h%
          (-set-local-item
            [:username]
            (-> event
                .-target
                .-value)
            Bus))}]]]

    [:ReactBootstrap/FormGroup nil
     [:ReactBootstrap/InputGroup nil
      [:ReactBootstrap/InputGroup.Addon nil
       (ui/icon :lock)]

      [:ReactBootstrap/FormControl
       {:type             "password"
        :value            (:password local-item)
        :onChange
        (h%
          (-set-local-item
            [:password]
            (-> event
                .-target
                .-value)
            Bus))}]]]

    [:.form-group
      [:ReactBootstrap/Button
       {:bsStyle   "primary"
        :type      "submit"
        :className "pull-right"
        :onClick
        (h%
          (.preventDefault event)
          (-login!
            local-item
            Bus))}
       [:ReactBootstrap/Glyphicon {:glyph :arrow-right}]
       " Sign In"]]]]))
