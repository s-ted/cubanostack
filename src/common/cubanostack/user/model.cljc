;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.user.model
  (:require
    [cubanostack.model :as model]
    #?(:clj [schema.core :as s :refer [defschema]]
       :cljs [schema.core :as s :include-macros true :refer-macros [defschema]])))



(defschema PublicRead
  (merge model/EntitySchema
         {:username s/Str}))

(defschema PublicWrite
  {})

(defschema PrivateRead
  (merge PublicRead
         {:roles [s/Str]}))

(defschema PrivateWrite
  (merge PublicWrite
         {:clear-password s/Str}))

(defschema Persisted
  {:username s/Str
   :password s/Str
   :roles    [s/Str]})


(defschema LoginWrite
  {:username s/Str
   :password s/Str})
(defschema LoginRead
  {:username s/Str
   :token    s/Str})
