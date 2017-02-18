(ns status-im.rtc.handlers
  (:require [re-frame.core :refer [after dispatch enrich]]
            [status-im.utils.utils :refer [first-index]]
            [status-im.utils.handlers :refer [register-handler get-hashtags]]
            [status-im.protocol.core :as protocol]
            [status-im.navigation.handlers :as nav]
            [status-im.data-store.discover :as discoveries]
            [status-im.utils.handlers :as u]
            [status-im.utils.datetime :as time]
            [status-im.utils.random :as random]
            [status-im.rtc.js-resources :as r]
            [taoensso.timbre :as log]))

(def request-discoveries-interval-s 600)

(defn identities [contacts]
  (->> (map second contacts)
       (remove (fn [{:keys [dapp? pending]}]
                 (or pending dapp?)))
       (map :whisper-identity)))

(defmethod nav/preload-data! :discover
  [db _]
  (-> db
      (assoc-in [:toolbar-search :show] nil)
      (assoc :tags (discoveries/get-all-tags))
      (assoc :discoveries (->> (discoveries/get-all :desc)
                               (map (fn [{:keys [message-id] :as discover}]
                                      [message-id discover]))
                               (into {})))))

(register-handler :add-msg
                  (fn [db [_ msg]]
                    (log/debug :add-msg msg)
                    (assoc-in db [:rtc :message] msg)
                    ))

(register-handler :remove-msg
                  (fn [db _]
                    (log/debug :remove-msg)
                    (assoc-in db [:rtc :message] "")
                    ))

(register-handler :add-card
                  (fn [db _]
                    (log/debug :add-card)
                    ;; https://gist.github.com/b31981768dc22390f8b7cbda283ab7
                    (let [eth (.-eth (:web3 db))
                          defaultAccouint (.-defaultAccount eth)
                          account (nth (keys (:accounts db)) 0)
                          ]

                      (if (nil? defaultAccouint)
                        (do
                          (log/debug :add-card "defaultAccouint is nil. getting from app-db" account
                                     "-" (count (keys (:accounts db))) "," (get-in db [:rtc :message]))
                          ;; CARD-SEND
                          (.sendEther (get-in db [:rtc :contractInst])
                                      (.-address r/contract) ;; Card target
                                      (get-in db [:rtc :message]) ;; Card msg
                                      (clj->js {:from (str "0x" account)
                                                :gas 50000})
                                      (fn [err res]
                                        (log/debug :add-card-call err (js->clj res))
                                        (if (nil? err)
                                          (dispatch [:navigate-back])
                                          (log/debug :add-card-call "backed") )
                                        ))
                          ;;(assoc-in db [:rtc :message] "")
                          )
                        (do
                          (log/debug :add-card (js->clj (.-defaultAccount eth) ;;(.-accounts eth)
                                                        ))
                          (.sendTransaction eth (clj->js {:from (str "0x" defaultAccouint)
                                                          :to    (get r/contract "address")
                                                          :value 10000000})
                                            (fn [err res] (log/debug :add-card-call err (js->clj res))))
                          ::db
                          ) )
                      db
                      )))

