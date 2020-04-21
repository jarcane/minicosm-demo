(ns minicosm-demo.main
  (:require [minicosm.core :refer [start!]]
            [minicosm.ddn :refer [render-to-canvas]]))

(enable-console-print!)

(defn init [] 
  {:ship [236 450]
   :bullet {:visible false
            :loc [250 250]}})

(defn assets [] 
  {})

(defn on-key [{:keys [ship bullet] :as state} key-evs]
  (let [[x y] ship]
    (cond
      (key-evs "ArrowLeft") (assoc-in state [:ship] [(- x 3) y])
      (key-evs "ArrowRight") (assoc-in state [:ship] [(+ x 3) y])
      (key-evs "Space") (if (not (:visible bullet)) 
                          (-> state 
                            (assoc-in [:bullet :visible] true)
                            (assoc-in [:bullet :loc] [(+ x 12) (- y 16)]))
                          state)
      :else state)))

(defn on-tick [{:keys [bullet] :as state} time]
  (let [{:keys [visible loc]} bullet
        [bx by] loc]
    (if (and visible (>= by 0))
      (assoc-in state [:bullet :loc] [bx (- by 5)])
      (assoc-in state [:bullet :visible] false))))

(defn to-play [state assets is-playing] 
  {})

(defn to-draw [{:keys [ship bullet]} assets]
  (let [[x y] ship
        {:keys [visible loc]} bullet
        [bx by] loc]
    [:group {:desc "base"}
     [:rect {:style :fill :pos [0 0] :dim [500 500] :color "black"}]
     [:text {:pos [32 32] :color "white" :font "16px serif"} "Hello Space!"]
     [:text {:pos [x y] :font "32px serif"} "🔺"]
     (when visible [:text {:pos [bx by] :color "white" :font "32px serif"} "◽"])]))

(start!
  {:init init
   :assets assets
   :on-key on-key
   :on-tick on-tick
   :to-play to-play
   :to-draw to-draw})
