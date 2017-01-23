(ns osdb-api.core
  #?@ (:cljs 
       [(:require-macros [cljs.core.async.macros :refer [go]])
        (:require [cljs-http.client :as cljs-http]
                  [cljs.core.async :refer [<!]]
                  [osdb-api.xml :as x])]
       :clj
       [(:require [clj-http.client :as clj-http]
                  [osdb-api.xml :as x]
                  [clojure.xml :as cxml])])
  )

(defn greet []
  #? (:clj (println "Hey, clj dude!")
      :cljs (println "Hullo, cljs pal!")))

(def protocol "http://")
(def hostname "api.opensubtitles.org")
(def endpoint "/xml-rpc")
(def url (str protocol hostname endpoint))
(def options
  {:agent false
   :headers {
             "User-Agent" "OSTestUserAgentTemp" ;; FILL
             "Host" hostname
             "Content-Type" "text/xml"
             }})

(defn parse-xml [s]
  (cxml/parse (java.io.ByteArrayInputStream. (.getBytes s))))


(defn flatten-map [in]
  (cond 
   (or (map? in) (sequential? in)) (->> (seq in)
                                        flatten
                                        (map flatten-map)
                                        (reduce into '()))
   :else [in] ))

;; TODO spec would be great
(defn parse-vals 
  ([pbody] (parse-vals pbody identity))
  ([pbody kick-trash-fn]
   (->> pbody
        flatten-map
        (filter string?)
        kick-trash-fn
        (reduce (fn [acc val] ;; can be split to two transducer funcs
                  (let [v (if (keyword? (last acc)) val (keyword val))]
                    (conj acc v)))
                [])
        (apply hash-map))))

           
#? (:cljs
    (defn- sync [method]
      (fn [req] (go (-> (<! (method url (merge options req)))
                        :body
                        parse-xml
                        parse-vals)))))
#? (:clj
    (defn- sync [method]
      (fn ([req & [resp-kick-trash-fn]]
           (-> (method url (merge options req))
               :body
               parse-xml
               (fn [arg] (if resp-kick-trash-fn (parse-vals arg resp-kick-trash-fn)
                             (parse-vals arg))))))))

;; shouldn't sync/async and resp changing be split ?!

(def method-funcs #? (:cljs
                      {:sync.get  (sync cljs-http/get) 
                       :sync.post (sync cljs-http/post)}
                      :clj
                      {:sync.get  (sync clj-http/get)
                       :sync.post (sync clj-http/post)}))
 
(defn login 
  ([] (login "OSTestUserAgentTemp"))
  ([useragent]      (login         "" useragent))
  ([lang useragent] (login "" "" lang useragent))
  ([username pass lang useragent]
   (let [xml (format x/log-in username pass lang useragent)]
     ((:sync.post method-funcs) {:body xml}))))

(defn logout [token]
  (let [xml (format x/log-out token)]
    ((:sync.post method-funcs) {:body xml})))

(defn no-operation 
  "Keep alive func, should be called every 15 mins between other calls"
  [token]
  (let [xml (format x/no-operation token)]
    ((:sync.post method-funcs) {:body xml})))



;; ---- Trash
#_ (defn avail-langs []
  ((:sync.get method-funcs) {:action "languages"}))

#_ (defn search 
  ([hash] (search hash false))
  ([hash versions?]
   (perform :get (merge {:action "search"
                         :hash hash
                         }
                        (when versions?
                          {:versions true})
                        ))))

#_ (defn download [hash lang]
  (perform :get {:action "download"
                 :hash hash
                 :language lang
                 }))


;; FIXME doesn't work
#_ (defn upload [hash path]
  (perform :post {:multipart [;; Add hash
                              {:content-disposition "form-data"
                               :name "file"
                               :content (clojure.java.io/file path)}]}))


;; TODO hash func
