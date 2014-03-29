(ns gif-to-html.convert
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [mikera.image.core :refer [scale-image]])
  (:import [javax.imageio ImageReader ImageIO]
           [java.awt.image BufferedImage]
           java.awt.Color))

(set! *warn-on-reflection* true)

(def ascii [\# \A \@ \% \$ \+ \= \* \: \, \. \space])
(def max-items (dec (count ascii)))

(defn colors [^BufferedImage img ^Integer x ^Integer y]
  (let [c (Color. (.getRGB img x y))]
    [(.getRed c) (.getGreen c) (.getBlue c)]))

(defn ascii-color [^BufferedImage img ^Integer y ^Integer x]
  (let [[r g b :as rgb] (colors img x y)
        max-color (apply max rgb)
        idx (if (zero? max-color) max-items (int (+ (* max-items (/ max-color 255)) 0.5)))]
    (nth ascii (max idx 0))))

(defn to-ascii [^BufferedImage img]
  (let [width  (.getWidth img)
        height (.getHeight img)
        sb     (StringBuilder.)]
    (dotimes [y height]
      (dotimes [x width]
        (.append sb (ascii-color img y x)))
      (.append sb "\n"))
    (.toString sb)))

(defn scale [a b]
  (int (* (/ 100 a) b)))

(defn gif->html [input]
  (let [rdr  ^ ImageReader (.next (ImageIO/getImageReadersByFormatName "gif"))
        ciis (ImageIO/createImageInputStream input)]
    (.setInput rdr ciis false)
    (let [frame-count (.getNumImages rdr true)
          w           (.getWidth rdr 0)
          h           (.getHeight rdr 0)
          [w h]       (if (> w h) [100 (scale w h)] [(scale h w) 100])]
      {:data
       (html5
         [:div.animation
          (map (fn [i]
                  [:pre
                   {:id (str "frame-" i)
                    :style "font-size:6pt; letter-spacing:1px; line-height:6pt; font-weight:bold; display: none;font-family:monospace;"}
                   (to-ascii (scale-image (.read ^ImageReader rdr i) w h))])
               (range frame-count))])
       :frames frame-count})))
