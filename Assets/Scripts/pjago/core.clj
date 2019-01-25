(ns pjago.core
  (:use arcadia.core arcadia.linear)
  (:require [pjago.camera :as cam]
            [common.processing :as x]
            [common.hooks :as h]))

(defn setup []
  (let [main-camera (object-named "main-camera")
        zezim (object-named "zezim")]
    (hook+ main-camera :start :setup #'cam/start-main-camera)
    (state+ main-camera :player zezim)
    (cam/start-main-camera main-camera :player)))
