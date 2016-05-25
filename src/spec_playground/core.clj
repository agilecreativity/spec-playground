(ns spec-playground.core
  (:require [clojure.spec :as s]))

;; From: http://clojure.org/guides/spec

(s/conform even? 1000) ;; 1000
(s/conform even? 1001) ;; :clojure.spec/invalid

(s/valid? even? 10) ;; true
(s/valid? even? 11) ;; false

(s/valid? nil? nil) ;; true

(s/valid? string? "abc") ;; true
(s/valid? string? 123) ;; false

(s/valid? #(> % 5) 10) ;; true
(s/valid? #(> % 5) 0) ;; false

(import java.util.Date)
(s/valid? #(instance? Date %) (Date.)) ;; true

(s/valid? #{:club :diamond :heart :spade} :club) ; true
(s/valid? #{:club :diamond :heart :spade} 42) ; false

(s/valid? #{42} 42) ;; true

;; Registry
(s/def ::date #(instance? Date %)) ;; :spec-playground.core/date
(s/def ::suit #{:club :diamond :heart :spade}) ;; :spec-playground.core/suit

(s/valid? ::date (Date.)) ;; true
(s/conform ::suit :club) ;; :club

;; Composing predicates
(s/def ::big-even (s/and integer? even? #(> % 1000))) ;; :spec-playground.core/big-even
(s/valid? ::big-even :foo) ;; false
(s/valid? ::big-even 10) ;; false
(s/valid? ::big-even 10000) ;; true

;; Use of s/or
(s/def ::name-or-id (s/or :name string?
                          :id integer?))

(s/valid? ::name-or-id "abc") ;; true
(s/valid? ::name-or-id 100) ;; true
(s/valid? ::name-or-id :foo) ;; false

;; Use with conform
(s/conform ::name-or-id "abc") ;; [:name "abc"]
(s/conform ::name-or-id 100) ;; [:id 100]

(s/valid? string? nil) ;; false
(s/valid? (s/nilable string?) nil) ;; true

;; Explain (quite different than the documention)
(s/explain ::suit 42) ;; nil
(s/explain ::big-even 5) ;; nil
(s/explain ::name-or-id :foo) ;; nil

(s/explain-data ::name-or-id :foo)
;; {:clojure.spec/problems {[:name] {:pred string?, :val :foo, :via []}, [:id] {:pred integer?, :val :foo, :via []}}}

;; Sequences: cat, alt, *, +, ?
(s/def ::ingredient (s/cat ::quantity number? :unit keyword?))
(s/conform ::ingredient [2 :teaspoon])
;; {:spec-playground.core/quantity 2, :unit :teaspoon}

(s/explain ::ingredient [2 :teaspoon]) ;; nil
(s/explain ::ingredient [11 "peaches"]) ;; nil
(s/explain ::ingredient [2]) ;; nil

(s/def ::seq-of-keywords (s/* keyword?))
(s/conform ::seq-of-keywords [:a :b :c :d])
;; [:a :b :c :d]

(s/explain ::seq-of-keywords [10 20])
