(ns status-im.rtc.handlers
  (:require [re-frame.core :refer [after dispatch enrich]]
            [status-im.utils.utils :refer [first-index]]
            [status-im.utils.handlers :refer [register-handler get-hashtags]]
            [status-im.protocol.core :as protocol]
            [status-im.navigation.handlers :as nav]
            [status-im.components.react :as r]
            [status-im.data-store.discover :as discoveries]
            [status-im.utils.datetime :as time]
            [status-im.utils.random :as random]
            [status-im.components.status :as status]
            [status-im.rtc.js-resources :as js-res]
            [status-im.utils.utils :refer [http-get]]
            [status-im.utils.types :refer [json->clj]]
            [taoensso.timbre :as log]))

(defonce account-creation? (atom false))

(def status
  (when (exists? (.-NativeModules r/react-native))
    (.-Status (.-NativeModules r/react-native))))

(defn create-account [password on-result]
  (when status
    (let [callback (fn [data]
                     (reset! account-creation? false)
                     (on-result data))]
      (swap! account-creation?
             (fn [creation?]
               (if-not creation?
                 (do
                   (.createAccount status password callback)
                   true)
                 false))))))

(defn public-key->address [public-key] ;; just copied from contacts/handlers.cljs
  (let [length         (count public-key)
        normalized-key (case length
                         132 (subs public-key 4)
                         130 (subs public-key 2)
                         128 public-key
                         nil)]
    (when normalized-key
      (subs (.sha3 js/Web3.prototype normalized-key #js {:encoding "hex"}) 26))))

(defn add-contacts [{:keys [chats]} x]
  (doseq [[id {:keys [name photo-path public-key add-chat?
                      dapp? dapp-url dapp-hash]}] x]
    (let [id' (clojure.core/name id)]
      (when-not (chats id')
        (when add-chat?
          (dispatch [:add-chat id' {:name (:en name)}]))
        (dispatch [:add-contacts [{:whisper-identity id'
                                   :address          (public-key->address id')
                                   :name             (:en name)
                                   :photo-path       photo-path
                                   :public-key       public-key
                                   :dapp?            dapp?
                                   :dapp-url         (:en dapp-url)
                                   :dapp-hash        dapp-hash}]])))))

(register-handler :initialize-rtc ;; called from src/status-im/handlers.cljs
                  (fn [db [_]]
                    (let [ABI (.-abi js-res/contract)
                          account (nth (keys (:accounts db)) 0) ;; Getting from key of hash
                          efilter (clj->js {:to (str "0x" account)})
                          contractInst (.at (.contract (.-eth (:web3 db)) ABI) (.-address js-res/contract))
                          eventInst (.logtest contractInst efilter)]
                      (log/debug :initialize-rtc ;;(:accounts db)
                                 (str ",filter=" efilter
                                      ",contract@" (.-address js-res/contract)))
                      ;; CARD-RECEIVE
                      (.watch eventInst (fn [err res]
                                          (let [result (:args (js->clj res :keywordize-keys true))]
                                            (log/debug "rtc-filter:" err "," result
                                                       ",t:" (:from result) "-" (:to result)
                                                       ",m:" (:message result))
                                            (dispatch [:rtc-receive-card
                                                       (-> {:photo-path js-res/photo} ;FAKE
                                                           (assoc-in [:message-id] (rand-int 10000)) ;FAKE
                                                           (assoc-in [:whisper-id] "A") ;FAKE
                                                           (assoc-in [:address] (:from result)) ;Sender address
                                                           (assoc-in [:status] (str (:message result) "." (rand-int 10000)))
                                                           (assoc-in [:name] (str (:from result) "." (rand-int 100)))
                                                           )
                                                       ])
                                            )))

                      (create-account "eeeeee" #(log/debug "crate-account:" %))
                      ;; GETTING CONTACTS FOR RTC
                      #_(http-get "https://gist.githubusercontent.com/tf0054/917efdf08cf3860ee4033c08b7f39231/raw/8383aaa6e00aabe97432ae13ada0c25f1444cb15/contacts.json"
                                  #(let [x (json->clj %)]
                                     (add-contacts db x)
                                     ;;(log/debug "Contacts-get-success" x)
                                     )
                                  #(log/debug "Contacts-get-error" ))
                      
                      (-> db
                          (assoc-in [:rtc :address] account)
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
                        (assoc-in [:rtc :target_name] name)
                        (assoc-in [:rtc :target_addr] address))
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
                          ;;account (nth (keys (:accounts db)) 0)
                          ]

                      (if (nil? defaultAccouint)
                        (do
                          (log/debug :add-card
                                     "msg:" (get-in db [:rtc :message]) ","
                                     "by" (get-in db [:rtc :address])
                                     "to" (get-in db [:rtc :target_addr]))
                          ;; CARD-SEND
                          (.sendEther (get-in db [:rtc :contractInst])
                                      (str "0x" (get-in db [:rtc :target_addr])) ;; Card to(1/2)
                                      (get-in db [:rtc :message]) ;; Card msg(2/2)
                                      (clj->js {:from (str "0x" (get-in db [:rtc :address]))
                                                :gas 50000})
                                      (fn [err res]
                                        (log/debug :add-card-call err (js->clj res))
                                        (if (nil? err)
                                          (log/debug :add-card-call "backed")
                                          (dispatch [:navigate-back]) )
                                        ))
                          )
                        (do
                          (log/debug :add-card (js->clj (.-defaultAccount eth) ;;(.-accounts eth)
                                                        ))
                          (.sendTransaction eth (clj->js {:from  (str "0x" defaultAccouint)
                                                          :to    (get-in db [:rtc :target_addr])
                                                          :value 10000000})
                                            (fn [err res] (log/debug :add-card-call err (js->clj res))))
                          ::db
                          ) )
                      db
                      )))

