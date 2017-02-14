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
                    (let [ABI          (.parse js/JSON "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"string\"}],\"name\":\"sendEther\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"address\"}],\"name\":\"sendEtherRaw\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"to\",\"type\":\"address\"}],\"name\":\"delegate\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"winningProposal\",\"outputs\":[{\"name\":\"winningProposal\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"voter\",\"type\":\"address\"}],\"name\":\"giveRightToVote\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"proposal\",\"type\":\"uint8\"}],\"name\":\"vote\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"payable\":true,\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"message\",\"type\":\"string\"}],\"name\":\"logtest\",\"type\":\"event\"}]"
                                               ;;"[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"string\"}],\"name\":\"sendEther\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"address\"}],\"name\":\"sendEtherRaw\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"to\",\"type\":\"address\"}],\"name\":\"delegate\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"winningProposal\",\"outputs\":[{\"name\":\"winningProposal\",\"type\":\"uint8\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"voter\",\"type\":\"address\"}],\"name\":\"giveRightToVote\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"proposal\",\"type\":\"uint8\"}],\"name\":\"vote\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"payable\":true,\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"message\",\"type\":\"string\"}],\"name\":\"logtest\",\"type\":\"event\"}]"
                                               )
                          ;; https://gist.github.com/b31981768dc22390f8b7cbda283ab7
                          contractAddr "0x7e61f98158f24ac4bc498a9ab4e9706dbe3ba315"
                          contract     (.contract (.-eth (:web3 db)) ABI)
                          contractInst (.at contract contractAddr)]
                      (let [eth (.-eth (:web3 db))
                            defaultAccouint (.-defaultAccount eth)]
                        
                        (if (nil? defaultAccouint)
                          (do
                            (log/debug :add-card "defaultAccouint is nil. getting from app-db" (nth (keys (:accounts db)) 0)
                                       "-" (keys (:accounts db)))
                            (.sendEtherRaw contractInst
                                           "0x39c4b70174041ab054f7cdb188d270cc56d90da8"
                                           (clj->js {:from (str "0x" (nth (keys (:accounts db)) 0))
                                                     :gas 50000})
                                           (fn [err res] (log/debug :add-card-call err (js->clj res))))
                            #_(.sendTransaction eth (clj->js {:from (str "0x" (nth (keys (:accounts db)) 0))
                                                              :to    contractAddr
                                                              :value 10000000})
                                                (fn [err res] (log/debug :add-card-call err (js->clj res)))))  
                          (do
                            (log/debug :add-card (js->clj (.-defaultAccount eth) ;;(.-accounts eth)
                                                          ))
                            (.sendTransaction eth (clj->js {:from (str "0x" defaultAccouint)
                                                            :to    contractAddr
                                                            :value 10000000})
                                              (fn [err res] (log/debug :add-card-call err (js->clj res)))))
                          )
                        
                        #_(.sendEtherRaw contractInst
                                         "0x39c4b70174041ab054f7cdb188d270cc56d90da8"
                                         (clj->js {:gas 50000})
                                         (fn [err res] (log/debug :add-card-call err (js->clj res))))
                        db
                        ))))

(comment
  (register-handler :broadcast-status
                    (u/side-effect!
                     (fn [{:keys [current-public-key web3 current-account-id accounts contacts]}
                          [_ status hashtags]]
                       (let [{:keys [name photo-path]} (get accounts current-account-id)
                             message-id (random/id)
                             message    {:message-id message-id
                                         :from       current-public-key
                                         :payload    {:message-id message-id
                                                      :status     status
                                                      :hashtags   (vec hashtags)
                                                      :profile    {:name          name
                                                                   :profile-image photo-path}}}]
                         (doseq [id (identities contacts)]
                           (protocol/send-status!
                            {:web3    web3
                             :message (assoc message :to id)}))
                         (dispatch [:status-received message])))))

  (register-handler :status-received
                    (u/side-effect!
                     (fn [{:keys [discoveries] :as db} [_ {:keys [from payload]}]]
                       (when (and (not (discoveries/exists? (:message-id payload)))
                                  (not (get discoveries (:message-id payload))))
                         (let [{:keys [message-id status hashtags profile]} payload
                               {:keys [name profile-image]} profile
                               discover {:message-id   message-id
                                         :name         name
                                         :photo-path   profile-image
                                         :status       status
                                         :whisper-id   from
                                         :tags         (map #(hash-map :name %) hashtags)
                                         :created-at   (time/now-ms)}]
                           (dispatch [:add-discover discover]))))))

  (register-handler :start-requesting-discoveries
                    (fn [{:keys [request-discoveries-timer] :as db}]
                      (when request-discoveries-timer
                        (js/clearInterval request-discoveries-timer))
                      (dispatch [:request-discoveries])
                      (assoc db :request-discoveries-timer
                             (js/setInterval #(dispatch [:request-discoveries])
                                             (* request-discoveries-interval-s 1000)))))

  (register-handler :request-discoveries
                    (u/side-effect!
                     (fn [{:keys [current-public-key web3 contacts]}]
                       (doseq [id (identities contacts)]
                         (when-not (protocol/message-pending? web3 :discoveries-request id)
                           (protocol/send-discoveries-request!
                            {:web3    web3
                             :message {:from       current-public-key
                                       :to         id
                                       :message-id (random/id)}}))))))

  (register-handler :discoveries-send-portions
                    (u/side-effect!
                     (fn [{:keys [current-public-key contacts web3]} [_ to]]
                       (when (get contacts to)
                         (protocol/send-discoveries-response!
                          {:web3        web3
                           :discoveries (discoveries/get-all :asc)
                           :message     {:from current-public-key
                                         :to   to}})))))

  (register-handler :discoveries-request-received
                    (u/side-effect!
                     (fn [_ [_ {:keys [from]}]]
                       (dispatch [:discoveries-send-portions from]))))

  (register-handler :discoveries-response-received
                    (u/side-effect!
                     (fn [{:keys [discoveries contacts]} [_ {:keys [payload from]}]]
                       (when (get contacts from)
                         (when-let [data (:data payload)]
                           (doseq [{:keys [message-id] :as discover} data]
                             (when (and (not (discoveries/exists? message-id))
                                        (not (get discoveries message-id)))
                               (let [discover (assoc discover :created-at (time/now-ms))]
                                 (dispatch [:add-discover discover])))))))))

  (defn add-discover
    [db [_ discover]]
    (assoc db :new-discover discover))

  (defn save-discover!
    [{:keys [new-discover]} _]
    (discoveries/save new-discover))

  (defn reload-tags!
    [db _]
    (assoc db :tags (discoveries/get-all-tags)
           :discoveries (->> (discoveries/get-all :desc)
                             (map (fn [{:keys [message-id] :as discover}]
                                    [message-id discover]))
                             (into {}))))

  (register-handler :add-discover
                    (-> add-discover
                        ((after save-discover!))
                        ((enrich reload-tags!))))

  (register-handler
   :remove-old-discoveries!
   (u/side-effect!
    (fn [_ _]
      (discoveries/delete :created-at :asc 1000 200))))
  )
