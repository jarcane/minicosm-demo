(ns minicosm-demo.main
  (:require [minicosm.core :refer [start!]]
            [minicosm.ddn :refer [render-to-canvas]]))

(enable-console-print!)

(defn make-enemy [x y]
  {:status :alive
   :loc [x y]})

(defn init [] 
  {:ship [236 450]
   :bullet {:visible false
            :loc [250 250]}
   :enemies {:offset {:direction :right
                      :value 0}
             :mobs (flatten (for [y (range 64 (* 5 48) 48)]
                              (for [x (range 32 (* 9 48) 48)]
                                (make-enemy x y))))
             :bullets []}
   :score 0
   :lives 4})                                

(defn assets [] 
  {:alien [:image "img/alien.png"]
   :explode [:image "img/explode.png"]
   :ship [:image "img/ship.png"]})

(def dead (render-to-canvas 32 32 [:rect {:pos [0 0] :dim [32 32] :color "black" :style :fill}]))

(defn on-key [{:keys [ship bullet] :as state} key-evs]
  (let [[x y] ship]
    (cond
      (key-evs "ArrowLeft") (assoc-in state [:ship] [(- x 3) y])
      (key-evs "ArrowRight") (assoc-in state [:ship] [(+ x 3) y])
      (key-evs "Space") (if (not (:visible bullet)) 
                          (-> state 
                            (assoc-in [:bullet :visible] true)
                            (assoc-in [:bullet :loc] [(+ x 15) y]))
                          state)
      :else state)))

(defn update-offset [{:keys [direction value] :as offset}]
  (case direction
    :right (if (= value 32)
             {:direction :left
              :value 31.5}
             (update offset :value #(+ % 0.5)))
    :left (if (= value -32)
            {:direction :right
             :value -31.5}
            (update offset :value #(- % 0.5)))))

(defn update-bullet [{:keys [visible loc] :as bullet}]
  (let [[bx by] loc]
    (if (and visible (>= by 0))
      (assoc bullet :loc [bx (- by 5)])
      (assoc bullet :visible false))))

(defn check-hit? [bullet offset enemy]
  (let [[bx by] (:loc bullet)
        [ex ey] (:loc enemy)
        ex' (+ ex (:value offset))]
    (and (= :alive (:status enemy))
         (:visible bullet)
         (< ex' bx (+ ex' 32))
         (< ey by (+ ey 32)))))

(defn expire-enemy [{:keys [status] :as enemy}]
  (if (= :explode status)
    (assoc enemy :status :dead)
    enemy))

(defn update-enemies [{:keys [bullet enemies] :as state}]
  (let [old-mobs (:mobs enemies)
        mobs (map expire-enemy old-mobs)
        do-hits (map #(if (check-hit? bullet (:offset enemies) %)
                          (assoc % :status :explode)
                          %)
                     mobs)
        hits? (some #(= :explode (:status %)) do-hits)]                  
    (-> state
        (assoc-in [:enemies :mobs] do-hits)
        (update-in [:bullet :visible] #(if hits? false %))
        (update-in [:score] #(if hits? (+ 100 %) %)))))

(defn spawn-enemy-bullets [{:keys [enemies] :as state} time]
  (let [{:keys [bullets mobs]} enemies
        gen-bullet (fn [mobs]
                     (let [[x y] (map #(+ 16 %) (:loc (rand-nth mobs)))]
                       [x y]))]
    (if (and (<= 0 (count bullets) 3)
             (= 0 (mod time 5)))
      (update-in state [:enemies :bullets] #(cons (gen-bullet mobs) %))      
      state)))



(defn on-tick [{:keys [bullet] :as state} time]
  (-> state
      (update-enemies)      
      (update-in [:bullet] update-bullet)
      (update-in [:enemies :offset] update-offset)
      (spawn-enemy-bullets time)))

(defn to-play [state assets is-playing] 
  {})

(defn to-draw [{:keys [ship bullet enemies score lives]} assets]
  (let [[x y] ship
        {:keys [visible loc]} bullet
        [bx by] loc
        {:keys [offset mobs]} enemies]
    [:group {:desc "base"}
     [:rect {:style :fill :pos [0 0] :dim [500 500] :color "black"}]
     [:text {:pos [16 16] :color "white" :font "16px monospace"} "SCORE: " score]
     [:text {:pos [400 16] :color "white" :font "16px monospace"} "LIVES: " lives]
     [:sprite {:pos [x y]} (:ship assets)]
     [:group {:desc "enemies"}
      (for [{:keys [status loc]} (:mobs enemies)]
        (let [[ex ey] loc]
          [:sprite {:pos [(+ ex (:value offset)) ey]}
            (case status
              :alive (:alien assets)
              :explode (:explode assets)
              :dead dead)]))]              
     (when visible [:rect {:pos [bx by] :dim [4 4] :color "white" :style :fill}])
     [:group {:desc "enemy bullets"}
      (for [[ebx eby] (:bullets enemies)]
        [:rect {:pos [ebx eby] :dim [4 4] :color "yellow" :style :fill}])]]))

(start!
  {:init init
   :assets assets
   :on-key on-key
   :on-tick on-tick
   :to-play to-play
   :to-draw to-draw})
