(ns knowledge.utilities)

(defn pmap-set
  "This function takes the same arguments as clojures (p)map and flattens the first level 
   of the resulting lists of lists into a set."
  [f & colls]
  (into #{} (apply concat (apply pmap f colls))))

