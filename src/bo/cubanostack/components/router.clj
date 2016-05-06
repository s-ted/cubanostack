;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.router
  (:require [com.stuartsierra.component :as c]
            [bidi.bidi :as bidi]
            [cubanostack.components.bus :as b]
            [cubanostack.components.state :as s]
            [cubanostack.wrapper.core :as w]
            [cubanostack.components.wrapper-manager :as wm]))

(defprotocol Router
  (match-route [this path req])
  (path-for [this route]))

(defprotocol RouteManager
  (routes [this])
  (add-routes [this routes])
  (remove-route [this path])
  (set-match-all-route [this handler])
  (match-all-handler [this]))

(defprotocol RingRouter
  (ring-handler [this]))


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

(defn- -match-route-handler [routes options]
  (let [routes ["/" routes]]
    (fn [{:keys [path]}]
      (bidi/match-route* routes path options))))

(defn- -path-for-handler [routes]
  (let [routes ["/" routes]]
    (fn [{:keys [handler route-params]}]
      (apply bidi/path-for routes handler (flatten (seq route-params))))))


(defrecord Router* [initial-routes Bus State WrapperManager]
  c/Lifecycle

  (start [this]
    (s/store! State [:router/routes] initial-routes)

    (wm/register WrapperManager :router/set-match-all-route ::set-match-all-route
                 (w/handler
                   (fn [{:keys [handler]}]
                     (set-match-all-route this handler))))

    (wm/register WrapperManager :router/add-route ::add-route
                 (w/handler
                   (fn [{:keys [route handler]}]
                     (add-routes this (assoc-in {} route handler)))))

    (wm/register WrapperManager :router/remove-route ::remove-route
                 (w/handler
                   (fn [{:keys [route]}]
                     (remove-route this route))))
    this)

  (stop [this]
    (wm/unregister WrapperManager :router/remove-route ::remove-route)
    (wm/unregister WrapperManager :router/add-route ::add-route)
    (wm/unregister WrapperManager :router/set-match-all-route ::set-match-all-route)

    (s/delete! State [:router/routes])

    this)

  RouteManager

  (add-routes [this routes]
    (s/update! State [:router/routes] #(deep-merge % routes))
    this)

  (remove-route [this path]
    (s/delete! State (cons :router/routes path))
    this)

  (set-match-all-route [this handler]
    (s/store! State [:router/match-all-handler] handler)
    this)

  (match-all-handler [this]
    (s/retrieve State [:router/match-all-handler]))

  (routes [this]
    (s/retrieve State [:router/routes]))

  Router

  (match-route [this path options]
    (let [handler (wm/wrap-with WrapperManager
                                :router/match-route
                                (-match-route-handler
                                  (routes this) options))]
      (handler {:path path})))

  (path-for [this route]
    (let [handler (wm/wrap-with WrapperManager
                                :router/path-for
                                (-path-for-handler
                                  (routes this)))]
      (handler route)))

  RingRouter

  (ring-handler [this]
    (fn [{:keys [uri path-info] :as req}]
      (let [path (or path-info uri)

            {:keys [handler route-params] :as match-context}
            (match-route this path req)]
        (if handler
          (handler
            (-> req
                (update-in [:params] merge route-params)
                (update-in [:route-params] merge route-params)))
          (when-let [handler (match-all-handler this)]
            (handler req)))))))

(defn new-router []
  (map->Router* {}))
