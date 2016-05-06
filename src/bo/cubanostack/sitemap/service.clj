;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.sitemap.service
  (:require [com.stuartsierra.component :as c]
            [liberator.core :refer [defresource]]
            [clojure.data.xml :as xml]
            [cubanostack.components.bus :as bus]
            [cubanostack.crud :as crud]
            [cubanostack.components.router :as router]))



(defn- to-indexed-seqs [coll]
  (if (map? coll)
    coll
    (map vector (range) coll)))

(defn- flatten-path [path step]
  (if (coll? step)
    (->> step
         to-indexed-seqs
         (map (fn [[k v]] (flatten-path (conj path k) v)))
         (into {}))
    [path step]))

(defn path-walk [f coll]
  (->> coll
      (flatten-path [])
      (map #(apply f %))))


(defn- -Router->Sitemap [Router]
  (let [routes (router/routes Router)]
    (xml/emit-str
      (apply xml/element
        :urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
        (path-walk
          (fn [_ route]
            (try
              (xml/element
                :url {}
                (xml/element
                  :loc {}
                  (router/path-for Router route)))
              (catch Exception e nil)))
          routes)))))


(defresource entity-resource [Router]
  crud/default-liberator-config

  :available-media-types ["application/xml" "text/xml"]
  :allowed-methods       [:get]
  :handle-ok             (fn [_]
                           (-Router->Sitemap Router)))



(defrecord Service* [route-path Bus Router]
  c/Lifecycle

  (start [this]
    (doto Bus
      (bus/send! :router/add-route {:route   route-path
                                    :handler (entity-resource Router)}))
    this)

  (stop [this]
    (doto Bus
      (bus/send! :router/remove-route {:route route-path}))
    this))


(defn new-sitemapService []
  (map->Service* {:route-path ["sitemap.xml"]}))
