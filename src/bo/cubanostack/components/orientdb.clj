;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.components.orientdb
  (:require
    [com.stuartsierra.component :as c]
    liberator.representation
    [clj-time.coerce :as coerce]
    [clj-time.core :as t]
    [clojure.data.json :as json]
    clojure.data
    [cubanostack.generic-dal :as generic-dal])
  (:import
    (java.io PrintWriter)

    (com.orientechnologies.orient.core.sql OCommandSQL)
    (com.orientechnologies.orient.core.record.impl ODocument)
    (com.orientechnologies.orient.core.db.document ODatabaseDocumentPool ODatabaseDocumentTx)
    (com.orientechnologies.orient.core.id ORecordId)
    (com.orientechnologies.orient.core.query OQuery)
    (com.orientechnologies.orient.core.sql.query OSQLSynchQuery)))


(def ^:dynamic *page-size* 10)


(defn pool->db
  ([]
   (pool->db nil))

  ([{:keys [store login password]
     :or {store    "memory:dev"
          login    "admin"
          password "admin"}}]
   (pool->db store login password))

  ([store login password]
   (-> (ODatabaseDocumentPool/global)
       (.acquire store login password))))


(declare prop-in prop-out)
(declare document?)

(def kw->oclass-name
  (memoize
    (fn [k]
      (if (string? k)
        k
        (str (if-let [n (namespace k)] (str n "_"))
             (name k))))))

(def oclass-name->kw (memoize (fn [o] (keyword (.replace o "_" "/")))))


(deftype CljODoc [^ODocument odoc]
  clojure.lang.IPersistentMap
  (assoc [_ k v] (.field odoc (name k) (prop-in v)) _)
  (assocEx [_ k v] (if (.containsKey _ k)
                     (throw (Exception. "Key already present."))
                     (do (.assoc odoc k v) _)))
  (without [_ k] (.removeField odoc (name k)) _)

  java.lang.Iterable
  (iterator [_] (.iterator (.seq _)))

  clojure.lang.Associative
  (containsKey [_ k] (.containsField odoc (name k)))
  (entryAt [_ k] (if-let [v (.valAt _ k)] (clojure.lang.MapEntry. k v)))

  clojure.lang.IPersistentCollection
  (count [_] (count (.fieldNames odoc)))
  (cons [self o] (doseq [[k v] o] (.assoc self k v)) self)
  (empty [_] (with-meta (CljODoc. (ODocument.)) (meta _)))
  (equiv [_ o] (= odoc (if (instance? CljODoc o) (.-odoc o) o)))

  clojure.lang.Seqable
  (seq [_] (for [k (.fieldNames odoc)]
             (clojure.lang.MapEntry. (keyword k)
                                     (prop-out (.field odoc k)))))

  clojure.lang.ILookup
  (valAt [_ k not-found]
    (or (prop-out (.field odoc (name k)))
        (case k
          :#rid     (.getIdentity odoc)
          :#class   (oclass-name->kw (.field odoc "@class"))
          :#version (.field odoc "@version")
          :#size    (.field odoc "@size")
          :#type    (.field odoc "@type")
          nil)
        not-found))
  (valAt [_ k] (.valAt _ k nil))

  clojure.lang.IFn
  (invoke [_ k] (.valAt _ k))
  (invoke [_ k nf] (.valAt _ k nf))

  clojure.lang.IObj
  (meta [self] (prop-out (.field odoc "__meta__")))
  (withMeta [self new-meta]
    {:pre [(map? new-meta)]}
    (.field odoc "__meta__" (prop-in new-meta)) self)

  java.lang.Object
  (equals [self o] (= (dissoc odoc "__meta__") (if (instance? CljODoc o) (dissoc (.-odoc o) "__meta__") o)))

  liberator.representation/Representation
  (as-response [this context]
    (liberator.representation/as-response (into {} this) context)))

(defn wrap-odoc "Wraps an ODocument object inside a CljODoc object."
  [odoc]
  (with-meta
    (CljODoc. odoc)
    {:orid     (.getIdentity odoc)
     :oclass   (oclass-name->kw (.field odoc "@class"))
     :oversion (.field odoc "@version")
     :osize    (.field odoc "@size")
     :otype    (.field odoc "@type")}))

(defn prop-in ; Prepares a property when inserting it on a document.
  [x]
  (cond
    (keyword? x) (str x)
    (set? x) (->> x (map prop-in) java.util.HashSet.)
    (map? x) (apply hash-map (mapcat (fn [[k v]] [(str k) (prop-in v)]) x))
    (coll? x) (map prop-in x)
    (document? x) (.-odoc x)
    :else x))

(defn prop-out ; Prepares a property when extracting it from a document.
  [x]
  (cond
    (and (string? x) (.startsWith x ":")) (keyword (.substring x 1))
    (instance? java.util.Set x) (->> x (map prop-out) set)
    (instance? java.util.Map x) (->> x (into {}) (mapcat (fn [[k v]] [(prop-out k) (prop-out v)])) (apply hash-map))
    (instance? java.util.List x) (->> x (map prop-out))
    :else x))



(defn document? [x] (instance? CljODoc x))




(extend-protocol json/JSONWriter

  org.joda.time.DateTime
  (-write [in ^PrintWriter out]
    (.print out (str "\"" (coerce/to-string in) "\"")))

  java.util.UUID
  (-write [in ^PrintWriter out]
    (.print out (str "\"" (str in) "\"")))

  CljODoc
  (-write [in ^PrintWriter out]
    (.print out (.toJSON (.-odoc in)))))




