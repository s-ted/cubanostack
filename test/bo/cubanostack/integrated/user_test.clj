;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.integrated.user-test
  (:require
    [cubanostack.user.model :as model]
    [cubanostack.integrated.crud :as crud]
    [midje.sweet :refer :all]))


(facts
  "user component"

  (facts
    "RESTFull API"

    #_(crud/test-crud "/app/user/"
                      model/PrivateRead
                      model/PrivateWrite)))
