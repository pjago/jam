(ns pjago.camera
  (:use arcadia.core arcadia.linear)
  (:import [UnityEngine GameObject Camera Quaternion Vector3])
  (:require [common.processing :as x]
            [common.hooks :as h]))

(def zezim-initial-position 
  (v3 0.6 0 -6.5))

(def main-camera-initial-position
  (v3 1.3 2.3 -9.0))

(defn start-main-camera [^GameObject cam k]
  (when-let [player (state cam :player)]
    (set! (.. player transform position) zezim-initial-position)
    (set! (.. cam transform position) main-camera-initial-position)
    (h/follow+ cam player :position)))