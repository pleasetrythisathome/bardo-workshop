;; Hello!

;; who am i?

;; Dylan Butman
;; https://github.com/pleasetrythisathome

;; what are we talking about?
;; https://github.com/pleasetrythisathome/bardo
;; workshop repo
;; https://github.com/pleasetrythisathome/bardo-workshop

(ns bardo-workshop.core
  (:require [bardo.ease :as e]
            [bardo.interpolate :as i]
            [bardo.transition :as t]
            [cljs.core.async :as async :refer [<! chan timeout put! close!]]
            [cljs.core.match]
            [cljs.reader :as edn]
            [cljs-time.core :as time]
            [clojure.set :as set])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]]))

;; what is this talk about?

;; Bardo is a library that can be used for animation...but this talk isn't really about animation.

;; Bardo attempts to present idioms for defining and acting on interpolators.

;; what is an interpolator?

;; first. what does it mean to interpolate? google says.

;; in·ter·po·late
;; "insert (something) interpolate fixed points"

;; so, in the context of programming, what do we want to interpolate?

;; Values!

;; let's 'take two values

(def start 0)
(def end 5)

;; what is the midpoint?

(/ (- end start) 2)
;; => 5/2

;; what if we want an arbitary number interpolate the two values?
;; the question becomes, how far interpolate? let's define a function.

(defn interpolate [start end d]
  (->> start
       (- end)
       (* d)
       (+ start)))

(interpolate start end 0.5)
;; => 2.5
(interpolate start end 0.1)
;; => 0.5

;; ok, that's fine. but what if we want our input value to be something other than 0-1.
;; say, in the case of animation, we want to start at start right now, and then we want the half way point to be 500ms later.

;; we might right a function like this.

(defn animate [start end duration]
  (let [start-time (time/now)]
    (fn [t]
      (let [dt (/ (- t start-time) duration)]
        (interpolate start end dt)))))

(go
  (let [animator (animate 0 5 500)]
    (<! (timeout 250))
    (print (animator (time/now)))))
;; => around 2.5

;; well ok. that works, but what happens if we wait to long?

(go
  (let [animator (animate 0 5 500)]
    (<! (timeout 1000))
    (print (animator (time/now)))))
;; => around 10

;; wait, now our value is too high! how do we fix it? we might go back to interpolate

(defn interpolate [start end d]
  (let [d (Math/min d 1)]
    (->> start
         (- end)
         (* d)
         (+ start))))

(go
  (let [intrpl (animate 0 5 500)]
    (<! (timeout 1000))
    (print (intrpl (time/now)))))
;; => 5

;; what if we want to change the duration? well, we can't.

;; let's try a different approach, instead of closing over start time, or thinking about duration inside an interpolation function, let's change our initial interpolator.

;; bardo defines an interpolator as a higher order function that returns a (fn [t]) where t is (<= 0 t 1)

(defn interpolate [start end]
  (fn [t]
    (->> start
         (- end)
         (* t)
         (+ start))))

((interpolate 0 5) 0.5)
;; => 2.5

;; instead of defining conditions inside an interpolator, we can control the input to the returned function by wraping it in other functions.

;; let's define a function that will retrict our input interpolate 0 and 1

(defn clamp
  "clamp input to function so that (<= 0 t 1)"
  [f]
  (fn [t]
    (f (cond
        (< t 0) 0
        (> t 1) 1
        :else t))))

((clamp (interpolate 0 5)) 1.5)
;; => 5

;; we can define similar functions to perform other operations on our input. let's define a function to shift the input domain

(defn shift
  "shifts the domain of input from [cmin cmax] to [nmin nmax]"
  ([f cmin cmax] (shift f cmin cmax 0 1))
  ([f cmin cmax nmin nmax]
     (fn [t]
       (f (-> t
              (- cmin)
              (/ (- cmax cmin))
              (* (- nmax nmin))
              (+ nmin))))))

((-> (interpolate 0 5)
     (clamp)
     (shift 0 100))
 50)
;; => 2.5

;; with these two functions, we can rewrite our time based animator in a composable way

(defn shift-timed
  [f duration]
  (let [start (time/now)]
    (shift f start (+ start duration))))

(go
  (let [intrpl (-> (interpolate 0 5)
                   (clamp)
                   (shift-timed 500))]
    (<! (timeout 250))
    (print (intrpl (time/now)))))
;; => around 2.5

;; what if we want to apply easing? an easing function is just a function that modifies the input of our interpolator.

;; let's make things parabolic
(defn quad
  "Modeled after the parabola y = x^2"
  [f]
  (fn [t]
    (f (* t t))))

(-> (interpolate 0 5)
    (clamp)
    (quad)
    (shift 0 10)
    (map (range 11)))
;; =>
;; (0
;;  0.05000000000000001
;;  0.20000000000000004
;;  0.44999999999999996
;;  0.8000000000000002
;;  1.25
;;  1.7999999999999998
;;  2.4499999999999997
;;  3.2000000000000006
;;  4.050000000000001
;;  5)


;; we can even define some helper functions for higher order easing functions