(defn- -execute [db command & args]
  (->> command
       OCommandSQL.
       (.command db)
       (#(.execute % args))))








(defn- ODocument->map [^ODocument odoc]
  (with-meta
    (->> odoc
         wrap-odoc
         (into {})
         (#(dissoc % :__meta__))
         clojure.walk/keywordize-keys)
    {:orid     (.getIdentity odoc)
     :oclass   (oclass-name->kw (.field odoc "@class"))
     :oversion (.field odoc "@version")
     :osize    (.field odoc "@size")}))



(defn- find-map-by-id [Dal collection id]
  (let [query (OSQLSynchQuery. (str "SELECT FROM " (kw->oclass-name collection)
                                    " WHERE _id = '" id "'")
                               1)
        db    (pool->db (:config Dal))]
    (try

      (-> db
          .activateOnCurrentThread
          (.query ^OSQLSynchQuery query (to-array nil))
          first
          (#(when %
              (when-not (:sentinel %)
                (ODocument->map %)))))

      (catch java.lang.IllegalArgumentException e nil)

      (finally
        (.close db)))))


(def ^:private undefined-ORecordId
  (ORecordId. -1 -1))

(defn- -find-all-deep-paginated
  ([dal collection] (-find-all-deep-paginated dal
                                              collection
                                              undefined-ORecordId
                                              *page-size*))

  ([dal collection lower-rid page-size]
   (let [query     (OSQLSynchQuery. (str "SELECT FROM " (kw->oclass-name collection)
                                         " WHERE @rid > " lower-rid
                                         " LIMIT " page-size)
                                    page-size)
         db        (pool->db (:config dal))]
     (try

       (let [items (seq
                     (-> db
                         .activateOnCurrentThread

                         (.query ^OSQLSynchQuery query nil)))]
         (when items
           (cons items
                 (-find-all-deep-paginated
                   dal
                   collection
                   (.getIdentity (last items))
                   page-size))))

       (catch java.lang.IllegalArgumentException e nil)

       (finally
         (.close db))))))








(defrecord Dal [config]
  c/Lifecycle

  (start [this]
    (let [{:keys [store login password]
           :or {store    "memory:dev"
                login    "admin"
                password "admin"}}
          config]

      (if (.exists (ODatabaseDocumentTx. store))
        (when (:empty-at-start? config)
          (doto (ODatabaseDocumentTx. store)
            (.open login password)
            .drop
            .create))

        ; else
        (.create (ODatabaseDocumentTx. store))))

    this)

  (stop [this]
    this)

  generic-dal/GenericReadDal

  (find-all [this collection request]
    (vals
      (map :_id
           (let [db (pool->db config)]
             (try

               (.activateOnCurrentThread db)

               (->> collection
                    kw->oclass-name

                    (.browseClass db)
                    iterator-seq

                    (map ODocument->map)

                    (filter #(not (second (clojure.data/diff % request))))
                    (remove :sentinel)

                    seq)

               (catch java.lang.IllegalArgumentException e [])

               (finally
                 (.close db)))))))

  (find-all-deep
    [this collection]
    (generic-dal/find-all-deep this collection nil))

  (find-all-deep
    [this collection request]


    (flatten
      (lazy-cat
        (map
          (fn [raw-items]
            (->> raw-items
                 (map ODocument->map)
                 (remove :sentinel)

                 (filter #(not (second (clojure.data/diff % request))))))
          (-find-all-deep-paginated this collection)))))


  (find-by-id
    [this collection id]
    (when id
      (when-let [result (find-map-by-id this collection id)]
        (when-not (:sentinel result)
          result))))

  (existed?
    [this collection id]
    (:sentinel (find-map-by-id this collection id)))

  (new?
    [this collection id]
    (nil? (generic-dal/find-by-id this collection id)))

  generic-dal/GenericCrudDal

  (create-collection!
    [this collection]
    this)

  (create-with-id!
    [this collection id item]
    (let [id        id
          now       (t/now)
          new-item  (merge item
                           {:_id id
                            :updatedAt now
                            :createdAt now})]

      (let [db (pool->db config)]
        (try

          (.activateOnCurrentThread db)

          (-> collection
              kw->oclass-name
              ODocument.

              wrap-odoc

              (merge (clojure.walk/stringify-keys new-item))

              .-odoc
              .save

              wrap-odoc)

          (finally
            (.close db)))
        {:_id id})))

  (create!
    [this collection item]
    (generic-dal/create-with-id! this collection (str (java.util.UUID/randomUUID)) item))

  (delete!
    [this collection id]
    (generic-dal/patch! this collection id {:sentinel true}))

  (update!
    [this collection id item]
    (let [now       (t/now)
          new-item  (-> item
                        (merge {:updatedAt now})
                        (dissoc :_id))]

      (let [document (generic-dal/find-by-id this collection id)

            {:keys [orid]}
            (meta document)

            db       (pool->db config)]
        (try

          (doto db
            .activateOnCurrentThread
            .begin
            (.delete orid))

          (generic-dal/create-with-id! this collection id new-item)

          (.commit db)

          (finally
            (.close db))))))

  (patch!
    [this collection id item]
    (let [document (generic-dal/find-by-id this collection id)]
      (generic-dal/update! this collection id (merge document item)))))

(defn new-dal [config]
  (map->Dal {:config config}))
