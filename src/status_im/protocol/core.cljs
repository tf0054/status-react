(ns status-im.protocol.core
  (:require status-im.protocol.message
            [status-im.protocol.web3.utils :as u]
            [status-im.protocol.web3.filtering :as f]
            [status-im.protocol.web3.delivery :as d]
            [taoensso.timbre :refer-macros [debug]]
            [status-im.protocol.validation :refer-macros [valid?]]
            [status-im.protocol.web3.utils :as u]
            [status-im.protocol.chat :as chat]
            [status-im.protocol.group :as group]
            [status-im.protocol.listeners :as l]
            [status-im.protocol.encryption :as e]
            [status-im.protocol.discoveries :as discoveries]
            [cljs.spec :as s]
            [status-im.utils.hex :as h]
            [status-im.utils.random :as random]))

;; user
(def send-message! chat/send!)
(def send-seen! chat/send-seen!)
(def send-clock-value-request! chat/send-clock-value-request!)
(def send-clock-value! chat/send-clock-value!)
(def reset-pending-messages! d/reset-pending-messages!)

;; group
(def start-watching-group! group/start-watching-group!)
(def stop-watching-group! group/stop-watching-group!)
(def send-group-message! group/send!)
(def invite-to-group! group/invite!)
(def update-group! group/update-group!)
(def remove-from-group! group/remove-identity!)
(def add-to-group! group/add-identity!)
(def leave-group-chat! group/leave!)

;; encryption
;; todo move somewhere, encryption functions shouldn't be there
(def new-keypair! e/new-keypair!)

;; discoveries
(def watch-user! discoveries/watch-user!)
(def stop-watching-user! discoveries/stop-watching-user!)
(def contact-request! discoveries/contact-request!)
(def broadcast-profile! discoveries/broadcast-profile!)
(def send-status! discoveries/send-status!)
(def send-discoveries-request! discoveries/send-discoveries-request!)
(def send-discoveries-response! discoveries/send-discoveries-response!)
(def update-keys! discoveries/update-keys!)

(def message-pending? d/message-pending?)

;; initialization
(s/def ::rpc-url string?)
(s/def ::identity string?)
(s/def :message/chat-id string?)
(s/def ::group (s/keys :req-un [:message/chat-id :message/keypair]))
(s/def ::groups (s/* ::group))
(s/def ::callback fn?)
(s/def ::contact (s/keys :req-un [::identity :message/keypair]))
(s/def ::contacts (s/* ::contact))
(s/def ::profile-keypair :message/keypair)
(s/def ::options
  (s/merge
   (s/keys :req-un [::rpc-url ::identity ::groups ::profile-keypair
                    ::callback :discoveries/hashtags ::contacts])
   ::d/delivery-options))

(def stop-watching-all! f/remove-all-filters!)
(def reset-all-pending-messages! d/reset-all-pending-messages!)

(defn init-whisper!
  [{:keys [rpc-url identity groups callback
           contacts profile-keypair pending-messages]
    :as   options}]
  {:pre [(valid? ::options options)]}
  (debug :init-whisper)
  (stop-watching-all!)
  (d/reset-all-pending-messages!)
  (let [web3             (u/make-web3 rpc-url)
        listener-options {:web3     web3
                          :identity identity}]
    ;; start listening to groups
    (doseq [{:keys [chat-id keypair]} groups]
      (f/add-filter!
       web3
       {:topics [chat-id]}
       (l/message-listener (assoc listener-options :callback callback
                                  :keypair keypair))))
    ;; start listening to user's inbox
    (f/add-filter!
     web3
     {:to     identity
      :topics [f/status-topic]}
     (l/message-listener (assoc listener-options :callback callback)))
    ;; start listening to profiles
    (doseq [{:keys [identity keypair]} contacts]
      (watch-user! {:web3     web3
                    :identity identity
                    :keypair  keypair
                    :callback callback}))
    (d/set-pending-mesage-callback! callback)
    (let [online-message #(discoveries/send-online!
                           {:web3    web3
                            :message {:from       identity
                                      :message-id (random/id)
                                      :keypair    profile-keypair}})]
      (d/run-delivery-loop!
       web3
       (assoc options :online-message online-message)))
    (doseq [pending-message pending-messages]
      (d/add-prepeared-pending-message! web3 pending-message))
    web3))

;; UNUSED
(defn init-rtc!
  [web3 adr]
  (debug :init-rtc)
  (f/add-rtc-filter!
   web3
   {:address adr
    ;;:topics  [(.sha3 web3 "logtest(address,uint,string)")]
    }
   (fn [err res]
     (debug "rtc-filter:" err "," (js->clj res) )
     (debug "rtc-filter:slipt:"
            (let [aryRes (map (fn[x] (str (clojure.string/join "" x)))
                              (partition 64 (.split (h/normalize-hex (get (js->clj res) "data")) "")))]
              (str aryRes ","
                   (str "0x" (.substr (nth aryRes 0) 24)) "," ;;From
                   (.toDecimal web3 (str "0x" (nth aryRes 1))) "," ;; Value
                   (.toAscii web3 (str "0x" (nth aryRes 4))) ) ;; DATA(ASCII)
              )
            )
     )))
