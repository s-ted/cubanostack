;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.crud
  (:require
    [liberator.core :as liberator]
    liberator.representation
    clojure.walk
    [clojure.java.io :as io]
    [clojure.string :as str]
    [cheshire.core :as json]
    [cubanostack.dal :as d]))

(def default-available-media-types ["application/json;charset=utf-8"
                                    "application/json"])

(defn body-as-string
  "Convert the body to a reader. Useful for testing in the REPL
   where setting the body to a string is much simpler."
  [ctx]

  (if-let [body (get-in ctx [:request :body])]
    (if (string? body)
      body
      (-> body
          io/reader
          slurp))))

(defn parse-json
  "For PUT and POST parse the body as json and store in the context
  under the given key."
  [context]

  (when ((hash-set :put :post :patch) (get-in context [:request :request-method]))
    (if-let [body (body-as-string context)]
      (let [json-data (json/parse-string body)]
        (clojure.walk/keywordize-keys json-data))
      (throw (Exception. "No body")))))

(defn check-content-type
  "For PUT and POST check if the content type is json."
  [ctx content-types]

  (if ((hash-set :put :post :patch) (get-in ctx [:request :request-method]))
    (let [content-type
          (-> ctx
              (get-in [:request :headers "content-type"])
              str
              (str/replace #"\s" "")
              str/lower-case)]
      (or
        (some (hash-set content-type) content-types)
        [false {:message (str "Unsupported Content-Type: " content-type)}]))
    true))


(def default-liberator-config
  {:available-media-types default-available-media-types
   :known-content-type?   #(check-content-type % default-available-media-types)})

(defn default-collection-liberator-config [map->entity entity->map Dal]
  (merge
    default-liberator-config
    {:allowed-methods       [:get :post :options]

     :malformed?            (fn [ctx]
                              (try
                                (case (get-in ctx [:request :request-method])
                                  :post    [false {:data (->> ctx
                                                              parse-json
                                                              (map->entity false))}]
                                  #_else   false)
                                (catch java.lang.Exception e
                                  [true {:message (.getMessage e)}])))

     :post!                 (fn [ctx]
                              {:entry (d/create! Dal
                                                 (:data ctx))})

     :post-redirect?        false

     :handle-created        :entry

     :handle-ok             (fn [_]
                              (map entity->map
                                   (d/find-all-deep Dal)))}))

(defn default-item-liberator-config [map->entity entity->map ctx->entity-id Dal]
  (merge
    default-liberator-config
    {:allowed-methods       [:get :put :patch :delete :options]

     :malformed?            (fn [ctx]
                              (try
                                (case (get-in ctx [:request :request-method])
                                  :put     [false {:data (->> ctx
                                                              parse-json
                                                              (map->entity false))}]
                                  :patch   [false {:data (->> ctx
                                                              parse-json
                                                              (map->entity true))}]
                                  #_else   false)
                                (catch java.lang.Exception e
                                  [true {:message (.getMessage e)}])))

     :exists?               (fn [ctx]
                              (when-let [entity (d/find-by-id Dal
                                                              (ctx->entity-id ctx))]
                                {:entry entity}))

     :existed?              (fn [ctx]
                              (d/existed? Dal
                                          (ctx->entity-id ctx)))

     :handle-ok             #(-> % :entry entity->map)

     :delete!               (fn [ctx]
                              (d/delete! Dal
                                         (ctx->entity-id ctx)))

     :can-put-to-missing?   false

     :put!                  (fn [ctx]
                              {:entry (d/update! Dal
                                                 (ctx->entity-id ctx)
                                                 (:data ctx :data))})

     :patch!                (fn [ctx]
                              {:entry (d/patch! Dal
                                                (ctx->entity-id ctx)
                                                (:data ctx))})

     :respond-with-entity?  false

     :new?                  (fn [ctx] (d/new? Dal (ctx->entity-id ctx)))
     :handle-not-found      (fn [_] "\"Resource not found\"")}))
