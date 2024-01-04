(ns tldr
  (:require [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [util :refer [devlog]]))

(def ^:private api-host "https://tldr.chat/")

;; curl -H 'Accept: text/markdown' https://tldr.chat/url/https://xkcd.com/1438/
(defn- get-url
  "GET url from tldr.chat in format of markdown"
  [url]
  (p/let [url (str api-host "url/" url)
          resp (fetch/get url {:accept "text/markdown"})]
    resp))

;; curl --data url=https://xkcd.com/1438/ https://tldr.chat/summarize
(defn summarize-url
  "POST url to tldr.chat and get summary in format of markdown"
  [url]
  (p/let [resp (fetch/post (str api-host "summarize")
                           {:body {:url url}})
          retry-countdown (atom 10)]
    (while (and (not= (:status (get-url url)) 200)
                (pos? @retry-countdown))
      (devlog "tldr/summarize-url"
              :url url
              :retry @retry-countdown
              :status (:status resp) :body (:body resp))
      (p/delay 10000 "sleep 10s")
      (swap! retry-countdown dec))
    (get-url url)))
