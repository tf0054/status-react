(ns status-im.rtc.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]
            [taoensso.timbre :as log]
            [status-im.rtc.js-resources :as r]
            [status-im.utils.datetime :as time]))

(register-sub :get-rtc-discoveries
              (fn [db [_]]
                ;;(do (http-post "https://api.github.com" "/gists" @db js/console))
                
                (let [;;disc (reaction (:discoveries @db))
                      discoveries (into [] (map #(-> {:photo-path r/photo} ;;(select-keys (nth discoveries 0) [:photo-path])
                                                     (assoc-in [:message-id] (rand-int 10000))
                                                     (assoc-in [:whisper-id] "A")
                                                     (assoc-in [:status] (str "STR." % "." (rand-int 10000)))
                                                     (assoc-in [:public-key] "A")
                                                     (assoc-in [:name] (str "A." (rand-int 100)))
                                                     ) [0 1 2 3 4 5])) 
                      ]
                  ;;(log/debug :event-str (select-keys (nth discoveries 0) [:photo-path]))
                  (reaction {:discoveries discoveries 
                             :total       (count discoveries)})))
              )

(register-sub :get-rtc-msg
              (fn [db [_]]
                (reaction (get-in db [:rtc :message]))
                ))
