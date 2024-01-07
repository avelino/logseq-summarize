(ns tldr
  (:require [lambdaisland.fetch :as fetch]
            [promesa.core :as p]
            [util :refer [devlog]]))

(def ^:private api-host "https://tldr.chat/")

;; curl -H 'Accept: text/markdown' https://tldr.chat/url/https://xkcd.com/1438/
(defn- get-url
  "GET url from tldr.chat in format of markdown"
  [url]
  (p/let [req-url (str api-host "url/" url)
          resp (fetch/get req-url {:Accept "text/markdown"
                                   :Content-Type "text/markdown"})
          body (:body resp)]
    (js/console.log "response: " resp)
    (devlog "tldr/get-url" req-url body)
    resp))

;; curl -X POST --data url=https://xkcd.com/1438/ https://tldr.chat/summarize
(defn summarize-url
  "POST url to tldr.chat and get summary in format of markdown"
  [url]
  (p/let [resp (fetch/post (str api-host "summarize")
                           {:headers {:Content-Type "application/x-www-form-urlencoded"}
                            :body (str "url=" url)})
          retry-countdown (atom 10)]
    (while (and (not= (:status (get-url url)) 200)
                (pos? @retry-countdown))
      (devlog "tldr/summarize-url" url @retry-countdown)
      (p/delay 100000 "sleep 10s")
      (swap! retry-countdown dec))
    (get-url url)))
