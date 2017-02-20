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

(register-handler :initialize-rtc ;; called from src/status-im/handlers.cljs
                  (fn [db [_]]
                    (let [ABI (.-abi r/contract)
                          account (nth (keys (:accounts db)) 0)
                          contractInst (.at (.contract (.-eth (:web3 db)) ABI) (.-address r/contract))
                          eventInst (.logtest contractInst (clj->js {:to account}))]
                      (log/debug :initialize-rtc (:accounts db) (str ",contract@" (.-address r/contract)))
                      ;; CARD-RECEIVE
                      (.watch eventInst (fn [err res]
                                          (let [result_ (js->clj res :keywordize-keys true)
                                                result (:args result_)]
                                            (log/debug "rtc-filter:" err "," result
                                                       ",t:" (:from result) "-" (:to result)
                                                       ",m:" (:message result))
                                            (dispatch [:rtc-receive-card
                                                       (-> {:photo-path r/photo} ;FAKE
                                                           (assoc-in [:message-id] (rand-int 10000)) ;FAKE
                                                           (assoc-in [:whisper-id] "A") ;FAKE
                                                           (assoc-in [:address] (:from result)) ;Sender address
                                                           (assoc-in [:status] (str (:message result) "." (rand-int 10000)))
                                                           (assoc-in [:name] (str (:from result) "." (rand-int 100)))
                                                           )
                                                       ])
                                            )))
                      (-> db
                          (assoc-in [:rtc :account] account)
                          (assoc-in [:rtc :contractInst] contractInst)
                          ))
                    ) )

(register-handler :rtc-receive-card
                  (fn [db [_ x]]
                    (log/debug :rtc-receive-card x "," (get-in db [:rtc :discoveries]))
                    (assoc-in db [:rtc :discoveries]
                              (if (nil? (get-in db [:rtc :discoveries]))
                                [x]
                                (conj (get-in db [:rtc :discoveries]) x) ))
                    ))

(register-handler :set-card-to
                  (fn [db [_ name address]]
                    (log/debug :set-card-to name address)
                    (-> db
                        (assoc-in [:rtc :name] name)
                        (assoc-in [:rtc :address] address))
                    ))

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
                                     "-" (count (keys (:accounts db))) "," (get-in db [:rtc :message]) "->" (get-in db [:rtc :address]))
                          ;; CARD-SEND
                          (.sendEther (get-in db [:rtc :contractInst])
                                      (str "0x" (get-in db [:rtc :address])) ;; Card target
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
                          (.sendTransaction eth (clj->js {:from  (str "0x" defaultAccouint)
                                                          :to    (get-in db [:rtc :address])
                                                          :value 10000000})
                                            (fn [err res] (log/debug :add-card-call err (js->clj res))))
                          ::db
                          ) )
                      db
                      )))

