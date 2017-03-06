(ns status-im.rtc.views.list-item
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [status-im.components.react :refer [view text image touchable-highlight]]
            [status-im.rtc.styles :as st]
            [status-im.rtc.utils :as utils]
            [status-im.components.status-view.view :refer [status-view]]
            [status-im.utils.gfycat.core :refer [generate-gfy]]
            [status-im.utils.identicon :refer [identicon]]
            [status-im.components.chat-icon.screen :as ci]
            [status-im.utils.hex :as h]
            [status-im.utils.platform :refer [platform-specific]]
            [taoensso.timbre :as log]))

(defview rtc-list-item [{{:keys [address
                                 ;;public-key
                                 photo-path
                                 ;;whisper-idcc
                                 message-id
                                 status]
                          :as   message}                   :message
                         show-separator?                   :show-separator?
                         }]
  [contacts [:get :contacts]]
  (let [item-style (get-in platform-specific [:component-styles :discover :item])
        from        (let [a (h/normalize-hex address)
                          x (nth (filter #(= (:address (nth % 1)) a)
                                         #_#(let [concat (nth % 1)]
                                              (and (= (:address concat) a)
                                                   (:dapps? concat)))
                                         contacts)
                                 0)]
                      (doall (map #(log/debug "rtc-list-item-d: " (:address (nth % 1))) contacts))
                      (if (empty? x)
                        (do
                          (log/debug "NOT-FOUND" address)
                          {:name address
                           :whisper-identity nil})
                        (do
                          (log/debug "FOUND" (nth x 1))
                          (nth x 1))) )
        name (:name from)
        whisper-id (:whisper-identity from)
        ]
    (log/debug "rtc-list-item:" (utils/removePhotoPath message))
    [view
     [view st/popular-list-item
      [view st/popular-list-item-name-container
       
       [text {:style           st/popular-list-item-name
              :font            :medium
              :number-of-lines 1}
        name]
       [status-view {:id     message-id
                     :style  (:status-text item-style)
                     :status status}]]
      ;;
      [view (merge st/popular-list-item-avatar-container
                   (:icon item-style))
       [touchable-highlight {:on-press (if (nil? whisper-id)
                                         #(log/debug "rtc-list-item" "Cannot start-chat!")
                                         #(dispatch [:start-chat whisper-id]) ) }
        [view
         [ci/chat-icon photo-path
          {:size 36}]]]]]
     (when show-separator?
       [view st/separator])]))
