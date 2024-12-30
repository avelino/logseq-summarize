(ns tldr
  (:require [clojure.string :as string]
            [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [config :as cfg]
            [errors :as e]
            [util :refer [devlog]]))

(def config (cfg/get-config))

(defn format-md-output
  "Pure function to format markdown output"
  [content]
  (string/replace content "- " (-> config :formatting :markdown-list-marker)))

(defn- build-request-url
  "Pure function to build API request URL"
  [endpoint url]
  (str (-> config :api :host) endpoint url))

(defn- handle-response
  "Pure function to process API response"
  [resp]
  (let [body (:body resp)]
    (if (e/api-error? resp)
      (e/error->response (:status resp) body)
      {:status (:status resp)
       :body (format-md-output body)})))

(defn- get-url
  "GET url from tldr.chat in format of markdown"
  [url]
  (p/let [req-url (build-request-url "/url/" url)
          resp (fetch/get req-url {:headers {:Accept "text/markdown"}})
          result (handle-response resp)]
    (devlog "tldr/get-url" req-url result)
    result))

(defn summarize-url
  "POST url to tldr.chat and get summary in format of markdown"
  [url]
  (p/let [_ (fetch/post (build-request-url "/summarize" "")
                        {:headers {:Content-Type "application/json"}
                         :body (fetch/encode-body :json {:url url})})
          request-tldr (get-url url)]
    (loop [result request-tldr]
      (cond
        (= (:status result) 202)
        (do
          (devlog "tldr/summarize-url waiting..." url)
          (-> (p/delay (-> config :api :timeout))
              (p/then #(get-url url))))
              
        (= (:status result) 200)
        result
        
        :else
        (e/error->response (:status result) 
                          (str "tldr.chat/error - " (:body result)))))))