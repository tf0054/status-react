(ns status-im.rtc.handlers
  (:require [re-frame.core :refer [after dispatch enrich]]
            [status-im.utils.utils :refer [first-index]]
            [status-im.utils.handlers :refer [register-handler get-hashtags]]
            [status-im.protocol.core :as protocol]
            [status-im.navigation.handlers :as nav]
            [status-im.data-store.discover :as discoveries]
            [status-im.utils.datetime :as time]
            [status-im.utils.random :as random]
            [status-im.components.status :as status]
            [status-im.rtc.js-resources :as js-res]
            [status-im.rtc.utils :as utils]
            [status-im.utils.utils :refer [http-get]]
            [status-im.utils.types :refer [json->clj]]
            [taoensso.timbre :as log]))

(defn- rtcFilterCallback [err res]
  (let [result (:args (js->clj res :keywordize-keys true))]
    (log/debug "rtc-card-receive:" err "," result
               ",t:" (:from result) "->" (:to result)
               ",m:" (:message result) "@" (:block result) )
    (if (nil? result)
      (do
        (log/debug "ERR" result ".")
        ;;(.stopWatching eventInst)
        (dispatch [:rtc-stop-watch]))
      (dispatch [:rtc-receive-card
                 (-> {:photo-path js-res/photo} ;FAKE
                     (assoc-in [:message-id] (:block result))
                     (assoc-in [:address] (:from result)) ;Sender address
                     (assoc-in [:status] (str (:message result))) ;Message body
                     (assoc-in [:name] (str (:from result)))
                     )
                 ]) )
    ))

(register-handler :rtc-start-watch
                  (fn [db [_]]
                    (let [contractInst (get-in db [:rtc :contractInst])
                          address (:address (utils/getAccount db))]
                      (if (nil? (get-in db [:rtc :filterInst]))
                        (let [efilter (clj->js {:to (str "0x" address)})
                              eventInst (.logtest contractInst efilter)
                              filterInst (.watch eventInst rtcFilterCallback)]
                          (-> db
                              (assoc-in [:rtc :filterInst] filterInst)))
                        (do
                          (log/debug "Filter exists.")
                          db) ))
                    ))

(register-handler :rtc-stop-watch
                  (fn [db [_]]
                    (let [filterInst (get-in db [:rtc :filterInst])]
                      (.stopWatching filterInst)
                      (-> db
                          (assoc-in [:rtc :filterInst] nil))
                      )))

(register-handler :initialize-rtc ;; called from src/status-im/handlers.cljs
                  (fn [db [_]]
                    (let [ABI (.-abi js-res/contract)
                          address (:address (utils/getAccount db))
                          contractInst (.at (.contract (.-eth (:web3 db)) ABI) (.-address js-res/contract))
                          ]
                      (log/debug :initialize-rtc ;;(:accounts db)
                                 (str ",filter=" (str "0x" address) 
                                      ",contract@" (.-address js-res/contract)))

                      ;; CRREATE NEW ACCOUT
                      #_(utils/create-account "eeeeee" #(utils/gist-post "https://api.github.com/gists"
                                                                         "desc test2"
                                                                         (pr-str %)
                                                                         (fn [x] (log/debug "s:" x))
                                                                         (fn [x] (log/debug "f:" x))) )

                      ;; GETTING CONTACTS FOR RTC
                      (http-get (str "https://gist.githubusercontent.com/tf0054"
                                     "/917efdf08cf3860ee4033c08b7f39231/raw/e06d133c225f238f2af510221eb7b5a000e94472"
                                     "/contacts.json")
                                (fn [response]
                                  (.then response ;; response is promise - https://mzl.la/2nmBPzs
                                         #(let [x (json->clj %)]
                                            (utils/add-contacts db x)
                                            ;; (log/debug "Contacts-get-success" x)
                                            )))
                                #(log/debug "Contacts-get-error" %))
                      (-> db
                          (assoc-in [:rtc :contractInst] contractInst) ))
                    ) )

