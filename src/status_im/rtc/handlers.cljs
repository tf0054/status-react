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

#_(register-handler :init-discoveries
                    (fn [db _]
                      (-> db
                          (assoc :tags [])
                          (assoc :discoveries {}))))

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

(register-handler :add-card
                  (fn [db _]
                    (log/debug :add-card)
                    ;; https://gist.github.com/b31981768dc22390f8b7cbda283ab7
                    (let [ABI r/abi     
                          contract     (.contract (.-eth (:web3 db)) ABI)
                          contractAddr "0x7e61f98158f24ac4bc498a9ab4e9706dbe3ba315"
                          contractInst (.at contract contractAddr)
                          eth (.-eth (:web3 db))
                          defaultAccouint (.-defaultAccount eth)]
                      
                      (if (nil? defaultAccouint)
                        (do
                          (log/debug :add-card "defaultAccouint is nil. getting from app-db" (nth (keys (:accounts db)) 0)
                                     "-" (count (keys (:accounts db))))
                          (.sendEtherRaw contractInst
                                         "0x39c4b70174041ab054f7cdb188d270cc56d90da8"
                                         (clj->js {:from (str "0x" (nth (keys (:accounts db)) 0))
                                                   :gas 50000})
                                         (fn [err res] (log/debug :add-card-call err (js->clj res)))))
                        (do
                          (log/debug :add-card (js->clj (.-defaultAccount eth) ;;(.-accounts eth)
                                                        ))
                          (.sendTransaction eth (clj->js {:from (str "0x" defaultAccouint)
                                                          :to    contractAddr
                                                          :value 10000000})
                                            (fn [err res] (log/debug :add-card-call err (js->clj res))))) )
                      db
                      )))


