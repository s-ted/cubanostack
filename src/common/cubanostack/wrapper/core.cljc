;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.wrapper.core
  #?(:clj (:require [slingshot.slingshot :refer [try+ throw+]])))

#?(:clj (defrecord PropagationStopper [body]))

(defprotocol Wrapper
  "A protocol to easily/modularly extends functionnalities using wrappers.

  Usage:
  (deftype TimeCommandWrapper
   Wrapper
   (before [this payload]
    (merge payload
           {::start-time (System/nanoTime)}))

   (after [this response payload]
    (assoc response
           :elapsed (- (System/nanoTime)
                       (::start-time payload)))))

  (def wrapped-command [cmd]
    (wrap-with cmd
      (TimeCommandWrapper.)))"

  (before
    [this payload]
    "The before method will be called before delegating to wrapped handlers.
    It returns the [optionnaly instrumentized] payload.")

  (after
    [this response payload]
    "The after medthod is called after delegating to wrapped handlers.
    It returns the [optionnaly instrumentized] response.
    The payload parameter is the payload as instrumentized by this Wrapper
    (can be useful for keeping track of 'local vars')."))

(defn wrap-with
  "Wraps a function around some wrappers/decorators.
  Each of the all-decorators must instanciate the Wrapper protocol.

  Usage:

  (defn command [param]
    [..])

  (def wrapped-command
    (wrap-with command
      (Wrapper1.)
      (Wrapper2. :with :args)
      [..]))

  (wrapped-command :arg)"
  [handler & all-decorators]

  (reduce
    (fn [handler wrapper]
      (fn [payload]
        (#?(:clj try+ :cljs try)
          (let [instrumentized-payload (before wrapper
                                               payload)]
            (after wrapper
                   (handler instrumentized-payload)
                   instrumentized-payload))
          (catch #?(:clj PropagationStopper :cljs :default) {:keys [body]}
            (after wrapper
                   body
                   nil)))))
    handler
    all-decorators))


(defn handler [f]
  (reify Wrapper
    (before [this payload]
      (let [result (f payload)]
        (if result
        #?(:clj (throw+ (PropagationStopper. {:body result}))
           :cljs (throw {:body result}))
        payload)))

    (after [this response payload]
      response)))
