 (ns errors
  (:require [clojure.spec.alpha :as s]))

(s/def ::status pos-int?)
(s/def ::body string?)
(s/def ::error boolean?)

(s/def ::api-response
  (s/keys :req-un [::status ::body]
          :opt-un [::error]))

(defn error->response
  "Pure function to format error responses"
  [status body]
  {:status status
   :error true
   :body body})

(defn api-error?
  "Check if response contains an error"
  [response]
  (or (:error response)
      (not (<= 200 (:status response) 299))))