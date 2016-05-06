;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.model
  (:require
    #?(:clj  [schema.core :as s :refer [defschema]]
       :cljs [schema.core :as s :include-macros true :refer-macros [defschema]])))



(defschema EntitySchema
  {:_id       s/Uuid
   :createdAt #?(:clj (s/conditional string? s/Str :else org.joda.time.DateTime) :cljs js/Date)
   :updatedAt #?(:clj (s/conditional string? s/Str :else org.joda.time.DateTime) :cljs js/Date)})
