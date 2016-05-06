;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.middleware
  (:require
    [com.stuartsierra.component :as c]))


(defn- middleware-fn [middleware entry]
  (if (vector? entry)
    (let [[f & keys] entry
          arguments  (map #(get middleware %) keys)]
      #(apply f % arguments))
    entry))

(defn- compose-middleware [middleware]
  (let [entries (:middleware middleware)]
    (->> (reverse entries)
         (map #(middleware-fn middleware %))
         (apply comp identity))))

(defprotocol Middleware
  (wrap-mw [this]))

(defrecord Middleware* [middleware]
  c/Lifecycle

  (start [this]
    (let [wrap-mw (compose-middleware middleware)]
      (assoc this ::wrap-mw wrap-mw)))

  (stop [this]
    (dissoc this ::wrap-mw))

  Middleware

  (wrap-mw [this]
    (::wrap-mw this)))

(defn new-middleware
  ([middleware]
   (->Middleware* middleware)))
