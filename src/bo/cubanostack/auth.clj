;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.auth
  (:require
    [environ.core :as env]

    [clj-time.core :as t]

    [buddy.auth :as buddy]
    [buddy.auth.backends.token :as buddy-token]
    [buddy.core.hash :as hash]
    [buddy.sign.jws :as jws]
    [buddy.sign.util :as jws-util]))



(def secret
  (hash/sha256
    (env/env :jws-auth-secret "my-secret-42!")))

(def auth-backend
  (buddy-token/jws-backend
    {:secret     secret
     :token-name "Bearer"}))




(defn any-granted?
  ([roles]
   (fn [ctx]
     (any-granted? ctx roles)))

  ([ctx roles]
   (seq
     (clojure.set/intersection
       (set (map keyword (get-in ctx [:request :identity :roles])))
       (set roles)))))


(defn authorized? [{:keys [request]}]
  (buddy/authenticated? request))


(defn user->token [{:keys [_id roles]}]
  (let [exp (-> (t/now)
                (t/plus (t/days 1))
                jws-util/to-timestamp)
        nbf (-> (t/now)
                (t/minus (t/minutes 10))
                jws-util/to-timestamp)
        iat (-> (t/now)
                jws-util/to-timestamp)


        claims {:sub   _id
                :exp   exp
                :nbf   nbf
                :iat   iat
                :aud   (env/env :jwt-aud "cubane")
                :iss   (env/env :jwt-iss "cubane")
                :roles roles}]
    (jws/sign claims
              secret
              {:alg :hs256})))
