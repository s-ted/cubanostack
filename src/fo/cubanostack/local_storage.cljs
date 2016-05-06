;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.local-storage
  (:require
    clojure.walk))

(defn- -localStorage []
  (.-localStorage js/window))

(defn set-item-in-local-storage!
  "Set `key' in browser's localStorage to `val`."
  [k v]
  (some->> v
           clj->js
           (.stringify js/JSON)
           (.setItem (-localStorage) k)))

(defn get-item-from-local-storage
  "Returns value of `key' from browser's localStorage."
  [k]
  (some->> k
           (.getItem (-localStorage))
           (.parse js/JSON)
           js->clj
           clojure.walk/keywordize-keys))

(defn remove-item-from-local-storage!
  "Remove the browser's localStorage value for the given `key`"
  [k]
  (.removeItem (-localStorage) k))
