(ns big-assets.message-digest
  (:import (java.security MessageDigest)
           (java.math BigInteger)))

(defn- digest-padding
  [desired-len
   actual-len]
  (apply str (repeat (- desired-len actual-len) "0")))

(defn- signature
  [^MessageDigest algo ^String msg]
  (-> (.getBytes msg)
      (->> (.digest algo))
      (->> (BigInteger. 1))
      (.toString 16)))

(defn- md5
  "Generate md5 hash of message."
  [^String msg]
  (let [algo (MessageDigest/getInstance "MD5")
        sig-len (-> algo .getDigestLength (* 2))
        sig (signature algo msg)
        padding (digest-padding sig-len (count sig))]
    (str padding sig)))

(defprotocol MessageDigestible
  (digest [msg]))

(extend-protocol MessageDigestible
  String
  (digest [msg] (md5 msg))

  java.io.File
  (digest [file] (md5 (slurp (.getAbsolutePath file))))

  nil
  (digest [s] nil))
