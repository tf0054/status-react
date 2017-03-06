(ns status-im.utils.dev
  (:require [re-frisk-remote.core :as rr]))

(def debug?
  ^boolean js/goog.DEBUG)

(defn dev-setup []
  (when debug?
    (rr/enable-re-frisk-remote!)))
