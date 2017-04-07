(ns spec-playground.03-generators
  (:require [clojure.test :as t]
            [clojure.spec.test :as stest]
            [clojure.spec.gen :as gen]
            [clojure.spec :as s]
            [clojure.string :as str]))

;; Tools
;; - Overriding Generator
;; - Modelling
;;   bind
;;   fmap
;; - Combining Models
;;   one-of

;; Arbitrary predicates that are not efficient constructors
(s/def ::id (s/and string?
                   #(str/starts-with? % "FOO-")))

(#_
 (s/exercise ::id)) ;; get exception

;; Transform an existing generator
(defn foo-gen
  []
  (->> (s/gen (s/int-in 1 100))
       (gen/fmap #(str "FOO-" %))))

(s/exercise ::id 10 {::id foo-gen})
;; (["FOO-1" "FOO-1"] ["FOO-2" "FOO-2"] ["FOO-3" "FOO-3"] ["FOO-1" "FOO-1"] ["FOO-2" "FOO-2"] ["FOO-3" "FOO-3"] ["FOO-3" "FOO-3"] ["FOO-1" "FOO-1"] ["FOO-34" "FOO-34"] ["FOO-13" "FOO-13"])

;; Add generator to spec registry
(s/def ::id
  (s/spec (s/and string?
                 #(str/starts-with? % "FOO-"))
          :gen foo-gen))

(s/exercise ::id)
;; (["FOO-1" "FOO-1"] ["FOO-2" "FOO-2"] ["FOO-3" "FOO-3"] ["FOO-2" "FOO-2"] ["FOO-4" "FOO-4"] ["FOO-2" "FOO-2"] ["FOO-2" "FOO-2"] ["FOO-2" "FOO-2"] ["FOO-5" "FOO-5"] ["FOO-1" "FOO-1"])

;; Lookup - good use case for {:some-key "some-value", :more-key "more-value"}
(s/def ::lookup (s/map-of keyword? string? :min-count 1))
(s/exercise ::lookup)

;; Dependent-values
(s/def ::lookup-finding-k (s/and (s/cat :lookup ::lookup
                                        :k keyword?)
                                 (fn [{:keys [lookup k]}]
                                   (contains? lookup k))))

(#_
 (s/exercise ::lookup-finding-k))
;; => Couldn't satisfy such-that predicate after 100 tries.

;; Generator and bind a model
(defn lookup-finding-k-gen
  []
  (gen/bind
   (s/gen ::lookup)
   #(gen/tuple
     (gen/return %)
     (gen/elements (keys %)))))

(s/exercise ::lookup-finding-k 10
            {::lookup-finding-k lookup-finding-k-gen})

;; we get the result that we want
;; See the REPL

;; Previous example
(defn my-index-of
  "Returns the index at which search appears in source"
  [source search]
  (str/index-of source search))

(s/fdef my-index-of
        :args (s/cat :source string?
                     :search string?))

;; Rarely generates a findable, nontrivial string
(s/exercise-fn `my-index-of)
;; ([("" "") 0] [("3" "") 0] [("" "") 0] [("" "n") nil] [("syuw" "N") nil] [("C1eC" "sL1C") nil] [("" "QK9Z") nil] [("3aGcj1z" "yij") nil] [("utQMXDyV" "WF") nil] [("EOwf65Fm4" "4X5TKWw8G") nil])

;; Constructively generate a string and substring
(def model (s/cat :prefix string?
                  :match string?
                  :suffix string?))

(defn gen-string-and-substring
  []
  (gen/fmap
   (fn [[prefix match suffix]] [(str prefix match suffix) match])
   (s/gen model)))

;; Always generate a findable string
(s/def ::my-index-of-args (s/cat :source string?
                                 :search string?))
(s/fdef my-index-of
        :args (s/spec ::my-index-of-args
                      :gen gen-string-and-substring))

(s/exercise-fn `my-index-of)
;; now we have some matching case =>
;; ([["" ""] 0] [["U4h" "4"] 1] [["boR99" "R9"] 2] [["6r6" "r"] 1] [["p4kpP" "k"] 2] [["pszL8G9" ""] 0] [["9J9ya7np3BqhQO0" "np3B"] 6] [["v4596HW8Y45Xr" "96H"] 3] [["u2ox8" "ox8"] 2] [["24uDtK7" "4uDtK"] 1])

;; Combining models with one-of
(defn gen-my-index-of-args
  []
  ;; This will pick at random the sample from one of the two possible generators
  (gen/one-of [(gen-string-and-substring)
               (s/gen ::my-index-of-args)]))

;; Generating matches and non-matching
(s/fdef my-index-of
        :args (s/spec (s/cat :source string?
                             :search string?)
                      :gen gen-my-index-of-args))
(s/exercise-fn `my-index-of)

;; We have the good mix of the two =>
;; ([["" ""] 0] [("P" "") 0] [("Il" "4") nil] [("" "E") nil] [("X5d" "Xp") nil] [["L8Ys0U" "s0U"] 3] [("34jmh" "oLy16F") nil] [["JhGCeu2o3w6" ""] 0] [["27S3hlBj" "7S3hl"] 1] [["" ""] 0])

;; Custom Generator
;; - Do as little as possible
;; - Build on existing specs
;; - Make a model
;; - High leverage
