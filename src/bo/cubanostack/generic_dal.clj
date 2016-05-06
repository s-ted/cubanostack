;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.generic-dal)

(defprotocol GenericReadDal

  (find-all
    [this collection]
    [this collection request]
    "Retrieves all item ids")

  (find-all-deep
    [this collection]
    [this collection request]
    "Retrieves all items")

  (find-by-id
    [this collection id]
    "Retrieves an item by its ID")

  (existed?
    [this collection id]
    "Returns whether an item by this ID ever existed")

  (new?
    [this collection id]
    "Returns whether an item by this ID is newly created"))


(defprotocol GenericCrudDal

  (create-collection!
    [this collection]
    "Initializes collection if not yet existing")

  (create-with-id!
    [this collection id item]
    "Stores a new item and return its ID")

  (create!
    [this collection item]
    "Stores a new item (generating its ID) and return its ID")

  (delete!
    [this collection id]
    "Deletes an item by its ID")

  (update!
    [this collection id item]
    "Updates an item by its ID")

  (patch!
    [this collection id item]
    "Patches an item by its ID"))
