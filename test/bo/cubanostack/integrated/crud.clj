;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.integrated.crud
  (:require
    [clojure.data.json :as json]
    [cubanostack.test-core :as tc]
    [schema.coerce :as coerce]
    [schema-tools.coerce :as stc]
    [schema.experimental.generators :as generator]
    [peridot.core :refer :all]
    [kerodon.test :as kt]
    [midje.sweet :refer :all]))


(defn test-crud [base-url read-schema write-schema]
  (facts
    "CRUD"

    (facts
      "listing"

      (fact
        "empty list should be returned when no item in db"
        (-> (tc/new-handler)
            session

            (request base-url)
            (kt/has (kt/status? 200))

            tc/parse-json-response)
        => []))

    (facts
      "creating item"

      (let [handler (tc/new-handler)

            [item1 item2 item3]
            (->> #(generator/generate write-schema)
                 repeatedly
                 distinct
                 (take 3))]

        (fact
          "post action should create an item"
          (-> handler
              session

              (request base-url
                       :request-method :post
                       :content-type   "application/json"
                       :body           (json/write-str item1))
              (kt/has (kt/status? 201))

              tc/parse-json-response
              :_id
              java.util.UUID/fromString)
          => truthy)

        (fact
          "and the listing should show it"
          (-> handler
              session

              (request base-url)
              (kt/has (kt/status? 200))

              tc/parse-json-response

              (kt/has
                (kt/validate =
                             count
                             1
                             "Only 1 item should be in the list"))


              first
              (stc/coerce
                read-schema
                coerce/json-coercion-matcher))
          => (contains item1))

        (let [id (-> (tc/entities->ids handler base-url)
                     first
                     java.util.UUID/fromString)]
          (fact
            "and we can ask for it by it's ID"
            (-> handler
                session

                (request (str base-url id))
                (kt/has (kt/status? 200))

                tc/parse-json-response
                (stc/coerce
                  read-schema
                  coerce/json-coercion-matcher))
            => (contains (assoc item1
                                :_id id)))


          (fact
            "and we can update it"
            (-> handler
                session

                (request (str base-url id)
                         :request-method :put
                         :content-type   "application/json"
                         :body           (json/write-str item2))
                (kt/has (kt/status? 204))

                :response
                :status)
            => 204

            (fact
              "and it has the new value"
              (-> handler
                  session

                  (request (str base-url id))
                  (kt/has (kt/status? 200))

                  tc/parse-json-response
                  (stc/coerce
                    read-schema
                    coerce/json-coercion-matcher))
              => (contains (assoc item2
                                  :_id id))))

          (fact
            "and we can patch it"
            (-> handler
                session

                (request (str base-url id)
                         :request-method :patch
                         :content-type   "application/json"
                         :body           (json/write-str item3))
                (kt/has (kt/status? 204))

                :response
                :status)
            => 204

            (fact
              "and it has the new value"
              (-> handler
                  session

                  (request (str base-url id))
                  (kt/has (kt/status? 200))

                  tc/parse-json-response
                  (stc/coerce
                    read-schema
                    coerce/json-coercion-matcher))
              => (contains (assoc item3
                                  :_id id))))

          (fact
            "and we can delete it"
            (-> handler
                session

                (request (str base-url id)
                         :request-method :delete)
                (kt/has (kt/status? 204))

                :response
                :status)
            => 204

            (fact
              "and it is no more accessible but marked as gone"
              (-> handler
                  session

                  (request (str base-url id))
                  :response
                  :status)
              => 410)

            (fact
              "and it is no more listed"
              (-> handler
                  session

                  (request base-url)
                  (kt/has (kt/status? 200))

                  tc/parse-json-response)
              => [])))))))
