(ns pjago.core
  (:use arcadia.core arcadia.linear)
  (:import [UnityEngine Screen Input KeyCode AudioSource AudioClip GameObject Vector3 Rigidbody2D Animator AnimatorOverrideController SpriteRenderer]
           [UnityEngine.UI Text]
           [SoundPiano])
  (:require [clojure.string :as str]
            [pjago :as p]
            [hicsuntdragons :as j]
            [pjago.camera :as cam]
            [common.processing :as x]
            [common.hooks :as h]))

;game constants
(def width 100)  ;nota units
(def height 16)  ;nota units
(def appart-x 2) ;nota scale
(def appart-y 1) ;nota scale
(def player-x 1) ;nota scale
(def player-y 3) ;will change to 4 with mushroom powerup
(def player-v 5) ;may change with powerups/themes

;matrix with the enemies, and the repetion number to kill them
;the height of the matrix associates with an interval from the tonic
(def random-level
  (for [w (range width)
        h (range height)]
    (if (< (rand) 0.1)
      (inc (rand-int 3))
      0)))

;@todo make sense of the enemy position
;https://www.howmusicreallyworks.com/Pages_Chapter_4/4_4.html
(def interval->animator
  (vec
   (concat
    (repeat 5 ::j/sad)
    (repeat 6 ::j/angry)
    (repeat 5 ::j/happy))))

;MUSIC 101

