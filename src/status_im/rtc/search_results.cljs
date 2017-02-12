(ns status-im.rtc.search-results
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [status-im.utils.listview :refer [to-datasource]]
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
            [status-im.components.drawer.view :refer [open-drawer]]
            [status-im.components.carousel.carousel :refer [carousel]]
            [status-im.components.toolbar.view :refer [toolbar-with-search]]
            [status-im.rtc.views.discover-list-item :refer [discover-list-item]]
            [status-im.rtc.views.popular-list :refer [discover-popular-list]]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.i18n :refer [label]]
            [status-im.rtc.styles :as st]
            [status-im.contacts.styles :as contacts-st]
            [taoensso.timbre :as log]))

(defn render-separator [_ row-id _]
  (list-item [view {:style st/row-separator
                    :key   row-id}]))

(defview rtc-discover []
  [discoveries [:get-rtc-discoveries]]
  (let [datasource (to-datasource discoveries)]
    [view st/discover-tag-container
     [status-bar]
     [touchable-highlight {:style    {:position :absolute}
                           :on-press #(dispatch [:navigate-back])}
      [view (get-in platform-specific [:component-styles :toolbar-nav-action])
       [icon :back {:width  8
                    :height 14}]]]
     
     ;;(log/debug :event-str discoveries) 
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
        (for [name [0 1 2 3 4 5]]
          ^{:key (str "list-rtc-" name)}
          [discover-popular-list {:tag name}]) ]
       )]))
