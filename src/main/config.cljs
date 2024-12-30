 (ns config)

(def defaults
  {:api {:host "https://api.tldr.chat/v0"
         :timeout 2000
         :retries 3}
   :formatting {:markdown-list-marker "* "}
   :debug true})

(defn get-config
  "Pure function to get configuration with overrides"
  [& overrides]
  (merge defaults (apply merge overrides)))