(ns merge-insertion-sort.core
  (:refer-clojure :exclude [sort fn->comparator]))

; clojurescript has this in core, but clojure does not
; taken from cljs/core.cljs#L2383
(defn fn->comparator
  "Given a fn that might be boolean valued or a comparator,
   return a fn that is a comparator."
  [f]
  (if (= f compare)
    compare
    (fn [x y]
      (let [r (f x y)]
        (if (number? r)
          r
          (if r
            -1
            (if (f y x) 1 0)))))))

(defn jacobsthal
  "Return the nth Jacobsthal number (0-indexed)"
  [n]
  (Math/round (/ (+ (Math/pow 2 n) 
                    (Math/pow -1 (dec n)))
                 3)))

(defn pending-element-order
  "Indexes to insert `b`s at, given `n` to insert (0-indexed)"
  [n]
  (-> (->> (range)
           (map jacobsthal)
           (take-while (partial > n)))
      (concat [n])
      (->>
        (partition 2 1)
        (mapcat (fn [[a b]]
                    (range b a -1)))
        (map dec))))

(defn binary-search-insertion-point
  "Return the index at which to insert `n` into sorted `coll`, 
   using binary search. If `comp` not provided, uses compare."
  ([n coll]
   (binary-search-insertion-point compare n coll))

  ([comp n coll]
   (binary-search-insertion-point comp n 0 (dec (count coll)) coll))

  ([comp n lower-bound upper-bound coll]
   (if (not (<= lower-bound upper-bound))
     lower-bound
     (let [comp (fn->comparator comp)
           mid-index (quot (+ lower-bound upper-bound) 2)]
       (case (comp n (nth coll mid-index))
         1 (binary-search-insertion-point comp n (inc mid-index) upper-bound coll)
         0 mid-index
         -1 (binary-search-insertion-point comp n lower-bound (dec mid-index) coll))))))

(defn insert 
  "Returns a new collection with `n` inserted at index `i` into `coll`."
  [coll n i]
  (concat (take i coll) [n] (drop i coll)))

(defn binary-insert 
  "Returns a new collection with `n` inserted into sorted `coll` 
   so as to keep it sorted. If `comp` not provided, uses compare."
  ([n coll]
   (binary-insert compare n coll))

  ([comp n coll]
   (let [comp (fn->comparator comp)]
     (insert coll n (binary-search-insertion-point comp n coll)))))

(defn sort
  "Returns a sorted sequence of the items in coll, using the merge-insertion sorting algorithm.  
   If `comp` not provided, uses compare."
  ([coll]
   (sort compare coll))

  ([comp coll]
   (if (< (count coll) 2) 
     coll
     (let [comp (fn->comparator comp)
           sorted-pairs (->> coll
                             ; split into pairs [[a b] [a b] ...]
                             ; ignore last element if coll length is odd (for now)
                             (partition 2 2)
                             ; sort each pair so that b "<" a
                             (map (fn [pair]
                                    (if (< 0 (apply comp pair))
                                      pair
                                      (reverse pair))))
                             ; recursively sort pairs by the `a`s
                             ; so that [ [a1 b1] [a2 b2] ...]  a1 "<" a2 
                             (sort (fn [x y] 
                                     (comp (first x) (first y)))))

           ; initialize the `main-chain` with the `a`s from above
           ; and `pending-elements` with the `b`s from above
           ; if coll length is odd, add the (previously ignored) last element to pending-elements
           ;  main chain:        [a1 a2 a3 a4]
           ;  pending elements:  [b1 b2 b3 b4 bX]
           main-chain (atom (vec (map first sorted-pairs)))
           pending-elements (vec (map last sorted-pairs))
           pending-elements (if (odd? (count coll))
                              (conj pending-elements (last coll))
                              pending-elements)

           ; initalize helper atom + fns to keep track of the positions of `a`s in main-chain 
           ; so that we can always get the current main-chain up to a requested `a`
           ; (the positions of `a`s will change as pending-elements get inserted in the main-chain)
           a-positions (atom (vec (range (count @main-chain))))
           update-a-positions (fn [a-positions i]
                                (vec (map (fn [pos]
                                            (if (>= pos i)
                                              (inc pos)
                                              pos)) 
                                          a-positions)))
           main-chain-until (fn [a-index]
                              (take (get @a-positions a-index (count @main-chain)) @main-chain))

           ; fn to insert the pending-element corresponding to `b-index` 
           ; into the relevant part of the main chain (using binary search insertion)
           binary-insert! (fn [b-index]
                            (let [n (pending-elements b-index)
                                  insert-index (binary-search-insertion-point comp n (main-chain-until b-index))]
                              (swap! a-positions update-a-positions insert-index)
                              (swap! main-chain insert n insert-index)))]

       ; binary-insert each pending-element into the main-chain
       ; in an order that maximizes binary search efficiency 
       (doseq [i (pending-element-order (count pending-elements))]
         (binary-insert! i))

       @main-chain))))
