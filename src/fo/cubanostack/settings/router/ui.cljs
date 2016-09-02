;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.settings.router.ui
  (:require
    [clojure.string :as str]
    [clojure.zip :as zip]
    [cubanostack.helper.ui :as ui :refer-macros [defcomponent h%]]
    [cubanostack.components.bus :as bus]))

(defn- nav! [path Bus]
  (doto Bus
    (bus/send! :route-path! {:path path})
    (bus/send! :renderer)))

(defn- keys-in [m]
  (letfn [(branch? [[path m]]
            (map? m))

          (children [[path m]]
            (for [[k v] m]
              [(conj path k) v]))]
    (if (empty? m)
      []
      (loop [t     (zip/zipper branch? children nil [[] m])
             paths []]
        (cond
          (zip/end? t)    paths
          (zip/branch? t) (recur (zip/next t), paths)
          :leaf           (recur (zip/next t)
                                 (conj paths (first (zip/node t)))))))))


(defn- set-local-new-route [value Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path [::new-routes :route] :value value})
    (bus/send! :renderer)))

(defn- set-local-new-route-handler [value Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path [::new-routes :handler] :value value})
    (bus/send! :renderer)))


(defn- add-new-route! [{:keys [route handler]} Bus]
  (let [exploded-route (remove str/blank? (str/split route #"/"))

        keywordized-handler (keyword handler)]
    (doto Bus
      (bus/send! :router/add-route {:route   exploded-route
                                    :handler keywordized-handler})
      (bus/send! :renderer))))

(defn- set-local-route-val [value route Bus]
  (doto Bus
    (bus/send! :state/store! {:id-path (cons ::routes route)
                              :value   value})
    (bus/send! :renderer)))

(defn- delete-route! [route Bus]
  (doto Bus
    (bus/send! :router/remove-route {:route route})
    (bus/send! :renderer)))

(defn- update-route! [route Bus state]
  (let [handler (-> state
                    (get-in (cons ::routes route))
                    keyword)]
    (doto Bus
      (bus/send! :router/add-route {:route   route
                                    :handler handler})
      (bus/send! :renderer))))

(defn -itemize [route routes state Bus]
  (let [route-str           (str "/" (str/join route))
        current-local-route (or
                              (get-in state (cons ::routes route))
                              (when-let [kw (get-in state (cons :routes route))]
                                (str
                                  (when-let [ns (namespace kw)]
                                    (str ns "/"))
                                  (name kw))))]
    [:ReactBootstrap/ListGroupItem {:key route-str}

     [:ReactBootstrap/ButtonGroup {:bsClass "pull-right"
                                  :bsSize  :small}

      [:ReactBootstrap/Button {:bsStyle :info
                               :onClick (h% (nav! route-str Bus))}
       [:ReactBootstrap/Glyphicon {:glyph :link}]
       "Browse"]

      (when-not (= (keyword current-local-route)
                   (get-in state (cons :routes route)))
        [:ReactBootstrap/Button {:bsStyle :warning
                                 :onClick (h% (update-route! route Bus state))}
         [:ReactBootstrap/Glyphicon {:glyph :asterisk}]
         "Update"])

      [:ReactBootstrap/Button {:bsStyle :danger
                               :onClick (h% (delete-route! route Bus))}
       [:ReactBootstrap/Glyphicon {:glyph :remove}]
       "Delete"]]

     [:form {:className "form-horizontal"}
      [:ReactBootstrap/FormGroup {:controlId (str "local-route-" route-str)}
       [:ReactBootstrap/ControlLabel route-str]
       [:ReactBootstrap/FormControl
        {:type             "text"
         :value            current-local-route
         :placeholder      "The handler (such as 'home' or 'settings/stuff') [will be keywordized]"
         :onChange
         (h%
           (set-local-route-val
             (-> event
                 .-target
                 .-value)
             route
             Bus))}]]]]))

(ui/defcomponent UI
  [{:keys [routes] :as state} Bus]


  [:ReactBootstrap/ListGroup nil

   (map #(-itemize % routes state Bus)
        (keys-in routes))

   [:ReactBootstrap/ListGroupItem {:key ::new-route}

    [:ReactBootstrap/ButtonGroup {:bsClass "pull-right"
                                  :bsSize  :small}

     [:ReactBootstrap/Button {:bsStyle :success
                              :onClick (h% (add-new-route! (::new-routes state) Bus))}
      [:ReactBootstrap/Glyphicon {:glyph :plus}]
      "Add"]]

    [:form {:className "form-horizontal"}
     [:ReactBootstrap/Row nil
      [:ReactBootstrap/Col {:md 4}
       [:ReactBootstrap/FormGroup nil
        [:ReactBootstrap/FormControl
         {:type        "text"
          :value       (get-in state [::new-routes :route])
          :placeholder "/my/new/route"
          :onChange
          (h%
            (.preventDefault event)
            (set-local-new-route
              (-> event
                  .-target
                  .-value)
              Bus))}]]]
      [:ReactBootstrap/Col {:md 4}
       [:ReactBootstrap/FormGroup nil
        [:ReactBootstrap/FormControl
        {:type        "text"
         :value       (get-in state [::new-routes :handler])
         :placeholder "The handler (such as 'home' or 'settings/stuff') [will be keywordized]"
         :onChange
         (h%
           (set-local-new-route-handler
             (-> event
                 .-target
                 .-value)
             Bus))}]]]]]]])