(defmacro get-keycodes []
  (let [syms '[W E T Y U O P A S D F G H J K L]]
    (mapv #(symbol "KeyCode" (str %)) syms)))

;@todo namespace these notes!
(def code->note
  (apply array-map 
    (interleave (get-keycodes)
      [  :C#4 :D#4      :F#4 :G#4 :A#4      :C#5 :D#5
       :C4  :D4  :E4  :F4  :G4  :A4  :B4  :C5  :D5])))

(def doremi {\C 0 \D 2 \E 4 \F 5 \G 7 \A 9 \B 11})
(def miredo (zipmap (vals doremi) (keys doremi)))

;value of a note is how many semi-tons it is away from C0
;for example, both C#0 and Db0 have a value of 1
(defn keyvalue [keynote]
  (let [[note oct] (str/split (name keynote) #"(?=[0-9])" 2)]
    (+ (case (second note) \b -1 \# 1 0)
       (doremi (first (str/capitalize note)))
       (* 12 oct))))

;jump accumulates on a keyword :\w[b|#]?[0-9]+
;if the interval is +/-, it will fall on #/b
(defn keyjump [keynote interval]
  (let [acc (if (number? keynote) keynote (keyvalue keynote))
        acc (+ acc interval)
        oct (quot acc 12)
        edc (mod acc 12)]
    (keyword
      (if (contains? miredo edc)
        (str (miredo edc) oct)
        (if (pos? interval)
          (str (miredo (dec edc)) \# oct)
          (str (miredo (inc edc)) \b oct))))))

(def tile->code
  (vec (sort-by (comp keyvalue code->note) (get-keycodes))))

(def tile->note
  (comp code->note tile->code))

;@maybe take out this 0.5
(defn tile->space [tile-x tile-y]
  (v3 (* tile-x appart-x)
      (* (- tile-y (* height 0.5)) appart-y)
      0))

; HOOKS

(defn nota-hit
  [^GameObject gob k ^Collider c]
  (when (-> (gobj c) 
            (cmpt Animator) 
            (.GetCurrentAnimatorStateInfo 0)
            (.IsName "keyboard-attack")) ;@bugy
    (.SetTrigger (cmpt gob Animator) "fade")
    (update-state (parent (gobj c)) :score inc)))

(defn switch-instrument
  [^GameObject gob k ^Collider c]
  (let [player (parent (gobj c))
        instrument (state player :instrument)
        instruments (state player :instruments)
        ciclo (conj instruments (first instruments))
        switch (second (drop-while #(not= instrument %) ciclo))]
    (.SetActive instrument false)
    (.SetActive switch true)
    (.SetTrigger (cmpt switch Animator) "powerup")
    (state+ player :instrument switch)
    (set! (.. gob transform position)
          (tile->space (rand-int width) (rand-int height)))))
    
(defn nota [position override repetition-value]
  (fn
    ([] [::j/nota])
    ([gob _])
    ([gob]
     (set! (.. gob transform position) position)
     (hook+ gob :on-trigger-enter2d :hit #'nota-hit)
     (hook+ gob :on-trigger-stay2d :hit #'nota-hit)
     (hook+ gob :on-trigger-exit2d :hit #'nota-hit)
     (with-cmpt gob [am Animator]
       (set! (.runtimeAnimatorController am) override))
     (state+ gob :repetition repetition-value))))

(defn new-game "returns a 2d matrix with a level game generator"
  [level-values]
  (let [interval->animator (mapv #(x/resource % AnimatorOverrideController) interval->animator)]
    (vec
      (map-indexed 
        (fn [idx value]
          (let [i (quot idx height)
                j (mod idx height)]
            (if-not (zero? value)
              (nota (v3 (* appart-x i) (* appart-y (- j (* 0.5 height))) 0.0)
                    (interval->animator j)
                    value))))
        level-values))))

;31.9753 = ortographicsSize*aspect*2 (main-camera)
(defn sidescroll-check
  [^GameObject gob k]
  (when (< (* appart-x width) (.. gob transform position x))
    (state+ gob :score 0)
    (set! (.. gob transform position)
          (v3 -35 0 0))))

(defn sidescroll-start
  [^GameObject gob k]
  (with-cmpt gob [rb Rigidbody2D]
    (set! (.isKinematic rb) true)
    (set! (.. gob transform position) (v3 -35 0 0))
    (set! (.velocity rb) (v2 player-v 0.0))
    (hook+ gob :fixed-update :warp #'sidescroll-check)))

(defn tile-move
  [^GameObject gob k]
  (let [keyboard (mapv #(Input/GetKeyDown %) tile->code)
        keycount (get (frequencies keyboard) true 0)]
    (when-not (zero? keycount)
      (loop [tile 0 srcs (cmpts gob AudioSource)]
        (when-let [src (and (< tile height) (first srcs))]
          (if (keyboard tile)
            (do (.PlayOneShot src (state gob (tile->note tile)))
                ;@todo handle more instruments later
                (when-let [instrument (state gob :instrument)]
                  (.SetTrigger (cmpt instrument Animator) "attack")
                  (set! (.. instrument transform localPosition) 
                        (tile->space 0.5 tile)))
                (recur (inc tile) (rest srcs)))
            (recur (inc tile) srcs)))))))

(defn game-ui
  [^GameObject gob _]
  (set! (.text (state gob :score-text))
        (str (state gob :score))))

;@important needs to be run after changing a game constant!
;@todo think about integration with user files for level creation
(defn setup
  ([] (setup random-level))
  ([level]
   (let [main-camera (object-named "main-camera")
         event-system (object-named "event-system")
         canvas (object-named "canvas")
         management #{main-camera event-system canvas}
         player (parent main-camera)]
     (sidescroll-start player :setup)
     (dotimes [tile height]
       (let [k (tile->note tile) nk (keyword "pjago" (name k))]
         (state+ player k (x/resource nk AudioClip))))
     (state+ player :instrument
                    (first 
                      (filter #(.activeSelf %)
                        (remove management (children player)))))
     (state+ player :instruments
                    (vec (remove management (children player))))
     ;(mapv x/render (new-game level))
     (state+ player :score-text 
                    (cmpt (first (children canvas)) Text))
     (state+ player :score 0)
     (hook+ player :update :gui #'game-ui)
     (hook+ player :update :tile #'tile-move)
     (hook+ player :start :setup #'sidescroll-start))))