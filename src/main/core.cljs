(ns core
  (:require ["@logseq/libs"]
            [clojure.string :refer [trim]]
            [ls]
            [promesa.core :as p]
            [tldr]
            [errors :as e]
            [util :as u :refer [devlog]]))

(defn handle-summary-result
  "Pure function to handle summary results"
  [uuid-child result]
  (if (e/api-error? result)
    (ls/show-msg (str ":logseq-summarize/error " (:body result)) "error")
    (ls/update-block uuid-child (:body result))))

(defn summarize-block
  "Summarize current block and insert it as a child block"
  []
  (p/let [line-content  (ls/get-editing-block-content)
          current-block (ls/get-current-block)
          block-uuid    (aget current-block "uuid")]
    (if (or (u/http? line-content)
            (u/md-link? line-content))
      (p/let [link       (u/extract-link line-content)
              uuid-child (:uuid (ls/insert-block block-uuid
                                                 "processing: calling tldr.chat..."))
              loading    (atom ["." ".." "..." "...." "..." ".."])
              interval   (js/setInterval
                          #(do
                             (ls/update-block uuid-child
                                              (str "processing: calling tldr.chat" (first @loading)))
                             (swap! loading (fn [curr]
                                              (conj (vec (rest curr)) (first curr)))))
                          500)
              result     (tldr/summarize-url (trim link))
              _         (js/clearInterval interval)]
        (handle-summary-result uuid-child result))
      (ls/show-msg ":logseq-summarize/error content is not a link" "error"))))

(defn main
  "Registering slash commands in Logseq"
  []
  (doseq [cmd ["summarize" "sum"]]
    (devlog "Registering slash command:" cmd)
    (ls/register-slash-command cmd summarize-block)))

(defn -init
  "Top level logseq methods have to be called directly"
  []
  (-> (p/promise (js/logseq.ready))
      (p/then main)
      (p/catch #(js/console.error))))