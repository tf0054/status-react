(ns status-im.rtc.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]
            [taoensso.timbre :as log]
            [status-im.rtc.js-resources :as r]
            [status-im.utils.datetime :as time]))

(register-sub :get-rtc-discoveries
              (fn [db [_]]
                
                ;;(do (http-post "https://api.github.com" "/gists" @db js/console))
                (let [discoveries (get-in @db [:rtc :discoveries])]
                  (log/debug "get-rtc-discoveries" (count (get-in @db [:rtc :discoveries])) (:rtc @db))
                  (reaction {:discoveries discoveries
                             :total discoveries})
                  )
                ))

(register-sub :get-rtc-msg
              (fn [db [_]]
                (reaction (get-in @db [:rtc :message]))
                ))
