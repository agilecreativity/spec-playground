(ns spec-playground.01-leverage
  (:require [clojure.spec.test :as stest]
            [clojure.spec.gen :as gen]
            [clojure.spec :as s]
            [clojure.string :as str]))

(defn my-index-of
  "Returns the index at which search appears in source"
  [source search]
  (str/index-of source search))

(my-index-of "foobar" "b") ;; 3
(apply my-index-of ["foobar" "b"]) ;; 3

;; spec regex
(s/def ::index-of-args (s/cat :source string?
                              :search string?))

;; validation
(s/valid? ::index-of-args ["foo" "f"]) ;; true
(s/valid? ::index-of-args ["foo" 3])   ;; false

;; conformance and destructuring
(s/conform ::index-of-args ["foo" "f"])
;; {:source "foo", :search "f"}

(s/unform ::index-of-args {:source "foo" :search "f"}) ;; ("foo" "f")

(s/conform ::index-of-args ["foo" 3]) ;; :clojure.spec/invalid

(s/explain-str ::index-of-args ["foo" 3]) ;; "In: [1] val: 3 fails spec: :spec-playground.01-leverage/index-of-args at: [:search] predicate: string?\n"

(s/explain-data ::index-of-args ["foo" "3"])

;; composition
(s/explain-data (s/every ::index-of-args) [["good" "a"]
                                           ["ok" "b"]
                                           ["bad" 42]])

;; #:clojure.spec{:problems ({:path [:search], :pred string?, :val 42, :via [:spec-playground.01-leverage/index-of-args], :in [2 1]})}

;; example data generation (useful for testing!)
(s/exercise ::index-of-args)

;;=> ([("" "") {:source "", :search ""}] [("" "c") {:source "", :search "c"}] [("" "xo") {:source "", :search "xo"}] [("U0" "V") {:source "U0", :search "V"}] [("zD8" "Zm7N") {:source "zD8", :search "Zm7N"}] [("sp" "Z") {:source "sp", :search "Z"}] [("BP" "T") {:source "BP", :search "T"}] [("" "") {:source "", :search ""}] [("ek" "7") {:source "ek", :search "7"}] [("4y6z3" "Qd") {:source "4y6z3", :search "Qd"}])

;; Assertion - that we can turn on/off by using (s/check-asserts ..)
(s/check-asserts false)
(s/assert ::index-of-args ["foo" "f"])

(#_
 ;; Note: this will turn the assertion on and will raise the errors
 (do
   (s/check-asserts true)
   (s/assert ::index-of-args ["foo" 42])))

;; Result of evaluating the above expression
;; => Spec assertion failed In: [1] val: 42 fails at: [:search]
;; predicate: string? :clojure.spec/failure :assertion-failed
;; #:clojure.spec{:problems [{:path [:search], :pred string?, :val 42, :via [], :in [1]}], :failure :assertion-failed}

;; Specing a funtion

(s/fdef my-index-of
        :args (s/cat :source string?
                     :search string?)
        :ret nat-int?
        :fn #(<= (:ret %) (-> % :args :source count)))

;; generative testing!
(->>
 (stest/check `my-index-of)
 (stest/summarize-results)) ;; {:total 1, :check-failed 1}

;; With the result like the following:
(#_
 {:spec
  (fspec
   :args
   (cat :source string? :search string?)
   :ret
   nat-int?
   :fn
   (<= (:ret %) (-> % :args :source count))),
  :sym spec-playground.01-leverage/my-index-of,
  :failure
  {:clojure.spec/problems
   [{:path [:ret], :pred nat-int?, :val nil, :via [], :in []}],
   :clojure.spec.test/args ("" "0"),
   :clojure.spec.test/val nil,
   :clojure.spec/failure :check-failed}})

;; Instrumentation
(stest/instrument `my-index-of)

;; When we call with invalid data then we get the result
(#_(my-index-of "foo" 42))

;; Result in the REPL:
;; 1. Unhandled clojure.lang.ExceptionInfo
;; Call to #'spec-playground.01-leverage/my-index-of did not conform
;; to spec: In: [1] val: 42 fails at: [:args :search] predicate:
;; string? :clojure.spec/args ("foo" 42) :clojure.spec/failure
;; :instrument :clojure.spec.test/caller {:file
;;                                        "form-init2689459066077851442.clj", :line 97, :var-scope
;;                                        spec-playground.01-leverage/eval22411}
