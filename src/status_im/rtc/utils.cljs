(ns ^:figwheel-always status-im.rtc.utils
  (:require-macros [status-im.utils.slurp :refer [slurp]])
  (:require [re-frame.core :refer [after dispatch enrich]]
            [status-im.utils.types :refer [json->clj]]
            [status-im.utils.identicon :refer [identicon]]
            [status-im.components.react :as r]
            [status-im.contacts.handlers :refer [public-key->address]]
            [taoensso.timbre :as log]
            )
  )

(defn- getAccount [db]
  (let [current-account-id (:current-account-id db)]
    (get-in db [:accounts current-account-id]))
  )

;;

(defn http-post [url data on-success on-error]
  (-> (.fetch js/window
              url
              (clj->js {:method "POST"
                        :body (.stringify js/JSON (clj->js data))}))
      (.then (fn [response]
               (log/debug "rtc/utils/http-post/Res" (.text response))
               (.text response)))
      (.then (fn [text]
               (let [json (.parse js/JSON text)
                     obj (js->clj json :keywordize-keys true)]
                 (on-success obj))))
      (.catch (or on-error
                  (fn [error]
                    (log/debug "rtc/utils/http-post/Error" (str error)))))))

(defn gist-post [url desc text on-success on-error]
  (http-post url
             {:description desc
              :public "true"
              :files {"log.txt" {:content text}}}
             ;;#(on-success (:html_url %))
             on-success
             on-error))

;; Testing

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

(defn rtc-add-contacts [db x]
  (doseq [[id {:keys [name photo-path public-key address add-chat?
                      dapp? dapp-url dapp-hash]}] x]
    (let [id' (clojure.core/name id)
          chats (:chats db)]
      (when-not (chats id')
        (when add-chat?
          (dispatch [:add-chat id' {:name (:en name)}]))
        (if (= (:public-key (getAccount db)) id')
          (do
            (log/debug "add-contacts/skipped" id')
            (dispatch [:set-account-name (:en name)]))
          (do (log/debug "add-contacts/added" id' (:en name))
              (dispatch [:remove-contact id' #(or true %)]) ;; true 
              (dispatch [:add-contacts [{:whisper-identity id'
                                         :address          (if (nil? address)
                                                             (public-key->address id')
                                                             address)
                                         :name             (:en name)
                                         :photo-path       (if (nil? photo-path)
                                                             (identicon public-key)
                                                             photo-path)
                                         :public-key       public-key
                                         :dapp?            dapp?
                                         :dapp-url         (:en dapp-url)
                                         :dapp-hash        dapp-hash}]]) ))))) )

;; Ethereum

(defn wei2eth [db x]
  (.fromWei (:web3 db) x "ether"))

(defn getBalance [db address func]
  ;;(log/debug :get-balance)
  (let [eth (.-eth (:web3 db))]
    (.getBalance eth address
                 (.-defaultBlock eth)
                 func
                 #_(fn [err res]
                     (log/debug "getBalance" address ":" err "," res)) ) 
    ))

(defn getBlockNumber [db func]
  (let [eth (.-eth (:web3 db))]
    (.getBlockNumber eth 
                     func
                     #_(fn [err res]
                         (log/debug "getBlockNumber" err "," res)) ) ) )

;;web3.net.getPeerCount (callback(error, result){ .

(defn getPeerCount [db func]
  (let [net (.-net (:web3 db))]
    (.getPeerCount net 
                   func
                   #_(fn [err res]
                       (log/debug "getPeerCount" err "," res)) ) ) )

;; 

(defn removePhotoPath [x]
  (dissoc x :photo-path) )

(defn removePhotoPathFromArray [ary]
  (into [] (map removePhotoPath ary)) )

;; React-native

(defonce ReactNative (js/require "react-native"))
(defonce ToastAndroid (.-ToastAndroid ReactNative))

(defn showToast [x]
  (.show ToastAndroid x (.-SHORT ToastAndroid))
  )
