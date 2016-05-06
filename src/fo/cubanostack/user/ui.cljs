;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.ui
  (:require
    [clojure.string :as str]
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))



(def item-state-prefix [::local-user])
(def list-state-prefix [:listing])

(defn- set-local-item [key-path v Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path (concat item-state-prefix key-path)
                              :value   v})
    (bus/send! :renderer)))

(defn- set-local-item-role [role v Bus]
  (doto Bus
    (bus/send! :state/update! {:id-path (concat item-state-prefix [:roles])
                               :f
                               #(if v
                                  (distinct (conj % role))
                                  (remove (fn [r] (= role r)) %))})
    (bus/send! :renderer)))

(defn- -delete! [id Bus]
  (doto Bus
    (bus/send! :user/delete! {:id id})
    (bus/send! :renderer)))

(defn- create-item [entity Bus]
  (doto Bus
    (bus/send! :user/create! {:entity entity})
    (bus/send! :state/delete! {:id-path item-state-prefix})
    (bus/send! :renderer)))

(defn- refresh-list [Bus]
  (bus/send! Bus :user/refresh-list))

(ui/defcomponent Item
  :keyfn :_id
  [{:keys [_id username roles createdAt]}
   Bus]

  [:ReactBootstrap/ListGroupItem
   [:.pull-right
    [:.muted _id]

    [:ReactBootstrap/Button {:bsStyle :danger
                             :onClick (h% (-delete! _id Bus))}
     [:ReactBootstrap/Glyphicon {:glyph :ban-circle}]
     "Delete"]]
   [:p username " " (str/join "," roles) " " createdAt]])

(ui/defcomponent Listing
  [items Bus]

  [:ReactBootstrap/ListGroup nil
   (map #(Item % Bus)
        items)])

(ui/defcomponent NewForm
  [local-item Bus]

  [:ReactBootstrap/Panel nil
   [:form {:className "form-horizontal"}
    [:ReactBootstrap/Input
     {:type             "text"
      :value            (:username local-item)
      :placeholder      "jdoe"
      :label            "Username"
      :labelClassName   "col-xs-2"
      :wrapperClassName "col-xs-10"
      :onChange
      (h%
        (set-local-item
          [:username]
          (-> event
              .-target
              .-value)
          Bus))}]

    [:ReactBootstrap/Input
     {:type             "text"
      :value            (:clear-password local-item)
      :placeholder      "mysecret"
      :label            "Password"
      :labelClassName   "col-xs-2"
      :wrapperClassName "col-xs-10"
      :onChange
      (h%
        (set-local-item
          [:clear-password]
          (-> event
              .-target
              .-value)
          Bus))}]

    (map
      (fn [[k v]]
        [:ReactBootstrap/Input
         {:type      "checkbox"
          :checked   (k (:roles local-item))
          :label     v
          :key       k
          :wrapperClassName "col-xs-offset-2 col-xs-10"
          :onChange
          (h%
            (set-local-item-role
              k
              (-> event
                  .-target
                  .-checked)
              Bus))}])
      {:admin "Admin"
       :user  "User"
       :guest "Guest"})

    [:ReactBootstrap/Button
     {:bsStyle   "primary"
      :type      "submit"
      :className "col-xs-offset-2"
      :onClick
      (h%
        (.preventDefault event)
        (create-item
          local-item
          Bus))}
     [:ReactBootstrap/Glyphicon {:glyph :plus}]
     " Create"]]])

(ui/defcomponent UI
  [[listing item]
   Bus]

  [:div nil

   (NewForm item Bus)

   [:ReactBootstrap/Button
    {:bsStyle   "default"
     :onClick
     (h%
       (refresh-list Bus))}
    [:ReactBootstrap/Glyphicon {:glyph :refresh}]
    " Refresh"]

   (Listing listing Bus)])
