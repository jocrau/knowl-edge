(ns knowledge.implementation.youtube
  (:require [knowledge.video :as video]
            [knowledge.states :as states]))

(def position)

(def states
  {-1 :unstarted
   0 :ended
   1 :playing
   2 :paused
   3 :buffering
   4 :cued})

(defn current-position [player] (.round js/Math (.getCurrentTime player)))
(defn current-state [player] (get states (.getPlayerState player)))
(defn play [player] (.playVideo player))
(defn goto [player position] (.seekTo player position))
(defn player-id [player] (.-id (.-a player)))

(defn check-position [player]
  #(if-not (= (current-state player) :unstarted)
     (let [new-position (current-position player)]
       (if-not (= new-position position)
         (do
           (set! position new-position)
           (states/send [(player-id player) :knowledge.video.position-changed] {:js-event nil :player player :position new-position}))))))

(defn init []
  (letfn [(on-ready [event]
                    (let [player (.-target event)]
                      (do 
                        (js/setInterval (check-position player) 100)
                        (states/send [(player-id player) :knowledge.video.player-ready] {:js-event event :player player}))))
          (on-state-changed [event]
                            (let [player (.-target event)]
                              (do
                                (states/send [(player-id player) :knowledge.video.state-changed] {:js-event event :player player :state (current-state player)})
                                ((check-position player)))))]
    (do 
      (let [tag (.createElement js/document "script")
            _ (set! (.-src tag) "https://www.youtube.com/iframe_api")
            first-script-tag (first (.getElementsByTagName js/document "script"))]
        (.insertBefore (.-parentNode first-script-tag) tag first-script-tag))
      (set! js/onYouTubeIframeAPIReady
            #(set! js/player (js/YT.Player. "player" 
                                            (clj->js {:events {:onReady on-ready
                                                               :onStateChange on-state-changed}})))))))

(.addEventListener js/document "DOMContentLoaded" init)