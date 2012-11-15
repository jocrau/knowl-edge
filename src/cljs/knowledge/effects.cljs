(ns knowledge.effects)

(def jquery (js* "$"))

(defn hide [elements]
  (jquery #(.hide (jquery elements))))

(defn show [elements]
  (jquery #(.show (jquery elements))))

(defn highlight [elements]
    (jquery #(.effect (jquery elements) "highlight")))
