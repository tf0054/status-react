(ns status-im.rtc.views.discover-list-item
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [status-im.components.react :refer [view text image touchable-highlight]]
            [status-im.rtc.styles :as st]
            [status-im.components.status-view.view :refer [status-view]]
            [status-im.utils.gfycat.core :refer [generate-gfy]]
            [status-im.utils.identicon :refer [identicon]]
            [status-im.components.chat-icon.screen :as ci]
            [status-im.utils.platform :refer [platform-specific]]
            [taoensso.timbre :as log]))

(defview discover-list-item [{{:keys [name
                                      photo-path
                                      whisper-id
                                      message-id
                                      status]
                               :as   message}                   :message
                              show-separator?                   :show-separator?
                              }]
  [contacts [:get :contacts]]
  #_[{contact-name       :name
      contact-photo-path :photo-path} [:get-in [:contacts whisper-id]]]
  (let [item-style (get-in platform-specific [:component-styles :discover :item])]
    (log/debug "Taoensso: " name) 
    [view
     [view st/popular-list-item
      [view st/popular-list-item-name-container
       
       [text {:style           st/popular-list-item-name
              :font            :medium
              :number-of-lines 1}
        (str name ":" whisper-id)]
       [status-view {:id     message-id
                     :style  (:status-text item-style)
                     :status status}]]
      ;;
      [view (merge st/popular-list-item-avatar-container
                   (:icon item-style))
       [touchable-highlight {:on-press #(dispatch [:start-chat whisper-id])}
        [view
         [ci/chat-icon photo-path
          {:size 36}]]]]]
     (when show-separator?
       [view st/separator])]))
