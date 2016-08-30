;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.rest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [com.stuartsierra.component :as c]
    [cljs-http.client :as http]
    [cljs.core.async :as async]

    [cubanostack.components.state :as state]
    [cubanostack.components.bus :as bus]
    [cubanostack.local-storage :as lstorage]))

(defprotocol Rest
  (find-all [this])
  (create! [this item])
  (get-one [this id])
  (delete! [this id])
  (replace! [this id entity])
  (patch! [this id entity]))




(defn- make-url
  ([Rest]
   (make-url Rest nil))

  ([{:keys [protocol hostname port url]} sub-url]
   (str protocol "//" hostname
        (when port ":") port
        "/" url
        sub-url)))


(defn- -ask-for-auth [status Bus]
  (doto Bus
    (bus/send! :notification/add {:content (case status
                                             401 "Server is asking for authentication!"
                                             403 "No enough permission!")})
    (bus/send! :route! {:handler :user/login}))
  nil
  )


(defn- -request
  ([url Bus State]
   (-request url Bus State nil))

  ([url Bus State req]
   (go
     (let [token (:token (lstorage/get-item-from-local-storage :current-user))

           {:keys [status body] :as response}
           (-> (merge {:oauth-token token}
                      req
                      {:url url})
               http/request
               async/<!)]
       (case status
         200    body
         201    body
         401    (-ask-for-auth status Bus)
         403    (-ask-for-auth status Bus)
         #_else nil)))))



(defrecord Rest* [url protocol hostname port
                  Bus State]
  c/Lifecycle

  (start [this]
    this)

  (stop [this]
    this)

  Rest

  (find-all [this]
    (-> this
        make-url
        (-request Bus State)))

  (create! [this entity]
    (-> this
        make-url
        (-request Bus State
                  {:method      :post
                   :json-params entity})))

  (get-one [this id]
    (-> this
        (make-url id)
        (-request Bus State)))

  (delete! [this id]
    (-> this
        (make-url id)
        (-request Bus State
                  {:method :delete})))

  (replace! [this id entity]
    (-> this
        (make-url id)
        (-request Bus State
                  {:method      :put
                   :json-params entity})))

  (patch! [this id entity]
    (-> this
        (make-url id)
        (-request Bus State
                  {:method      :patch
                   :json-params entity}))))

(defn new-rest [params]
  (map->Rest*
    (merge
      (let [location (aget js/window "location")]
        {:protocol (aget location "protocol")
         :port     (let [port (aget location "port")]
                     (when-not (= "" port)
                       port))
         :hostname (aget location "hostname")})
      params)))
