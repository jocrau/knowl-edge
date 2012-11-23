(ns knowledge.implementation.youtube
  (:require [knowledge.video :as video]))

(def position)

(def states
  {-1 :unstarted
   0 :ended
   1 :playing
   2 :paused
   3 :buffering
   4 :cued})

(extend-type video/VideoPlayer
  video/PlayerInfo
  (current-time [player] (.getCurrentTime (.-implementation player)))
  (current-state [player] (get states (.getPlayerState (.-implementation player)))))

(defn on-player-ready [event]
  #_(.playVideo (.-target event)))

(defn on-player-state-change [event]
  (let [player (video/VideoPlayer. (.-target event))
        update-fn (fn [] (if-not (= (video/current-state player) :unstarted)
                           (let [current-position (.round js/Math (video/current-time player))]
                             (if-not (= current-position position)
                               (set! position current-position)))))]
    (js/setInterval update-fn 600)))

(defn init []
  (do 
    (let [tag (.createElement js/document "script")
          _ (set! (.-src tag) "https://www.youtube.com/iframe_api")
          first-script-tag (first (.getElementsByTagName js/document "script"))]
      (.insertBefore (.-parentNode first-script-tag) tag first-script-tag))
    (set! js/onYouTubeIframeAPIReady
          #(set! js/player (js/YT.Player. "player" 
                                          (clj->js {:events {:onReady on-player-ready
                                                             :onStateChange on-player-state-change}}))))))

(.addEventListener js/document "DOMContentLoaded" init)