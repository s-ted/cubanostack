;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.dal
  (:require
    [com.stuartsierra.component :as c]
    [cubanostack.generic-dal :as generic-dal]))

(defprotocol ReadDal

  (find-all
    [this]
    [this request]
    "Retrieves all item ids")

  (find-all-deep
    [this]
    [this request]
    "Retrieves all items")

  (find-by-id
    [this id]
    "Retrieves an item by its ID")

  (existed?
    [this id]
    "Returns whether an item by this ID ever existed")

  (new?
    [this id]
    "Returns whether an item by this ID is newly created"))


(defprotocol CrudDal

  (create-with-id!
    [this id item]
    "Stores a new item and return its ID")

  (create!
    [this item]
    "Stores a new item (generating its ID) and return its ID")

  (delete!
    [this id]
    "Deletes an item by its ID")

  (update!
    [this id item]
    "Updates an item by its ID")

  (patch!
    [this id item]
    "Patches an item by its ID"))



(defrecord Dal* [collection GenericDal]
  c/Lifecycle

  (start [this]
    (generic-dal/create-collection! GenericDal collection)
    this)

  (stop [this]
    this)

  ReadDal

  (find-all [this request]
    (generic-dal/find-all GenericDal collection request))

  (find-all-deep [this]
    (generic-dal/find-all-deep GenericDal collection))
  (find-all-deep [this request]
    (generic-dal/find-all-deep GenericDal collection request))


  (find-by-id [this id]
    (generic-dal/find-by-id GenericDal collection id))

  (existed? [this id]
    (generic-dal/existed? GenericDal collection id))

  (new? [this id]
    (generic-dal/new? GenericDal collection id))

  CrudDal

  (create-with-id! [this id item]
    (generic-dal/create-with-id! GenericDal collection id item))

  (create! [this item]
    (generic-dal/create! GenericDal collection item))

  (delete! [this id]
    (generic-dal/delete! GenericDal collection id))

  (update! [this id item]
    (generic-dal/update! GenericDal collection id item))

  (patch! [this id item]
    (generic-dal/patch! GenericDal collection id item)))


(defn new-dal [collection]
  (map->Dal* {:collection collection}))
