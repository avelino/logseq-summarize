(ns core
  (:require ["@logseq/libs"]
            [clojure.string :refer [trim]]
            [run.avelino.logseq-libs.ui :as ls-ui]
            [run.avelino.logseq-libs.editor :as ls-editor]
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
          _            (devlog "Got line content:" line-content)
          current-block (ls/get-current-block)
          _            (devlog "Got current block:" current-block)
          block-uuid    (aget current-block "uuid")
          _            (devlog "Block UUID:" block-uuid)]
    (if (or (u/http? line-content)
            (u/md-link? line-content))
      (-> (p/let [link        (u/extract-link line-content)
                  _          (devlog "Extracted link:" link)
                  _          (devlog "Attempting to insert block with ls-editor...")


                   (ls-editor/insert-block! block-uuid "processing: calling tldr.chat...")
                  _          (devlog "Insert block result:" child-block)
                  child-uuid  (when child-block (aget child-block "uuid"))
                  _          (devlog "Child UUID:" child-uuid)
                  result      (tldr/summarize-url (trim link))]
            (if child-uuid
              (do
                (devlog "||||||||||| child-uuid:" child-uuid)
                (handle-summary-result child-uuid result))
              (ls/show-msg ":logseq-summarize/error failed to insert block" "error")))
          (p/catch (fn [err]
                    (devlog "Error inserting block:" err)
                    (ls/show-msg ":logseq-summarize/error failed to insert block" "error"))))
      (ls/show-msg ":logseq-summarize/error content is not a link" "error"))))

(defn main
  "Registering slash commands in Logseq"
  []
  (devlog "Initializing logseq-libs...")
  (-> (ls-editor/init!)  ;; Inicializa a biblioteca
      (p/then (fn [_]
                (doseq [cmd ["summarize" "sum"]]
                  (devlog "Registering slash command:" cmd)
                  (ls/register-slash-command cmd summarize-block))))
      (p/catch (fn [err]
                (devlog "Error initializing logseq-libs:" err)))))

(defn -init
  "Top level logseq methods have to be called directly"
  []
  (-> (p/promise (js/logseq.ready))
      (p/then main)
      (p/catch #(js/console.error))))