(ns baymax.source.mongo
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [baymax.source.proto :refer [Source]])
  (:import [com.mongodb Block ConnectionString MongoClientSettings MongoCredential]
           [com.mongodb.client MongoClient MongoClients MongoCollection MongoDatabase]
           [org.bson Document]
           [org.bson.conversions Bson]
           [org.bson.types Decimal128 ObjectId]
           [java.security KeyStore]
           [java.security.cert CertificateFactory]
           [javax.net.ssl SSLContext TrustManagerFactory]
           [java.util ArrayList Date List Map]))

(defn- ->bson
  "convert edn (as it comes from the config) to bson:
   keyword keys and values become strings, so {:$match {:status {:$in [\"OBSERVED\"]}}} just works"
  [v]
  (cond
    (map? v)        (reduce-kv (fn [^Document doc k value]
                                 (.append doc
                                          (if (keyword? k) (name k) (str k))
                                          (->bson value)))
                               (Document.)
                               v)
    (sequential? v) (ArrayList. ^java.util.Collection (mapv ->bson v))
    (keyword? v)    (name v)
    (symbol? v)     (str v)
    :else           v))

(defn- ->clj
  "convert bson to plain clojure data:
   transforms run in a sandbox without java interop, and intel is serialized to json by
   publishers and http handlers, so no bson types should leak out of a source"
  [v]
  (condp instance? v
    Map        (into {} (for [[k value] v]
                          [(keyword (str k)) (->clj value)]))
    List       (mapv ->clj v)
    ObjectId   (str v)
    Decimal128 (.bigDecimalValue ^Decimal128 v)
    Date       (.toInstant ^Date v)
    v))

(defn- drain
  "pull everything from a mongo cursor, closing it on the way out"
  [iterable]
  (with-open [cursor (.cursor iterable)]
    (into [] (map ->clj) (iterator-seq cursor))))

(defn- run-pipeline [^MongoCollection coll pipeline]
  (.aggregate coll (->bson pipeline)))

(defn- run-find [^MongoCollection coll {:keys [filter projection sort skip limit]}]
  (cond-> (.find coll ^Bson (->bson (or filter {})))
    projection (.projection (->bson projection))
    sort       (.sort (->bson sort))
    skip       (.skip (int skip))
    limit      (.limit (int limit))))

(defn execute-query
  "run a collector's :pipeline (aggregation) or :find (filter / projection / sort / skip / limit)
   against a collection and return a vector of documents"
  [{:keys [client config]} {:keys [id database collection pipeline find]}]
  (let [db-name (or database
                    (get-in config [:connection :database]))]

    (when-not db-name
      (throw (ex-info "mongo collector has no database to look at: set :database on the source's :connection or on the collector"
                      {:collector id})))
    (when-not collection
      (throw (ex-info "mongo collector needs a :collection" {:collector id})))
    (when-not (or pipeline find)
      (throw (ex-info "mongo collector needs either a :pipeline or a :find" {:collector id
                                                                            :collection collection})))
    (try
      (let [coll (-> ^MongoClient client
                     (.getDatabase db-name)
                     (.getCollection collection))]
        (drain (if pipeline
                 (run-pipeline coll pipeline)
                 (run-find coll find))))
      (catch Exception e
        (log/error e "failed to query mongo collection:" (str db-name "." collection))
        (throw (ex-info "query execution failed"
                        {:collector id
                         :collection (str db-name "." collection)
                         :query (or pipeline find)
                         :error (.getMessage e)}))))))

(defn check-health
  "check if the mongo connection is healthy"
  [{:keys [client config]}]
  (try
    (let [{:keys [database auth-source]} (:connection config)]
      (when (-> ^MongoClient client
                (.getDatabase (or database auth-source "admin"))
                (.runCommand (Document. "ping" 1)))
        {:healthy true
         :status "connected"
         :message "mongo connection is 6 feet above"}))
    (catch Exception e
      {:healthy false
       :status "error"
       :message (.getMessage e)})))

(defn- pool-settings [pool]
  (reify Block
    (apply [_ builder]
      (cond-> builder
        (:maximum-pool-size pool) (.maxSize (int (:maximum-pool-size pool)))
        (:minimum-idle pool)      (.minSize (int (:minimum-idle pool)))))))

(defn- read-certificates [ca-file]
  (let [file (io/file ca-file)]
    (when-not (.exists file)
      (throw (ex-info "mongo :tls-ca-file is not there" {:tls-ca-file ca-file})))
    (with-open [in (io/input-stream file)]
      (let [certs (.generateCertificates (CertificateFactory/getInstance "X.509") in)]
        (when (empty? certs)
          (throw (ex-info "mongo :tls-ca-file has no certificates in it" {:tls-ca-file ca-file})))
        certs))))

(defn- ca-ssl-context
  "the mongo java driver does not read a \"tlsCAFile\": it trusts whatever the jvm trust store trusts.
   to talk to a cluster that is behind a private ca, trust is built right here: from the pem itself.
   it only applies to this source: the jvm trust store is left alone"
  [ca-file]
  (let [certs (read-certificates ca-file)
        trust (doto (KeyStore/getInstance (KeyStore/getDefaultType))
                (.load nil nil))]
    (doseq [[idx cert] (map-indexed vector certs)]
      (.setCertificateEntry trust (str "ca-" idx) cert))
    (let [factory (doto (TrustManagerFactory/getInstance (TrustManagerFactory/getDefaultAlgorithm))
                    (.init trust))]
      (log/info "trusting" (count certs) "certificate(s) from" ca-file)
      (doto (SSLContext/getInstance "TLS")
        (.init nil (.getTrustManagers factory) nil)))))

(defn- url-auth-source
  "the driver only reports an \"authSource\" as a part of a credential,
   and a credential only exists when the url carries a user, which is exactly what is stripped above.
   hence it is read from the url itself"
  [url]
  (second (re-find #"(?i)[?&]authSource=([^&]+)" url)))

(defn- url-ca-file
  "\"tlsCAFile\" is a mongosh / libmongoc option: the java driver logs it as unsupported and moves on.
   since a url is usually copied from a mongosh command that works, it is picked up from there as well"
  [url]
  (second (re-find #"(?i)[?&]tlsCAFile=([^&]+)" url)))

(defn- ssl-settings [^SSLContext ssl-context]
  (reify Block
    (apply [_ builder]
      (doto builder
        (.enabled true)       ;; a ca is only ever given for a tls connection
        (.context ssl-context)))))

(defn- user-without-password?
  "a url that brought a username, but no password: mongo won't even parse it"
  [url]
  (boolean (re-find #"://[^/@:]+@" url)))

(defn- without-credentials
  "credentials belong in :user / :password, hence a url that brought its own is cleaned up first.
   (a url with a username, but no password, would not even parse)"
  [url]
  (s/replace url #"://[^/@]*@" "://"))

(defn make-client
  "a mongo url usually carries a lot more than a host and a port:
   cluster (srv) seed list, tls, replica set, read preference, timeouts, etc.
   hence it is taken as is, and credentials are kept separately: they tend to come from the environment"
  [{:keys [connection pool]}]
  (let [{:keys [url user password database auth-source tls-ca-file]} connection]

    (when-not url
      (throw (ex-info "mongo source needs a :connection :url" {:connection (dissoc connection :password)})))
    (when (and user (not password))
      (throw (ex-info "mongo source has a :connection :user, but no :password"
                      {:connection (dissoc connection :password)})))
    (when (and (not user) (user-without-password? url))
      (throw (ex-info "mongo url carries a user, but no password: move them to :connection :user and :password"
                      {:connection (assoc connection :url (without-credentials url))})))

    (let [cs (ConnectionString. (if user                      ;; ours win over whatever the url brought
                                  (without-credentials url)
                                  url))
          auth-db (or auth-source                             ;; explicit wins
                      (url-auth-source url)                   ;; then "?authSource=" from the url
                      (some-> (.getCredential cs) .getSource) ;; then a url that kept its own credentials
                      (not-empty (.getDatabase cs))           ;; then the url's own database (mongo does the same)
                      database
                      "admin")
          ca-file (or (not-empty (s/trim (str tls-ca-file)))  ;; blank is "no ca": it keeps a config
                                                             ;; placeholder around for env vars to fill in
                      (when-let [from-url (url-ca-file url)]
                        (log/info "taking a tls ca file from the url:" from-url
                                  "(set it as :connection :tls-ca-file to be explicit)")
                        from-url))
          builder (-> (MongoClientSettings/builder)
                      (.applyConnectionString cs)
                      (.applyToConnectionPoolSettings (pool-settings pool)))]
      (MongoClients/create
        (.build (cond-> builder
                  ca-file (.applyToSslSettings (ssl-settings (ca-ssl-context ca-file)))
                  user (.credential (MongoCredential/createCredential user
                                                                      auth-db
                                                                      ;; a config / env value is not always a string
                                                                      (char-array (str password))))))))))

(defn- redact-url
  "in case credentials made their way into the url itself, don't keep them around"
  [url]
  (when url
    (s/replace url #"://[^/@]+@" "://<redacted>@")))

(defrecord MongoSource [config client]
  Source
  (collect [this {:keys [transform] :as collector}]
    (let [collected (execute-query this collector)]
      ((or transform identity) collected)))

  (health-check [this]
    (check-health this))

  (disconnect [this]
    (when client
      (.close ^MongoClient client)
      (log/info "disconnected from mongo:"
                (get-in config [:connection :database])))))

(defn make-source [config]
  (let [client (make-client config)
        config (update config :connection
                       #(-> %
                            (dissoc :password)
                            (assoc :url (redact-url (:url %)))))]
    (->MongoSource config client)))
