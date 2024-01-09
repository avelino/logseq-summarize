(ns tldr
  (:require [clojure.string :as string]
            [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [util :refer [devlog]]))

(def ^:private api-host
  "https://api.tldr.chat/")

(defn format-md-output
  "auto fix markdown output from tldr.chat"
  [content]
  ;; TODO: use a library that auto-fixed markdown
  (string/replace content "- " "* "))

;; curl -H 'Accept: text/markdown' https://tldr.chat/url/https://xkcd.com/1438/
(defn- get-url
  "GET url from tldr.chat in format of markdown"
  [url]
  (p/let [req-url (str api-host "v0/url/" url)
          resp (fetch/get req-url {:headers {:Accept "text/markdown"}})
          body (:body resp)]
    (devlog "tldr/get-url" req-url (format-md-output body))
    (assoc resp :body (format-md-output body))))

;; curl -X POST --data url=https://xkcd.com/1438/ https://tldr.chat/summarize
(defn summarize-url
  "POST url to tldr.chat and get summary in format of markdown"
  [url]
  (p/let [resp (fetch/post (str api-host "v0/summarize")
                           {:headers {:Content-Type "application/json"}
                            :body (fetch/encode-body :json {:url url})})
          retry-countdown (atom 10)]
    (while (and (not= (:status (get-url url)) 200)
                (pos? @retry-countdown))
      (devlog "tldr/summarize-url" url @retry-countdown)
      (p/delay 100000 "sleep 10s")
      (swap! retry-countdown dec))
    (get-url url)))
    
