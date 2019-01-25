(ns common.core
  (:use arcadia.core arcadia.linear)
  (:import [UnityEngine GameObject Vector3 Vector4]))

(defmacro v3map "equivalent to map, but the list is a Vector3 xyz"
  [f & v3s]
  (let [vs (->> (repeatedly (count v3s) #(gensym "v"))
                (map #(vary-meta % assoc :tag Vector3)))
        xs (map #(list '.x %) vs)
        ys (map #(list '.y %) vs)
        zs (map #(list '.z %) vs)]
    `(let [~@(interleave vs v3s)]
        (v3 (~f ~@xs) (~f ~@ys) (~f ~@zs)))))

(defmacro v4map "equivalent to map, but the list is a Vector4 xyzw"
  [f & v4s]
  (let [vs (->> (repeatedly (count v4s) #(gensym "v"))
                (map #(vary-meta % assoc :tag Vector4)))
        xs (map #(list '.x %) vs)
        ys (map #(list '.y %) vs)
        zs (map #(list '.z %) vs)
        ws (map #(list '.w %) vs)]
    `(let [~@(interleave vs v4s)]
        (v4 (~f ~@xs) (~f ~@ys) (~f ~@zs) (~f ~@ws)))))

(defmacro bit-match "similar to case, but will test every clause"
  [mask & clauses]
  `(do ~@(for [[bit# action#] (partition 2 clauses)]
           `(if-not (zero? (bit-and ~mask ~bit#))
              ~action#))))