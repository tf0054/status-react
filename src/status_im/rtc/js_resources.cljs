(ns ^:figwheel-always status-im.rtc.js-resources
  (:require-macros [status-im.utils.slurp :refer [slurp]])
  (:require [status-im.utils.types :refer [json->clj]])
  )

(def contract (.parse js/JSON (slurp "resources/rtc-contract.json")))

(def photo  (.parse js/JSON (slurp "resources/rtc-photo.json")))
