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
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

(s/def ::email-type (s/and string?
                           #(re-matches email-regex %)))
(s/def ::acctid int?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name
                              ::last-name
                              ::email]
                        :opt [::phone]))


(s/valid?  ::person
           {::first-name "Elon"
            ::last-name "Musk"
            ::email "elon@example.com"}) ;; true

(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           }) ;; false

;; How we use spec in real application
(if (s/valid? ::person
              {::first-name "Elon"
               ::last-name "Mustk"
               ;; ::email "elon@example.com"
               })
  "we have valid person"
  "Invalid person"
  )

(s/def :unq/person
  (s/keys :req-un [::first-name
                   ::last-name
                   ::email]
          :opt-un [::phone]))

(s/conform :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "mail@example.com"})
;; {:first-name "Elon", :last-name "Musk", :email "mail@example.com"}

(s/conform :unq/person
           {;;:first-name "Elon"
            :last-name "Musk"
            :email "mail@example.com"})
;; :clojure.spec.alpha/invalid

(s/explain :unq/person
           {:first-name "Elon"}) ;;=> nil
;; See the REPL
;; REPL: val: {:first-name "Elon"} fails spec: :unq/person predicate: (contains? % :email)

;; Use for validating record attribute
(defrecord person [first-name last-name email phone])
;; spec_playground.04_spec_review.person

(s/explain-str :unq/person
               (->person "Elon"
                         nil ;; last-name
                         nil
                         nil))

;; "In: [:last-name] val: nil fails spec: :spec-playground.04-spec-review/last-name at: [:last-name] predicate: string?\nIn: [:email] val: nil fails spec: :spec-playground.04-spec-review/email-type at: [:email] predicate: string?\n"

(s/conform :unq/person
           (->person "Elon"
                     nil ;; last-name
                     nil
                     nil))

;; :clojure.spec.alpha/invalid

;; Keyword arguments
(s/def ::port number?)
(s/def ::host string?)
(s/def ::id keyword?)
(s/def ::server (s/keys* :req [::id ::host]
                         :opt [::port]))
(s/conform ::server [::id :server-1
                     ::host "example.com"
                     ])
;; #:spec-playground.04-spec-review{:id :server-1, :host "example.com"}
(s/conform ::server [::id :server-1
                     ::host "example.com"
                     ::port 123])
;; #:spec-playground.04-spec-review{:id :server-1, :host "example.com", :port 123}

(s/explain-str ::server [::id :server-1
                         ::host "example.com"
                         ::port 123
                         :other "xyz"])
(do
  (s/def :animal/kind string?)
  (s/def :animal/says string?)
  (s/def :animal/common (s/keys :req [:animal/kind :animal/says]))

  (s/def :dog/tail? boolean?)
  (s/def :dog/breed string?)
  (s/def :animal/dog (s/merge :animal/common
                              (s/keys :req [:dog/tail?
                                            :dog/breed]))))
(s/conform :animal/dog
           {:animal/kind "dog"
            :animal/says "woof"
            :dog/tail? true
            :dog/breed "retriever"})

;; {:animal/kind "dog", :animal/says "woof", :dog/tail? true, :dog/breed "retriever"}
(s/valid? :animal/dog
          {:animal/kind "dog"
           :animal/says "woof"
           :dog/tail? true
           :dog/breed "retriever"})

;; true

;; Multi-spec (to-be-continue)
;; see: https://clojure.org/guides/spec
