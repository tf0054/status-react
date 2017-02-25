(ns status-im.rtc.views.new-card
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [status-im.components.react :refer [view
                                                text
                                                text-input
                                                image
                                                linear-gradient
                                                touchable-highlight]]
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
            [status-im.rtc.styles-c :as st]
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
    :handler #(do (dispatch [:regist-card
                             (fn [] (log/debug :regist-card-success))
                             (fn [] (log/debug :regist-card-fail))])
                  (dispatch [:clear-msg]))
    }])

(defn- replaceReturn [x]
  (clojure.string/join "_"
                       (clojure.string/split x "\n")) )

(defview msgbody-input [error]
  [msg [:get-rtc-msg]
   current-account [:get-current-account]]
  [view button-input-container
   [text-input {:error          error
                :error-color    color-blue
                ;;:value          msg
                :editable true
                :style {:marginLeft        16
                        :height            100
                        :width             300
                        :alignItems        :center
                        :justifyContent    :center
                        :borderBottomWidth 2}
                :wrapper-style  button-input
                :placeholder    "Message here"
                :max-length     240
                :multiline      true
                :on-submit-editing #(let [text (.-text (.-nativeEvent %))]
                                      (log/debug "submitediting" (replaceReturn text))
                                      (dispatch [:add-msg text]))
                }]
   ])

(defview new-card []
  [error [:get :new-contact-public-key-error]
   account [:get-current-account]]
  [view st/contact-form-container
   ;;[status-bar]
   [toolbar {:background-color toolbar-background1
             :style            (get-in platform-specific [:component-styles :toolbar])
             :nav-action       (act/back #(dispatch [:navigate-back]))
             :title            "-NEW CARD INPUT-"
             :actions          (toolbar-actions account error)
             }] 
   [view st/form-container
    [msgbody-input error]]
   [view st/address-explication-container
    [text {:style st/address-explication
           :font  :default}
     (str (:name account) "\n" (:address account) "\n" "Here you can add some explanation")]]])
