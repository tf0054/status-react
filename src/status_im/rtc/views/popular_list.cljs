(ns status-im.rtc.views.popular-list
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [status-im.components.react :refer [view
                                       list-view
                                       list-item
                                       touchable-highlight
                                       text]]
   [status-im.rtc.styles :as st]
   [status-im.utils.listview :refer [to-datasource]]
   [status-im.rtc.views.list-item :refer [rtc-list-item]]
   [status-im.utils.platform :refer [platform-specific]]
   [taoensso.timbre :as log]))

(defview discover-popular-list [{:keys [tag]}]
  [discoveries [:get-rtc-discoveries] ]
  [view (merge st/popular-list-container
               (get-in platform-specific [:component-styles :discover :popular]))
   [view st/row
    [view (get-in platform-specific [:component-styles :discover :tag])
     #_[touchable-highlight {:on-press #(do (dispatch [:set :discover-search-tags [tag]])
                                            (dispatch [:navigate-to :discover-search-results]))}
        [view
         [text {:style st/tag-name
                :font  :medium}
          (str " #" (name tag))]]]
     [view [text {:style st/tag-name
                  :font  :medium}
            (str " #" tag "#")]] ]
    ]
   (let [disc (:discoveries discoveries)]
     ;; (log/debug "popular0: " discoveries)
     (for [[i {:keys [message-id] :as discover}]
           (map-indexed vector disc)]
       (do (log/debug "popular: " i message-id)
           ^{:key (str "message-rtc-" message-id)}
           [rtc-list-item {:message         discover
                           :show-separator? (not= (inc i) (count disc))}] )
       ))])
