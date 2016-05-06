;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.helper.ui
  #?(:cljs (:require
             [cublono-quiescent.interpreter :as cublono]
             [quiescent.core :as q])))

#? (:cljs
     (do
       (defn interpret [v]
         (cublono/interpret v))

       (defn component [render options]
         (q/component render options))

       (defn icon [kind]
         (interpret [:i.glyphicon {:class (str "glyphicon-" (name kind))}]))))

#?(:clj
    (do
      (defn- extract-docstr
        [[docstr? & forms]]
        (if (string? docstr?)
          [docstr? forms]
          ["" (cons docstr? forms)]))

      (defn- extract-opts
        ([forms] (extract-opts forms {}))
        ([[k v & forms] opts]
         (if (keyword? k)
           (extract-opts forms (assoc opts k v))
           [opts (concat [k v] forms)])))

      (defmacro defcomponent [name & forms]
        (let [[docstr forms]  (extract-docstr forms)
              [options forms] (extract-opts forms)
              [argvec & body] forms
              options         (merge {:name (str name)} options)
              sub-f-name      (symbol (str "-" name))]
          `(do
             (def ~sub-f-name
               (fn ~argvec
                 ~@body))

             (def ~name ~docstr
               (cubanostack.helper.ui/component
                 (fn [& args#]
                   (cubanostack.helper.ui/interpret
                     (apply ~sub-f-name args#)))
                 ~options)))))


      (defmacro h%
        "Macro which will stop you from inadvertently returning false in a handler,
        used to signal pseudo stopPropagation() or preventDefault()"
        ([& body]
         `(fn [~'event]
            ~@body

            ;; force return nil
            nil)))))
