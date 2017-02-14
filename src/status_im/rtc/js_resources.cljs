(ns ^:figwheel-always status-im.rtc.js-resources
  (:require-macros [status-im.utils.slurp :refer [slurp]])
  )

(def abi (.parse js/JSON (slurp "resources/rtc-abi.json")))

(def photo  (.parse js/JSON (slurp "resources/rtc-photo.json")))
