;; Hello!

;; who am i?

;; Dylan Butman
;; https://github.com/pleasetrythisathome

;; what are we talking about?
;; https://github.com/pleasetrythisathome/bardo

(ns bardo-workshop.core
  (:require [cljs-time.core :as time]
            [bardo.ease :as e]
            [bardo.interpolate :as i]
            [bardo.transition :as t]))

;; what is this talk about?

;; Bardo is a library that can be used for animation...but this talk isn't really about animation.

;; Bardo attempts to present idioms for defining and acting on interpolators.

;; what is an interpolator?

;; first. what does it mean to interpolate? google says.

;; in·ter·po·late
;; "insert (something) between fixed points"

;; so, in the context of programming, what do we want to interpolate?

;; Values!

;; let's 'take two values

(def start 0)
(def end 5)

;; what is the midpoint?

(/ (- end start) 2)
;; => 5/2

;; what if we want an arbitary number between the two values?
;; the question becomes, how far between? let's define a function.

(defn between [start end d]
  (->> start
       (- end)
       (* d)
       (+ start)))

(between start end 0.5)
;; => 2.5
(between start end 0.1)
;; => 0.5

;; ok, that's fine. but what if we want our input value to be something other than 0-1.
;; say, in the case of animation, we want to start at start right now, and then we want the half way point to be 500ms later.

;; we might right a function like this.

(defn animate [start end duration]
  (let [start-time (time/now)]
    (fn []
      )))
