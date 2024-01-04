(ns tldr
  (:require [lambdaisland.fetch :as fetch]
            [promesa.core :as p]))

(def api-host "https://tldr.chat/")

;; curl -H 'Accept: text/markdown' https://tldr.chat/url/https://xkcd.com/1438/
(defn get-url
  "get url from tldr.chat in format of markdown"
  [url]
  (p/let [url (str api-host "url/" url)
          resp (fetch/get url {:accept "text/markdown"})]
    resp))
