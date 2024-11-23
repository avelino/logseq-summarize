(ns ls
  "Interops with Logseq plugin API"
  (:require ["@logseq/libs"]))

(def show-msg js/logseq.UI.showMsg)
(def get-current-block js/logseq.Editor.getCurrentBlock)
(def get-editing-block-content js/logseq.Editor.getEditingBlockContent)
(def update-block js/logseq.Editor.updateBlock)
(def insert-block js/logseq.Editor.insertBlock)
(def insert-batch-block js/logseq.Editor.insertBatchBlock)
(def register-slash-command js/logseq.Editor.registerSlashCommand)

;; Top level Logseq methods have to be called directly.
;; Defining in any ns won't work
;; (def ready js/logseq.ready)
;; (def use-settings-schema js/logseq.useSettingsSchema)
;; (def show-main-ui js/logseq.showMainUI)
;; (def toggle-main-ui js/logseq.toggleMainUI)
;; (def hide-main-ui js/logseq.hideMainUI)
;; (def provide-model js/logseq.provideModel)
;; (def set-main-ui-inline-style js/logseq.setMainUIInlineStyle)
;; (def show-settings-ui js/logseq.showSettingsUI)

(defn reload-plugin [plugin-id]
  ;; In JS console: LSPluginCore.reload("logseq-summarize")
  ;; Since cljs REPL runtime lives in an iframe. 
  ;; Use `top.LSPluginCore` in the parent window.
  (js-invoke js/top.LSPluginCore "reload" plugin-id))
