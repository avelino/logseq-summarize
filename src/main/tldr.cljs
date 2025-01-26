(ns tldr
  (:require [clojure.string :as string]
            [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [config :as cfg]
            [errors :as e]
            [util :refer [devlog]]))

(def config (cfg/get-config))
(def api-host (-> config :api :host))
(def api-timeout (-> config :api :timeout))

(defn- process-body
  "Processes the response body by removing unnecessary content and formatting it.
   Returns the cleaned body text."
  [body]
  (->> [{:pattern #"\n{3,}" :replacement "\n\n"} ; Normalize newlines
        {:pattern #"^\s*[-*]\s*" :replacement ""} ; Remove list markers
        {:pattern #"(?m)^\s*$\n" :replacement ""} ; Remove empty lines
        {:pattern #"</?faq>" :replacement ""} ; Remove FAQ tags
        {:pattern #"(?m)^###\s*(.*)" :replacement "\n**$1**"} ; Headers to bold
        {:pattern #"^- " :replacement (-> config :formatting :markdown-list-marker)}]
       (reduce (fn
                 [text {:keys [pattern replacement]}]
                 (string/replace text pattern replacement))
               (string/trim body))))

(defn- get-summary
  "Private function that fetches a summary for a given URL from the API.
   Makes a GET request to fetch the summary in markdown format.
   Returns a response map with:
   - For 500+ errors: Converts error to standard response format
   - For 200 success: Returns summary with configured markdown list markers
   - Otherwise: Returns 202 status to continue polling"
  [url]
  (p/let [resp (fetch/get (str api-host "/url/" url)
                          {:headers {:Accept "text/markdown"}})
          body (:body resp)
          status (:status resp)]
    (devlog "get-summary response - status:" status "body:" body)
    (cond
      (>= status 500) (e/error->response status body)
      (= status 200) {:status 200 :body (process-body body)}
      :else {:status 202})))

(defn summarize-url
  "Summarizes content from a URL by making a POST request to initiate summarization,
   then polling the summary endpoint until completion. Uses configured timeout between retries.
   Returns a map containing :status and :body with the summary result."
  [url]
  (devlog "starting summarize for URL:" url)
  (p/let [post-resp (fetch/post (str api-host "/summarize")
                                {:headers {:Content-Type "application/json"}
                                 :body (fetch/encode-body :json {:url url})})
          _         (devlog "Post response:" post-resp)
          result    (get-summary url)]
    (devlog "Current result:" result)
    (if (= (:status result) 200)
      result
      (do
        (devlog "not ready yet, retrying...")
        (-> (p/delay api-timeout)
            (p/then #(summarize-url url)))))))