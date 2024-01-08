(ns core
  (:require ["@logseq/libs"]
            [clojure.string :refer [trim]]
            [ls]
            [promesa.core :as p]
            [tldr]
            [util :as u :refer [devlog]]))

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
              tldr-content (tldr/summarize-url (trim link))]
        (if (empty? (:body tldr-content))
          (ls/show-msg ":logseq-summarize/error no content found" "error")
          (ls/update-block uuid-child (:body tldr-content))))
      (ls/show-msg ":logseq-summarize/error content is not a link" "error"))))
(defn main []
  "Registering slash commands in Logseq"
  (doseq [cmd ["summarize" "sum"]]
    (devlog "Registering slash command:" cmd)
    (ls/register-slash-command cmd summarize-block)))

; Logseq handshake
; JS equivalent: `logseq.ready(main).catch(() => console.error)`
(defn -init
  "Top level logseq methods have to be called directly"
  []
  (-> (p/promise (js/logseq.ready))
      (p/then main)
      (p/catch #(js/console.error))))
