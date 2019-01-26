(ns pjago.core
  (:use arcadia.core arcadia.linear)
  (:import [UnityEngine Screen GameObject Vector3 Rigidbody2D Animator AnimatorOverrideController])
  (:require [pjago :as p]
            [hicsuntdragons :as j]
            [pjago.camera :as cam]
            [common.processing :as x]
            [common.hooks :as h]))

;game constants
(def width-max 100)
(def height-max 12)
(def appart-x 2)
(def appart-y 1.5)
(def player-x 1)
(def player-y 3) ;will change to 4 with mushroom powerup
(def player-v 5) ;may change with powerups/themes

;matrix with the enemies, and the repetion number to kill them
;the height of the matrix associates with an interval from the tonic
(def random-level
  (for [width (range width-max)
        height (range height-max)]
    (if (< (rand) 0.1)
      (inc (rand-int 3))
      0)))

;@todo make sense of the enemy position
;https://www.howmusicreallyworks.com/Pages_Chapter_4/4_4.html
(def interval->animator
  (vec
   (concat
    (repeat 4 ::j/sad)
    (repeat 4 ::j/angry)
    (repeat 4 ::j/happy))))

(defn nota [position override repetition-value]
  (fn
    ([] [::j/nota])
    ([gob _])
    ([gob]
     (set! (.. gob transform position) position)
     (with-cmpt gob [am Animator]
       (set! (.runtimeAnimatorController am) override))
     (state+ gob :repetition repetition-value))))

(defn new-game "returns a 2d matrix with a level game generator"
  [level-values]
  (let [interval->animator (mapv #(x/resource % AnimatorOverrideController) interval->animator)]
    (vec
      (map-indexed 
        (fn [idx value]
          (let [i (quot idx 12)
                j (mod idx 12)]
            (if-not (zero? value)
              (nota (v3 (* i appart-x) (+ 1 (* (- j 6) appart-y)) 0.0)
                    (interval->animator j)
                    value))))
        level-values))))

;31.9753 = ortographicsSize*aspect*2 (main-camera)
(defn sidescroll-check
  [^GameObject gob k]
  (if (< (* appart-x width-max) (.. gob transform position x))
    (set! (.. gob transform position)
          (v3 -35 0 0))))

(defn sidescroll-start
  [^GameObject gob k]
  (with-cmpt gob [rb Rigidbody2D]
    (set! (.isKinematic rb) true)
    (set! (.. gob transform position) (v3 -35 0 0))
    (set! (.velocity rb) (v2 player-v 0.0))
    (hook+ gob :fixed-update :warp #'sidescroll-check)))

;@important needs to be run after changing a game constant!
;@todo think about integration with user files for level creation
(defn setup 
  ([] (setup random-level))
  ([level]
   (let [main-camera (object-named "main-camera")
         player (parent main-camera)]
     (sidescroll-start player :setup)
     (hook+ player :start :setup #'sidescroll-start)
     (mapv x/render (new-game level)))))