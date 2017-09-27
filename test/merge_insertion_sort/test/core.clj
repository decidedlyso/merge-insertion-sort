(ns merge-insertion-sort.test.core
  (:require
    [clojure.test :refer :all]
    [merge-insertion-sort.core :as lib]))

(deftest binary-search-insertion-point
  (testing "empty"
    (is (= 0 (lib/binary-search-insertion-point 1 []))))

  (testing "start"
    (is (= 0 (lib/binary-search-insertion-point 0 [1]))))

  (testing "end"
    (is (= 1 (lib/binary-search-insertion-point 2 [1])))
    (is (= 2 (lib/binary-search-insertion-point 2 [0 1]))))

  (testing "middle odd"
    (is (= 1 (lib/binary-search-insertion-point 1 [0 2])))
    (is (= 1 (lib/binary-search-insertion-point 1 [0 2 3 4]))))

  (testing "middle even"
    (is (= 1 (lib/binary-search-insertion-point 1 [0 2 3])))
    (is (= 1 (lib/binary-search-insertion-point 1 [0 2 3 4 5])))))

(deftest insert 
  (is (= [1] (lib/insert [] 1 0)))
  (is (= [1 2 3] (lib/insert [1 3] 2 1))))

(deftest binary-insert
  (is (= [0 1 2 3 4 5] (lib/binary-insert compare 4 [0 1 2 3 5])))
  (is (= [4] (lib/binary-insert compare 4 [])))
  (is (= [1 2 3 4] (lib/binary-insert compare 4 [1 2 3]))))

(deftest sort
  (let [target-max-comparisons [0 0 1 3 5 7 10 13 16 19 
                                22 26 30 34 38 42 46 50 54 58 62 66 71]
        benchmark (fn [n]
                    (let [sample-count 100
                          cs (atom [])
                          c (atom 0)
                          errors (atom [])]
                      (dotimes [_ sample-count]
                        (let [c (atom 0)
                              coll (range n)
                              shuffled-coll (shuffle coll)
                              result (lib/sort
                                       (fn [a b] 
                                         (swap! c inc)
                                         (compare a b))
                                       shuffled-coll)]
                          (when (not= result coll)
                            (swap! errors conj shuffled-coll))
                          (swap! cs conj @c)))
                      {:errors @errors
                       :max-comparisons (apply max @cs)}))]

    (doseq [i (range (count target-max-comparisons))]
      (let [{:keys [errors max-comparisons]} (benchmark i)]

        (testing (str "sorts properly for n = " i)
          (is (= [] errors)))

        (testing (str "counts optimally for n = " i)
          (is (= (target-max-comparisons i) max-comparisons))))))

  (testing "sorts with repeating"
    (is (= [1 1 2 2 3 3 4 4] (lib/sort [1 2 3 4 4 3 2 1]))))

  (testing "sorts non-integers"
    (is (= [[3 1] [4 2]] 
           (lib/sort 
             (fn [x y] 
               (compare (first x) (first y))) 
             [[4 2] [3 1]])))))
