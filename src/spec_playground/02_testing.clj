(ns spec-playground.02-testing
  (:require [clojure.test :as t]
            [clojure.spec.test :as stest]
            [clojure.spec.gen :as gen]
            [clojure.spec :as s]
            [clojure.string :as str]))

;; test/check :: generates conforming inputs, checks returns
;; test/instrument :: checks conforming inputs

;; spec for Testing
;; s/alt, s/?, s/nilable, :fn with :or and test/instrument

;; fn under test
(defn my-index-of
  "Returns the index at which search appears in source"
  [source search & opts]
  (apply str/index-of source search opts))

;; fspec
(s/fdef my-index-of
        :args (s/cat :source string?
                     :search string?)
        :ret nat-int?
        :fn #(<= (:ret %) (-> :args :source count)))

(s/exercise-fn `my-index-of)

;; ([("" "") 0] [("z" "5") nil] [("" "yw") nil] [("NM" "U") nil] [("" "8KC3") nil] [("I5" "") 0] [("B2" "nzO") nil] [("" "pq70") nil] [("0F9C7u" "e1ID9V7") nil] [("kklF6OGn8" "dIuJNV") nil])

;; alt
(s/conform (s/alt :string string? :char char?) ["foo"]) ;; [:string "foo"]
(s/conform (s/alt :string string? :char char?) [\f]) ;; [:char \f]
(s/conform (s/alt :string string? :char char?) [42]) ;; :clojure.spec/invalid
(s/explain-data (s/alt :string string? :char char?) [42]) ;; #:clojure.spec{:problems ({:path [:string], :pred string?, :val 42, :via [], :in [0]} {:path [:char], :pred char?, :val 42, :via [], :in [0]})}

(s/fdef my-index-of
        :args (s/cat :source string?
                     :search (s/alt :string string?
                                    :char char?))
        :ret nat-int?
        :fn #(<= (:ret %) (-> :args :source count)))

(s/exercise-fn `my-index-of)

;; ([("" \{) nil] [("" "") 0] [("" "") 0] [("qEV" "WYM") nil] [("" \s) nil] [("R" \) nil] [("" "6Cj0Tk") nil] [("" "9I") nil] [("2NB43" \) nil] [("T4c7e9V" \^) nil])

;; quantification (? is zero or one)
(s/conform (s/? nat-int?) []) ;; nil
(s/conform (s/? nat-int?) [1]) ;; 1
(s/explain-str (s/? nat-int?) [:a]) ;; "In: [0] val: :a fails predicate: nat-int?\n"
(s/explain-str (s/? nat-int?) [1 2]) ;; "In: [1] val: (2) fails predicate: (? nat-int?),  Extra input\n"

;; Let's update the fdef for optional args e.g. :from
(s/fdef my-index-of
        :args (s/cat :source string?
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        :ret nat-int?
        :fn #(<= (:ret %) (-> :args :source count)))

(s/exercise-fn `my-index-of)

;; ([("" \) nil] [("v" \c) nil] [("" \1 1) nil] [("m" \?) nil] [("l3" "" 1) 1] [("MUI2v" \ 1) nil] [("s2" \ó) nil] [("n" \ö) nil] [("m2" \ 2) nil] [("306" "XVnen1lF0" 2) nil])

;; Example testing (traditional way)
(assert (= 8 (my-index-of "testing with spec" "w")))

;; test/check generative testing
(->> (stest/check `my-index-of)
     (stest/summarize-results))

;;=> {:total 1, :check-failed 1}
;; The REPL:
(#_
 ;; ...
 {:clojure.spec/problems
  [{:path [:ret], :pred nat-int?, :val nil, :via [], :in []}],
  :clojure.spec.test/args ("" \  0),
  :clojure.spec.test/val nil,
  :clojure.spec/failure :check-failed})

;; nilable
(s/conform (s/nilable string?) "foo") ;; "foo"
(s/conform (s/nilable string?) nil) ;; nil
(s/explain-str (s/nilable string?) 42)  ;; "val: 42 fails at: [:clojure.spec/pred] predicate: string?\nval: 42 fails at: [:clojure.spec/nil] predicate: nil?\n"

;; With this knowledge we can use nilable to help with the :ret expression
(s/fdef my-index-of
        :args (s/cat :source string?
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        :ret (s/nilable nat-int?) ;; allow the result to return nil
        :fn #(<= (:ret %) (-> :args :source count)))

(->> (stest/check `my-index-of)
     (stest/summarize-results))
;; {:total 1, :check-threw 1}
;; In the REPL:
(#_
 ;; ...
 [{:type java.lang.NullPointerException
   :message nil
   :at [clojure.lang.Numbers ops "Numbers.java" 1013]}])

;; Note since the result can be nil the :fn need to be updated as well
;; Using or
(s/fdef my-index-of
        :args (s/cat :source string?
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        :ret (s/nilable nat-int?) ;; allow the result to return nil
        :fn (s/or
             :not-found #(nil? (:ret %)) ;; case the result is not found
             :found #(<= (:ret %) (-> % :args :source count))))

(->> (stest/check `my-index-of)
     (stest/summarize-results))

;; Finally we got at good result
;; {:total 1, :check-passed 1}

;; calling a speced fn

(defn which-came-first
  "Returns :checken or :egg, depending on which string appears
  first in s, starting from positon from."
  [s from]
  (let [c-idx (my-index-of s "chicken" :from from)
        e-idx (my-index-of s "egg" :from from)]
    (cond
      (< (c-idx e-idx) :chicken)
      (< (e-idx c-idx) :egg))))

;; Stacktrace Assisted Debugging
(#_
 (which-came-first "the chicken or the ege" 0))
;; Note this generate the errors

;; instrumentation
(do
  (stest/instrument `my-index-of)
  (which-came-first "the chicken or the egg" 0))

;; See the nice info on the REPL:
;; Call to #'spec-playground.02-testing/my-index-of did not conform to
;; spec: In: [2] val: :from fails at: [:args :from] predicate:
;; nat-int? :clojure.spec/args ("the chicken or the egg" "chicken"
;;                              :from 0) :clojure.spec/failure :instrument
;; :clojure.spec.test/caller {:file
;;                            "form-init2689459066077851442.clj", :line 132, :var-scope
;;                            spec-playground.02-testing/which-came-first}

;; test + instrumentation
(s/fdef which-came-first
        :args (s/cat :source string?
                     :from nat-int?
                     :ret #{:chicken :egg}))

(->>
 (stest/check `which-came-first)
 stest/summarize-results)

;;=> {:total 1, :check-threw 1}
