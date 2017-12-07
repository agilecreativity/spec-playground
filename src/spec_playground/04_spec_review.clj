;; https://clojure.org/guides/spec
(ns spec-playground.04-spec-review
  (:require [clojure.spec.alpha :as s]))

(s/conform even? 1000) ;; 1000

(s/valid? even? 1000) ;; true

(s/valid? nil? nil) ;; true

(s/valid? string? "abc") ;; true

(s/valid? #(> % 5) 10) ;; true

(s/valid? #(> % 5) 2) ;; false

(import java.util.Date)
(s/valid? inst? (Date.)) ;; true

(s/valid? #{:club :diamond :heart :spade} :club) ;; true
(s/valid? #{:club :diamond :heart :spade} 42) ;; false

(s/valid? #{42} 42) ;; true

;; Registry
(s/def ::date inst?)
(s/def ::suit #{:club :diamond :heart :spade})

(s/valid? ::date (Date.)) ;; true
(s/conform ::suit :club) ;; :club
(s/conform ::suit 42) ;; :clojure.spec.alpha/invalid

;; Compositng predicates
(s/def ::big-even (s/and int? even? #(> % 1000)))
(s/valid? ::big-even :foo) ;; false
(s/valid? ::big-even 10) ;; false
(s/valid? ::big-even 0) ;; false
(s/valid? ::big-even 100000) ;; true

;; Use s/or to specify alternative
(s/def ::name-or-id (s/or :name string?
                          :id int?))

(s/valid? ::name-or-id "abc") ;; true
(s/valid? ::name-or-id 100) ;; true
(s/valid? ::name-or-id :foo) ;; false

(s/conform ::name-or-id "abc") ;; [:name "abc"]
(s/conform ::name-or-id 100) ;; [:id 100]

(s/conform ::name-or-id :some-symbol) ;; :clojure.spec.alpha/invalid
(s/valid? string? nil) ;; false

(s/valid? (s/nilable string?) nil) ;; true

;; Explain
(s/explain ::suit 42) ;; see the REPL
;; val: 42 fails spec: :spec-playground.04-spec-review/suit predicate: #{:spade :heart :diamond :club}

(s/explain ::big-even 5) ;; See the REPL output
(s/explain ::name-or-id :foo) ;; See the REPL output

(s/explain-data ::name-or-id :foo)
;; #:clojure.spec.alpha{:problems ({:path [:name], :pred clojure.core/string?, :val :foo, :via [:spec-playground.04-spec-review/name-or-id], :in []} {:path [:id], :pred clojure.core/int?, :val :foo, :via [:spec-playground.04-spec-review/name-or-id], :in []}), :spec :spec-playground.04-spec-review/name-or-id, :value :foo}

(s/explain-str ::name-or-id :foo) ;; "val: :foo fails spec: :spec-playground.04-spec-review/name-or-id at: [:name] predicate: string?\nval: :foo fails spec: :spec-playground.04-spec-review/name-or-id at: [:id] predicate: int?\n"

;; Entity Maps (to be continue)