(defn wrap
  [f ease]
  (fn [t]
    (f (ease t))))

(defn quad
  "Modeled after the parabola y = x^2"
  [t]
  (* t t))

(-> (interpolate 0 5)
    (clamp)
    (wrap quad)
    (shift 0 10)
    (map (range 11)))

;; bardo.ease defines a variety of easing functions and helpers

;; what if we want to combine interpolators together? we can write functions for that too

(defn mix
  [start end]
  (fn [t]
    ((interpolate (start t) (end t)) t)))

(-> (interpolate 0 5)
    (mix (interpolate 0 25))
    (shift 0 10)
    (map (range 11)))
;; => (0 0.7 1.8 3.3 5.2 7.5 10.2 13.299999999999999 16.8 20.7 25)

;; how about with a curve?

(-> (interpolate 0 5)
    (mix (interpolate 0 25))
    (wrap quad)
    (shift 0 10)
    (map (range 11)))
;; =>
;; (0
;;  0.05200000000000001
;;  0.23200000000000004
;;  0.612
;;  1.3120000000000003
;;  2.5
;;  4.3919999999999995
;;  7.251999999999999
;;  11.392000000000005
;;  17.172
;;  25)

;; what if we want to support different types of values?

;; we could write another intepolation function for strings

;; here's a silly one that assuming strings represent numbers. probably not a safe assumption.

(defn interpolate-string
  [start end]
  (comp str (apply interpolate (map edn/read-string [start end]))))

((interpolate-string "0" "5") 0.5)

;; but there's something better. let's use protocols!

(defprotocol IInterpolate
  (interpolate [start end]))

(extend-protocol IInterpolate
  number
  (interpolate [start end]
    (fn [t]
      (->> start
           (- end)
           (* t)
           (+ start)))))

((interpolate 0 5) 0.5)
;; => 2.5

;; this might also be a good time to support nil values. let's define a protocol let represent an interpolatable nil.

(defprotocol IFresh
  (fresh [x]))

(extend-protocol IFresh
  number
  (fresh [x]
    0))

(extend-protocol IInterpolate
  nil
  (interpolate [_ end]
    (interpolate (fresh end) end))
  number
  (interpolate [start end]
    (fn [t]
      (->> start
           (- (if (nil? end) (fresh start) end))
           (* t)
           (+ start)))))

((interpolate nil 5) 0.5)
;;=> 2.5
((interpolate 2 nil) 0.5)
;;=> 1

;; let's' support lists!

(extend-protocol IFresh
  PersistentVector
  (fresh [x]
    []))

(extend-protocol IInterpolate
  PersistentVector
  (interpolate [start end]
    (let [intrpls (map interpolate start end)]
      (fn [t]
        (mapv #(% t) intrpls)))))

((interpolate [0 5] [5 10]) 0.5)
;; => (2.5 7.5)

;; what if the lists are different sizes?

;; we need to redefine our list interpolator to interate over the larger list

(extend-protocol IInterpolate
  PersistentVector
  (interpolate [start end]
    (let [intrpls (for [k (range (Math/max (count start)
                                          (count end)))]
                   (->> [(nth start k nil) (nth end k nil)]
                        (apply interpolate)))]
      (fn [t]
        (mapv #(% t) intrpls)))))

((interpolate [0 nil] [2 1]) 0.5)
;; => (1 0.5)

;; how about maps?

(extend-protocol IFresh
  PersistentHashMap
  (fresh [x]
    {}))

(extend-protocol IInterpolate
  PersistentArrayMap
  (interpolate [start end]
    (let [intrpls (for [k (->> [start end]
                              (map keys)
                              (map set)
                              (apply set/union))]
                   [k (->> [start end]
                           (map k)
                           (apply interpolate))])]
      (fn [t]
        (->> (for [[k intrpl] intrpls]
               [k (intrpl t)])
             (into {}))))))

((interpolate {:a 1} {:a 2}) 0.5)
;;=> {:a 1.5}

((interpolate {:a 1} {:b 2}) 0.5)
;;=> {:a 0.5, :b 1}

;; what if we want values over time?

;; let's define a function that runs on request animation frame

(defn request-animation-frame
  [f]
  (if-let [native (let [vendors ["" "ms" "moz" "webkit" "o"]]
                    (->> vendors
                         (map #(aget js/window (str % "requestAnimationFrame")))
                         (filter identity)
                         (first)))]
    (.call native js/window f)))

(defn request-anim-chan
  []
  (let [out (chan)]
    (request-animation-frame (fn [t] (put! out (time/now))))
    out))

(defn animation-chan
  [duration]
  (let [out (chan)
        start (time/now)
        end (+ start duration)]
    (go
      (loop []
        (let [t (-> (<! (request-anim-chan))
                    (- start)
                    (/ duration))]
          (if (<= t 1)
            (do
              (put! out t)
              (recur))
            (do
              (put! out 1)
              (close! out))))))
    out))

(enable-console-print!)

(let [anim (animation-chan 10000)
      intrpl (interpolate 0 5)]
  (go
    (loop []
      (when-let [t (<! anim)]
        (print (intrpl t))
        (recur)))))
