(ns spec-playground.core
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))

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

(s/explain ::seq-of-keywords [10 20]) ;; nil
(s/conform ::seq-of-keywords [10 20]) ;; :clojure.spec/invalid

(s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                     :even (s/? even?)))

(s/conform ::odds-then-maybe-even [1 3 5 100])
;; {:odds [1 3 5], :even 100}

(s/conform ::odds-then-maybe-even [1])
;; {:odds [1]}

(s/explain ::odds-then-maybe-even [100]) ;; nil

;; opts are alternating keywords and booleans
(defn boolean? [b] (instance? Boolean b))

(s/def ::opts (s/* (s/cat :opt keyword? :val boolean?)))

(s/conform ::opts [:silent? false :verbose true])
;; [{:opt :silent?, :val false} {:opt :verbose, :val true}]

(s/def ::config (s/*
                 (s/cat :prop string?
                        :val (s/alt :s string? :b boolean?))))

(s/conform ::config ["-server" "foo"
                     "-verbose" true
                     "-user" "joe"])
;; [{:prop "-server", :val [:s "foo"]} {:prop "-verbose", :val [:b true]} {:prop "-user", :val [:s "joe"]}]

(s/describe ::seq-of-keywords) ;; (* keyword?)
(s/describe ::odds-then-maybe-even)
;; (cat :odds (+ odd?) :even (? even?))

(s/describe ::opts)
;; (* (cat :opt keyword? :val boolean?))

;; Regex operators that combine two expression
(s/def ::even-strings (s/& (s/* string?) #(even? (count %))))

(s/valid? ::even-strings ["a"])         ;; false
(s/valid? ::even-strings ["a" "b"])     ;; true
(s/valid? ::even-strings ["a" "b" "c"]) ;; false

;; If we use spec
(s/def ::nested
  (s/cat :names-kw #{:names}
         :names (s/spec (s/* string?))
         :nums-kw #{:nums}
         :nums (s/spec (s/* number?))))

(s/conform ::nested [:names ["a" "b"]
                     :nums [1 2 3]])
;; {:names-kw :names, :names ["a" "b"], :nums-kw :nums, :nums [1 2 3]}

;; If we remove the spec
(s/def ::unnested
  (s/cat :names-kw #{:names}
         :names (s/* string?)
         :nums-kw #{:nums}
         :nums (s/* number?)))
(s/conform ::unnested [:names "a" "b"
                       :nums 1 2 3])
;; {:names-kw :names, :names ["a" "b"], :nums-kw :nums, :nums [1 2 3]}

;; Entity Maps
(def email-regex #"^[a-zA-Z09._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(s/def ::acctid integer?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))
(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon@example.com"}) ;; true
;; fails required key check
(s/explain ::person
           {::first-name "Elon"}) ;; nil
;; fails attribute conformance
(s/explain ::person
           {::first-name "Elon"
            ::last-name "Musk"
            ::email "n/a"}) ;; nil
(s/def :unq/person
  (s/keys :req-un [::first-name ::last-name ::email]
          :opt-un [::phone]))
(s/conform :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "elon@example.com"})
;; {:first-name "Elon", :last-name "Musk", :email "elon@example.com"}
(s/explain :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "elon@example.com"})

(s/explain :unq/person
           {:firs-name "Elon"})

(defrecord Person [first-name last-name email phone])

(s/conform :unq/person
           (->Person "Elon" nil nil nil)) ;; :clojure.spec/invalid

(s/conform :unq/person
           (->Person "Elon" "Musk" "elon@example.com" nil))
;; #spec_playground.core.Person{:first-name "Elon", :last-name "Musk", :email "elon@example.com", :phone nil}

;; Use of *keys*
(s/def ::port number?)
(s/def ::host string?)
(s/def ::id keyword?)
(s/def ::server (s/keys* :req [::id ::host] :opt [::port]))
(s/conform ::server [::id :s1 ::host "example.com" ::port 5555])
;; {:spec-playground.core/id :s1, :spec-playground.core/host "example.com", :spec-playground.core/port 5555}

;; Multi-spec
(s/def :event/type keyword?)
(s/def :event/timestamp integer?)
(s/def :search/url string?)
(s/def :error/message string?)
(s/def :error/code integer?)

(defmulti event-type :event/type :default ::s/invalid)
(defmethod event-type ::s/invalid [_] nil)
(defmethod event-type :event/search [_]
  (s/keys :req [:event/type :event/timestamp :search/url]))
(defmethod event-type :event/error [_]
  (s/keys :req [:event/type :event/timestamp :error/message :error/code]))

(s/def :event/event (s/multi-spec event-type :event/type))

(s/valid? :event/event
          {:event/type :event/search
           :event/timestamp 12343243434
           :search/url "http://clojure.org"}) ;; true

(s/valid? :event/event
          {:event/type :event/search
           :event/timestamp 12343243434
           :error/message "Invalid host"
           :error/code 500}) ;; false

(s/conform :event/event
           {:event/type :event/search
            :search/url 200}) ;; :clojure.spec/invalid

;; Collections : coll-of, tuple, and map-of
(s/conform (s/coll-of keyword?) [:a :b :c]) ;; [:a :b :c]
(s/conform (s/coll-of number?) #{5 10 2}) ;; #{2 5 10}

;(s/def ::point (s/tuple double? double? double?))

(s/def ::scores (s/map-of string? integer?))
(s/conform ::scores {"Sally" 1000 "Joe" 500}) ;; {"Sally" 1000, "Joe" 500}

;; Using spec for validation
(defn person-name
  [person]
  {:pre [(s/valid? ::person person)]
   :post [(s/valid? string? %)]}
  (str (::first-name person) " " (::last-name person)))

;(person-name 42)
;;=> java.lang.AssertionError:

(person-name {::first-name "Elon"
              ::last-name "Musk"
              ::email "elon@example.com"}) ;; "Elon Musk"

;; Spec'ing functions
(defn ranged-rand
  "Returns random integer in range start <= rand < end"
  [start end]
  (+ start (rand-int (- end start))))

;; the specification for this function
(s/fdef ranged-rand
        :args (s/and (s/cat :start integer? :end integer?)
                     #(< (:start %) (:end %)))
        :ret integer?
        :fn (s/and #(>= (:ret %) (-> % :args :start))
                   #(< (:ret %) (-> % :args :end))))

;; Turn on instrumentation (spec checking) with:
(stest/instrument `ranged-rand)

(#_
 (ranged-rand 8 5)) ;; This gave errors when run

;; Higher order functions with fspec
(defn adder [x] #(+ x %))

(s/fdef adder
        :args (s/cat :x number?)
        :ret (s/fspec :args (s/cat :y number?)
                      :ret number?)
        :fn #(= (-> % :args :x) ((:ret %) 0)))

;; Conformers
(s/def ::name-or-id (s/or :name string?
                          :id integer?))

(s/conform ::name-or-id "abc") ;; [:name "abc"]

(s/def ::name-or-id
  (s/and ::name-or-id
         (s/conformer second)))

;; See: https://clojure.org/guides/spec#_instrumentation_and_testing
;; Testing

(stest/check `range-rand)
