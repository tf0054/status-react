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

(defn on-add-card [id]
  (log/debug :on-add-contact id)
  #_(http-post "get-contacts-by-address" {:addresses [id]}
               (fn [{:keys [contacts]}]
                 (if (> (count contacts) 0)
                   (let [{:keys [whisper-identity]} (first contacts)
                         contact {:name             (generate-gfy)
                                  :address          id
                                  :photo-path       (identicon whisper-identity)
                                  :whisper-identity whisper-identity}]
                     (if (contacts/exists? whisper-identity)
                       (dispatch [:add-pending-contact whisper-identity])
                       (dispatch [:add-new-contact contact])))
                   (dispatch [:set :new-contact-public-key-error (label :t/unknown-address)]))))  
  (dispatch [:add-card {:name             (generate-gfy)
                        :photo-path       (identicon id)
                        :whisper-identity id}]))

(defn toolbar-actions [new-contact-identity account error]  
  [{:image   {:source {:uri :icon_ok_blue}
              :style  icon-ok}
    :handler #(when true
                (on-add-card new-contact-identity))}])

(defview contact-whisper-id-input [whisper-identity error]
  [current-account [:get-current-account]]
  ;; (let [error (when-not (str/blank? whisper-identity)
  ;;              (validation-error-message whisper-identity current-account error))]
  [view button-input-container
   [text-field
    {:error          error
     :error-color    color-blue
     :input-style    st/qr-input
     :value          whisper-identity
     :wrapper-style  button-input
     :label          (label :t/public-key)
     :on-change-text #(do
                        (dispatch [:set-in [:new-contact-identity] %])
                        (dispatch [:set :new-contact-public-key-error nil]))}]
   ])


(defview new-card []
  [new-contact-identity [:get :new-contact-identity]
   error [:get :new-contact-public-key-error]
   account [:get-current-account]]
  [view st/contact-form-container
   [status-bar]
   [toolbar {:background-color toolbar-background1
             :style            (get-in platform-specific [:component-styles :toolbar])
             :nav-action       (act/back #(dispatch [:navigate-back]))
             :title            "NEW CARD INPUT2"
             :actions          (toolbar-actions new-contact-identity account error)}]
   [view st/form-container
    [contact-whisper-id-input new-contact-identity error]]
   [view st/address-explication-container
    [text {:style st/address-explication
           :font  :default}
     (label :t/address-explication)]]])
