(ns knowledge.video)


(defprotocol PlayerInfo
  (current-position [player])
  (current-state [player]))

#_(defprotocol PlayerControls
  (play [player])
  (current-state [player]))

#_(deftype VideoPlayer [implementation])
