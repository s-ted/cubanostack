;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.config
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.components.state :as s]))




(defprotocol Config
  (store! [this id-path value])
  (delete! [this id-path])
  (retrieve [this id-path]))



(defrecord Config* [initial-config State]
  c/Lifecycle

  (start [this]
    (when-not (s/retrieve State [:Config])
      (s/store! State [:Config] initial-config))
    this)

  (stop [this]
    (s/store! State [:Config] nil)
    this)

  Config

  (store! [this id-path value]
    (s/store! State (cons :Config id-path) value)
    this)

  (delete! [this id-path]
    (s/delete! State (cons :Config id-path))
    this)

  (retrieve [this id-path]
    (s/retrieve State (cons :Config id-path))))


(defn new-config
  ([]
   (new-config {}))

  ([initial-config]
   (map->Config* {:initial-config initial-config})))
