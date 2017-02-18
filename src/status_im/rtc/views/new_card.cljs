(ns status-im.rtc.views.new-card
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [status-im.components.react :refer [view
                                                text
                                                image
                                                linear-gradient
                                                touchable-highlight]]
            [status-im.components.text-field.view :refer [text-field]]
            [status-im.utils.identicon :refer [identicon]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar.view :refer [toolbar]]
            [status-im.components.toolbar.actions :as act]
            [status-im.components.toolbar.styles :refer [toolbar-title-container
                                                         toolbar-title-text
                                                         toolbar-background1]]
            [status-im.utils.utils :refer [log http-post]]
            [status-im.components.styles :refer [icon-ok
                                                 button-input-container
                                                 button-input
                                                 color-blue]]
            [status-im.i18n :refer [label]]
            [cljs.spec :as s]
            ;;[status-im.contacts.validations :as v]
            [status-im.contacts.styles :as st]
            [status-im.data-store.contacts :as contacts]
            [status-im.utils.gfycat.core :refer [generate-gfy]]
            [status-im.utils.hex :refer [normalize-hex]]
            [status-im.utils.platform :refer [platform-specific]]
            [taoensso.timbre :as log]))

(def toolbar-title
  [view toolbar-title-container
   [text {:style toolbar-title-text}
    "NEW CARD INPUT"]])

(defn toolbar-actions [account error]  
  [{:image   {:source {:uri :icon_ok_blue}
              :style  icon-ok}
    :handler #(dispatch [:add-card account])
    }])

(defview msgbody-input [error]
  [msg [:get-rtc-msg]
   current-account [:get-current-account]]
  ;; (let [error (when-not (str/blank? whisper-identity)
  ;;              (validation-error-message whisper-identity current-account error))]
  [view button-input-container
   [text-field
    {:error          error
     :error-color    color-blue
     :input-style    st/qr-input
     :value          msg
     :wrapper-style  button-input
     :label          "Message"
     :on-change-text #(dispatch [:add-msg %])}]
   ])

(defview new-card []
  [;; new-msg[:get-rtc-msg]
   error [:get :new-contact-public-key-error]
   account [:get-current-account]]
  [view st/contact-form-container
   [status-bar]
   [toolbar {:background-color toolbar-background1
             :style            (get-in platform-specific [:component-styles :toolbar])
             :nav-action       (act/back #(dispatch [:navigate-back]))
             :title            "NEW CARD INPUT2"
             :actions          (toolbar-actions account error)
             }]
   [view st/form-container
    [msgbody-input error]]
   [view st/address-explication-container
    [text {:style st/address-explication
           :font  :default}
     "Here you can add some explanation"]]])
