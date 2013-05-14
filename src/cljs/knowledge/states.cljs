(ns knowledge.states)

(def transitions (atom #{}))

(defn subscribe
  ([condition action]
     (let [transition {:condition condition
                       :action action}]
       (swap! transitions conj transition)
       transition)))

(defn unsubscribe
  [transition]
  (swap! transitions disj transition))

(defn send
  ([event-id]
     (send event-id nil))
  ([event-id event-data]
     (let [matching-transitions (filter (fn [{condition :condition}]
                                          (condition event-id))
                                      @transitions)]
       (doseq [transition matching-transitions]
           ((:action transition) event-id event-data)))))