(register-handler :rtc-receive-card
                  (fn [db [_ x]]
                    (let [message-id (:message-id x)
                          cards (get-in db [:rtc :cards])
                          exists-cards (into (hash-set) (map #(:message-id %) cards))]
                      (log/debug :rtc-receive-card (utils/removePhotoPath x) "," exists-cards ","
                                 (utils/removePhotoPathFromArray cards))
                      (if (contains? exists-cards message-id)
                        (do (log/debug "Dupricated card found:" message-id)
                            db)
                        (assoc-in db [:rtc :cards]
                                  (if (nil? cards)
                                    [x]
                                    (conj cards x) ))))
                    ))

(register-handler :set-card-to
                  (fn [db [_ name address]]
                    (log/debug :set-card-to name address)
                    (-> db
                        (assoc-in [:rtc :target_name] name)
                        (assoc-in [:rtc :target_addr] address))
                    ))

(register-handler :add-msg
                  (fn [db [_ msg]]
                    ;;(log/debug :add-msg msg)
                    (assoc-in db [:rtc :message] msg)
                    ))

(register-handler :clear-msg
                  (fn [db _]
                    (log/debug :clear-msg)
                    (assoc-in db [:rtc :message] "")
                    ))

(register-handler :regist-card
                  (fn [db  [_ success fail]]
                    (let [eth (.-eth (:web3 db))
                          address (:address (utils/getAccount db))]
                      (log/debug :regist-card
                                 "msg:" (get-in db [:rtc :message]) ","
                                 "by" address "to" (get-in db [:rtc :target_addr]))
                      ;; CARD-SEND
                      (.sendEther (get-in db [:rtc :contractInst])
                                  (str "0x" (get-in db [:rtc :target_addr])) ;; Card to(1/2)
                                  (get-in db [:rtc :message]) ;; Card msg(2/2)
                                  (clj->js {:from (str "0x" address)
                                            :gas 2000000})
                                  (fn [err res]
                                    (log/debug :add-card-call err (js->clj res))
                                    (if (nil? err)
                                      fail
                                      success)
                                    ))
                      #_(do
                          (log/debug :add-card (js->clj (.-defaultAccount eth) ;;(.-accounts eth)
                                                        ))
                          (.sendTransaction eth (clj->js {:from  (str "0x" defaultAccouint)
                                                          :to    (get-in db [:rtc :target_addr])
                                                          :value 10000000})
                                            (fn [err res] (log/debug :add-card-call err (js->clj res))))
                          )
                      (dispatch [:navigate-to :rtc]) 
                      db
                      )))

(register-handler :get-ethinfo
                  (fn [db _]
                    (let [x (utils/getAccount db)
                          adr (str "0x" (:address x))
                          rtc "0x39c4B70174041AB054f7CDb188d270Cc56D90da8"]
                      ;;
                      #_(log/debug "filterInst" (get-in db [:rtc :filterInst]))
                      (log/debug "public-key" (:public-key x))
                      ;;
                      (utils/getBalance db adr
                                        (fn [err res]
                                          (log/debug "getBalance(" adr ")" err ","
                                                     (utils/wei2eth db res)
                                                     )))
                      (utils/getBalance db rtc
                                        (fn [err res]
                                          (log/debug "getBalance(" rtc ")" err ","
                                                     (utils/wei2eth db res)
                                                     )))
                      (utils/getPeerCount db
                                          (fn [err res]
                                            (utils/showToast (str "Peers:" res))
                                            (log/debug "getPeerCount" err "," res)))
                      (utils/getBlockNumber db
                                            (fn [err res]
                                              (dispatch [:set-blocknumber res] )
                                              (utils/showToast (str "BlockNumber:" res))
                                              (log/debug "getBlockNumber" err "," res)))
                      )
                    db
                    ))

(register-handler :set-account-name
                  (fn [db [_ name]]
                    (let [current-account-id (:current-account-id db)]
                      (-> db
                          (assoc-in [:accounts current-account-id :name] name))
                      )))

(register-handler :set-blocknumber
                  (fn [db [_ num]]
                    #_(log/debug :set-blocknumber)
                    (-> db
                        (assoc-in [:rtc :blockNumer] num))
                    ))
