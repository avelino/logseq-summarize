(ns tldr
  (:require [clojure.string :as string]
            [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [util :refer [devlog]]))

(def ^:private api-host
  "https://api.tldr.chat/v0")

(defn format-md-output
  "auto fix markdown output from tldr.chat"
  [content]
  ;; TODO: use a library that auto-fixed markdown
  (string/replace content "- " "* "))

;; curl -H 'Accept: text/markdown' https://tldr.chat/url/https://xkcd.com/1438/
(defn- get-url
  "GET url from tldr.chat in format of markdown"
  [url]
  (p/let [req-url (str api-host "/url/" url)
          resp (fetch/get req-url {:headers {:Accept "text/markdown"}})
          body (:body resp)]
    (devlog "tldr/get-url" req-url (format-md-output body))
    (assoc resp :body (format-md-output body))))

;; curl -X POST --data url=https://xkcd.com/1438/ https://tldr.chat/summarize
(defn summarize-url
  "POST url to tldr.chat and get summary in format of markdown"
  [url]
  (p/let [_ (fetch/post (str api-host "/summarize")
                        {:headers {:Content-Type "application/json"}
                         :body (fetch/encode-body :json {:url url})}) ;; first request to notify tldr.chat that we are waiting
          request-tldr (get-url url)]
    (loop [result request-tldr]
      (devlog "tldr/summarize-loop" result url)
      (cond
        ;; continue the loop while status is 202
        (= (:status result) 202)
        (do
          (devlog "tldr/summarize-url waiting..." url)
          (-> (p/delay 2000)
              (p/then #(get-url url))))

        ;; status 200 - returns the result
        (= (:status result) 200)
        result

        ;; status outside the 200 range - returns error
        :else
        {:body
         (str "tldr.chat/error - status " (:status result) " / body: " (:body result))}))))
