(ns status-im.rtc.search-results
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            #_[status-im.utils.listview :refer [to-datasource]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.react :refer [view
                                                text
                                                icon
                                                list-view
                                                list-item
                                                touchable-highlight
                                                scroll-view]]
            [status-im.components.toolbar.view :refer [toolbar]]
            [status-im.components.toolbar.actions :as act]
            [status-im.components.drawer.view :refer [open-drawer drawer-view]]
            [status-im.components.carousel.carousel :refer [carousel]]
            [status-im.components.icons.custom-icons :refer [ion-icon]]
            [status-im.components.action-button :refer [action-button
                                                        action-button-item]]
            [status-im.components.styles :refer [color-blue
                                                 create-icon]]
            [status-im.rtc.views.list-item :refer [rtc-list-item]]
            [status-im.rtc.views.popular-list :refer [rtc-popular-list]]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.i18n :refer [label]]
            [status-im.rtc.styles :as st]
            [status-im.contacts.styles :as contacts-st]            
            [taoensso.timbre :as log]
            #_[status-im.android.platform :refer [show-dialog]]
            ))

(defn toolbar-view []
  (toolbar
   {:title              "RTC THANKS CARD"
    :nav-action         (act/hamburger open-drawer)
    }))

(defn contacts-action-button []
  (let [blocknum (subscribe [:get-rtc-blocknumber])]
    [action-button {:button-color color-blue
                    :offset-x     16
                    :offset-y     22
                    :hide-shadow  true
                    :spacing      13}
     [action-button-item
      {:title       "NEW CARD"
       :buttonColor :#9b59b6
       :onPress     #(dispatch [:navigate-to :rtc-contact])}
      [ion-icon {:name  :md-create
                 :style create-icon}]]
     #_[action-button-item
        {:title       "GET INFO"
         :buttonColor :#9bf9b6
         :onPress     #(dispatch [:get-ethinfo])}
        [ion-icon {:name  :md-create
                   :style create-icon}]]
     [action-button-item
      {:title       "DEBUG(ETH)"
       :buttonColor :#9bf9b6
       :onPress     #(do
                       (dispatch [:get-ethinfo])
                       #_(show-dialog {:title (str @blocknum)
                                       :options '()
                                       :callback (fn [pos txt]
                                                   (log/debug "dialog" pos "," txt))} ))}
      [ion-icon {:name  :md-create
                 :style create-icon}]]
     ]))

(defn render-separator [_ row-id _]
  (list-item [view {:style st/row-separator
                    :key   row-id}]))

(defview rtc-main []
  [discoveries [:get-rtc-card]]
  ;; CARD-RECEIVE
  (do (dispatch [:rtc-start-watch])
      ;;
      [drawer-view
       [view st/rtc-tag-container
        [toolbar-view]
        (if (empty? discoveries)
          [view (merge st/empty-view {:margin-top 55})
           ;; todo change icon
           [icon :group_big contacts-st/empty-contacts-icon]
           [text {:style contacts-st/empty-contacts-text}
            "NO DATA"
            ;; (label :t/no-statuses-found)
            ]]
          [scroll-view (merge {:align-items    :stretch
                               :flex-firection :column
                               :padding-left     16
                               :padding-right     16
                               :margin-top     55}
                              {:keyboardShouldPersistTaps true
                               :bounces                   false})
           (for [name [0]]
             ^{:key (str "list-rtc-" name)}
             [rtc-popular-list {:tag name}]) ]
          )
        [contacts-action-button]
        ]]) )
