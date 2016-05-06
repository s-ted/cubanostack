;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.router
  (:require
    [com.stuartsierra.component :as c]
    [bidi.bidi :as bidi]
    [cubanostack.components.bus :as b]
    [cubanostack.components.state :as s]
    [cubanostack.wrapper.core :as w]
    [cubanostack.components.wrapper-manager :as wm]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import goog.history.Html5History))


(defn- deep-merge* [& maps]
  (let [f (fn [old new]
             (if (and (map? old) (map? new))
                 (merge-with deep-merge* old new)
                 new))]
    (if (every? map? maps)
      (apply merge-with f maps)
     (last maps))))

(defn- deep-merge [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
   (apply merge-with deep-merge* maps)))


(defn- -match-route-handler [routes]
  (let [routes ["/" routes]]
    (fn [{:keys [path]}]
      (bidi/match-route routes path))))

(defn- -path-for-handler [routes]
  (let [routes ["/" routes]]
    (fn [{:keys [handler route-params]}]
      (apply bidi/path-for routes handler (flatten (seq route-params))))))




(defn- get-token []
  (str js/window.location.pathname js/window.location.search))

(defn- handle-url-change [e Bus]
  (when-not (.-isNavigation e)
    ; Token set programmatically => simulating user navigation by scrolling to top left corner
    (js/window.scrollTo 0 0))

  ;; dispatch on the token
  (b/send! Bus :route-path!
           {:path (aget e "token")}))

(defprotocol Router
  (match-route [this path])
  (path-for [this route]))

(defprotocol MasterRouter
  (route! [this handler])
  (route-path! [this path])
  (add-routes [this routes])
  (remove-route [this path]))


(defrecord Router* [initial-routes State Bus WrapperManager]
  c/Lifecycle

  (start [this]
    (s/store! State [:routes] initial-routes)
    (let [new-this
          (-> this
              (assoc ::history
                    (doto (Html5History.)
                      (.setPathPrefix (str js/window.location.protocol
                                           "//"
                                           js/window.location.host))
                      (.setUseFragment false)

                      (goog.events/listen EventType/NAVIGATE
                                          #(handle-url-change % Bus))
                      (.setEnabled true))))]
      (wm/register WrapperManager :route! :core-router
                   (w/handler #(route! new-this %)))
      (wm/register WrapperManager :route-path! :core-router
                   (w/handler #(route-path! new-this (:path %))))
      (wm/register WrapperManager :router/add-route ::add-route
                   (w/handler
                     (fn [{:keys [route handler]}]
                       (add-routes new-this (assoc-in {} (conj route "") handler)))))
      (wm/register WrapperManager :router/remove-route ::remove-route
                   (w/handler
                     (fn [{:keys [route]}]
                       (remove-route new-this route))))
      new-this))

  (stop [this]
    (wm/unregister WrapperManager :router/remove-route ::remove-route)
    (wm/unregister WrapperManager :router/add-route ::add-route)
    (wm/unregister WrapperManager :route-path! :core-router)
    (wm/unregister WrapperManager :route! :core-router)

    (s/delete! State [:routes])

    (doto (::history this)
      (.setEnabled false))

    (-> this
        (assoc ::history nil)))

  MasterRouter

  (route-path! [this path]
    (route! this (match-route this path)))

  (route! [this route]
    (let [history        (::history this)
          token          (get-token)
          path           (path-for this route)
          route-changed? (not= path token)]
      (when route-changed?
        (.setToken (::history this) path))
      (s/store! State [:route-info] route)
      (b/send! Bus :renderer {:route-changed? route-changed?})
      this))

  (add-routes [this routes]
    (s/update! State [:routes] #(deep-merge % routes))
    this)

  (remove-route [this path]
    (s/delete! State (cons :routes path))
    this)

  Router

  (match-route [this path]
    (let [handler (wm/wrap-with WrapperManager
                                :router/match-route
                                (-match-route-handler
                                  (s/retrieve State [:routes])))]
      (handler {:path path})))

  (path-for [this route]
    (let [handler (wm/wrap-with WrapperManager
                                :router/path-for
                                (-path-for-handler
                                  (s/retrieve State [:routes])))]
      (handler route))))

(defn new-router []
  (map->Router* {}))
