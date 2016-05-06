;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.helper.listing
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async]
    [cubanostack.rest :as rest]
    [cubanostack.components.bus :as bus]))

(defn refresh-list
  ([Bus Rest]
   (refresh-list Bus Rest [:listing]))

  ([Bus Rest state-path]
   (go
     (bus/send! Bus :state/delete!
                {:id-path state-path})

     (let [users (-> Rest
                     rest/find-all
                     async/<!)]
       (doto Bus
         (bus/send! :state/store!
                    {:id-path state-path
                     :value   users})
         (bus/send! :renderer))))))
