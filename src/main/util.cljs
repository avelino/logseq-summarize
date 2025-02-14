(ns util
  "Utility helpers for data transformation, DOM etc..."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :refer [keywordize-keys]]
            [cuerdas.core :as str]
            [goog.html.textExtractor :as gtext]
            [medley.core :refer [filter-vals]]))

;; Specs
(s/def ::url string?)
(s/def ::label string?)
(s/def ::link string?)
(s/def ::md-link (s/keys :req-un [::label ::link]))
(s/def ::mime-type string?)
(s/def ::charset string?)
(s/def ::content-type (s/keys :req-un [::mime-type] :opt-un [::charset]))

;; Debug helpers
(def decode-html-content gtext/extractTextContent)

(defn devlog
  "Pure logging function that only runs in debug mode"
  [& msgs]
  (when goog.DEBUG
    (apply js/console.log (into ["Logseq Summarize"] msgs))))

;; DOM helpers
(defn target-value
  "Pure function to extract value from DOM event"
  [e]
  (.. e -target -value))

(defn target-checked
  "Pure function to extract checked state from DOM event"
  [e]
  (.. e -target -checked))

;; Data transformation
(defn ednize
  "Pure function to convert JS data to EDN"
  [data]
  (js->clj data :keywordize-keys true))

;; URL handling
(defn url?
  "Pure predicate to check if string is valid URL"
  [s]
  {:pre [(string? s)]}
  (try
    (do (js/URL. s) true)
    (catch js/Object _ false)))

(defn http?
  "Pure predicate to check if string is HTTP URL"
  [s]
  {:pre [(string? s)]}
  (and (str/starts-with? s "http")
       (url? s)))

;; Markdown link handling
(defn str->md-link
  "Pure function to parse markdown link string into map"
  [s]
  {:pre [(string? s)]}
  (let [trimmed (str/trim s)
        md-link (re-find #"\[(.*?)\]\((.*?)\)" trimmed)
        url (re-find #"https?://\S+" trimmed)]
    (cond
      md-link (-> {:label (first (rest md-link))
                   :link (second (rest md-link))})
      url {:label url :link url}
      :else nil)))

(defn md-link->str
  "Pure function to convert markdown link map to string"
  [{:keys [label link] :as md-link}]
  {:pre [(s/valid? ::md-link md-link)]}
  (str/format "[%s](%s)" label link))

(defn md-link?
  "Pure predicate to check if string is markdown link"
  [s]
  {:pre [(string? s)]}
  (some? (:link (str->md-link s))))

(defn extract-link
  "Pure function to extract link from content"
  [line-content]
  {:pre [(string? line-content)]}
  (if (md-link? line-content)
    (:link (str->md-link line-content))
    line-content))

;; Content type handling
(defn parse-content-type
  "Pure function to parse content type string"
  [s]
  {:pre [(string? s)]}
  (some->> (str/trim s)
           (re-find #"(.*?);\s*(.*?)$")
           rest
           (#(into {:mime-type (first %)}
                   (vector (-> % second (str/split #"=")))))
           keywordize-keys))

(defn json-response?
  "Pure predicate to check if content type is JSON"
  [content-type-str]
  {:pre [(string? content-type-str)]}
  (= "application/json" (:mime-type (parse-content-type content-type-str))))

;; Data structure helpers
(defn nested?
  "Pure predicate to check if data structure is nested"
  [data]
  (and (or (map? data) (sequential? data))
       (seq data)))

(defn attrs-and-children
  "Pure function to split map into flat and nested values"
  [data]
  {:pre [(map? data)]}
  [(filter-vals (complement nested?) data)
   (filter-vals nested? data)])

(defn tokenize-str
  "Pure function to split string into tokens"
  [s]
  {:pre [(string? s)]}
  (-> (str/trim s)
      (str/replace #"," " ")
      (str/split #"\s+")))

(defn exclude-include-ks
  "Pure function to filter map keys"
  [m exclude-keys include-keys]
  {:pre [(map? m) (sequential? exclude-keys) (sequential? include-keys)]}
  (cond-> m
    (seq exclude-keys) (select-keys (remove (set exclude-keys) (keys m)))
    (seq include-keys) (select-keys include-keys)))