;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.state
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.wrapper.core :as w]
    [clojure.string :as str]))


(defprotocol State
  (store! [this id-path value])
  (delete! [this id-path])
  (retrieve [this id-path])
  (update! [this id-path f])
  (snapshot [this]))


; these 2 functions are to remove cyclic-dependencies
(defn- wm-register [State workflow wrapper-id wrapper]
  (store! State [:workflows workflow wrapper-id]
          {:active? true
           :wrapper wrapper}))
(defn- wm-unregister [State workflow wrapper-id]
  (delete! State [:workflows workflow wrapper-id]))


(defrecord State* [initial-state]
  c/Lifecycle

  (start [this]
    (if (::Store this)
      this
      (-> this
          (assoc ::Store (atom initial-state))
          (#(wm-register % :state/store! ::store!
                         (w/handler (fn [{:keys [id-path value]}] (store! % id-path value)))))
          (#(wm-register % :state/delete! ::delete!
                         (w/handler (fn [{:keys [id-path]}] (delete! % id-path)))))
          (#(wm-register % :state/update! ::update!
                         (w/handler (fn [{:keys [id-path f]}] (update! % id-path f))))))))

  (stop [this]
    (-> this
        (wm-unregister :state/update! ::udpate!)
        (wm-unregister :state/delete! ::delete!)
        (wm-unregister :state/store! ::store!)
        (assoc ::Store nil)))

  State

  (store!
    [this id-path value]

    (swap! (::Store this)
           assoc-in id-path value)
    this)

  (delete!
    [this
     [first-id-path & rest-id-paths :as id-path]]

    (if-not rest-id-paths
      (swap! (::Store this)
             #(dissoc % first-id-path))

      ; else
      (swap! (::Store this)
             #(update-in % (drop-last id-path) dissoc (last id-path))))
    this)

  (retrieve
    [this id-path]

    (get-in @(::Store this)
            id-path))

  (update!
    [this id-path f]

    (swap! (::Store this)
           #(update-in % id-path f)))

  (snapshot [this]
    @(::Store this)))


(defn new-state
  ([]
   (new-state {}))

  ([initial-state]
   (map->State* {:initial-state initial-state})))
