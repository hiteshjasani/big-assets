(ns big-assets.files
  (:require [clojure.java.io :as io]
            [clojure.string :as cs]))

(defn list-files
  "Returns a list of java.io.File objects that reside in the specified dirname

   See https://docs.oracle.com/javase/8/docs/api/java/io/File.html
  "
  [^String dirname]
  (-> dirname io/file .listFiles))

(defn list-files-recursive
  [^String dirname]
  (-> dirname io/file file-seq))

(defn mkdir
  "Make a directory if it does not already exist"
  [^String dirname]
  (let [f (io/file dirname)]
    (if-not (.exists f)
      (.mkdirs f))))

(defn extract-filename
  "(f \"foo.bar\") => \"foo\""
  [^String full-filename]
  (subs full-filename 0 (cs/last-index-of full-filename \.)))

(defn extract-file-ext
  "(f \"foo.bar\") => \".bar\""
  [^String full-filename]
  (some->> (cs/last-index-of full-filename \.)
           (subs full-filename)))

(defn name-with-suffix
  "(f \"foo.bar\" \"1234\") => \"foo-1234.bar\""
  [^String full-filename ^String suffix]
  (let [prefix (extract-filename full-filename)
        ext (extract-file-ext full-filename)]
    (str prefix "-" suffix ext)))

(defn name-without-suffix
  "(f \"foo-1234.bar\") => \"foo-.bar\""
  [^String full-filename]
  (let [[prefix suffix] (cs/split (extract-filename full-filename) #"-")
        ext (extract-file-ext full-filename)]
    (str prefix ext)))

(defprotocol Copyable
  "Protocol for enabling easy file copying to similar filenames.

   (copy-with-suffix (io/file \"/tmp/foo.bar\") \"1234\")
   => \"/tmp/foo-1234.css\" created.
  "
  (copy-with-suffix [file suffix] [file dest-dir-name suffix])
  (copy [file dest-dir-name dest-filename])
  )

(extend-protocol Copyable
  java.io.File
  (copy-with-suffix
    ([fileobj suffix]
     (copy-with-suffix fileobj (.getParent fileobj) suffix))

    ([fileobj dest-dir-name suffix]
     (let [new-name (name-with-suffix (.getName fileobj) suffix)
           new-file (io/file dest-dir-name new-name)]
       (io/copy fileobj new-file)
       (.getAbsolutePath new-file))))

  (copy
    ([fileobj dest-dir-name dest-filename]
     (let [new-file (io/file dest-dir-name dest-filename)]
       (io/copy fileobj new-file)
       (.getAbsolutePath new-file))))
  )

(extend-protocol Copyable
  String
  (copy-with-suffix
    ([filename suffix]
     (let [fileobj (io/file filename)]
       (copy-with-suffix fileobj (.getParent fileobj) suffix)))

    ([filename dest-dir-name suffix]
     (let [fileobj (io/file filename)
           new-name (name-with-suffix (.getName fileobj) suffix)
           new-file (io/file dest-dir-name new-name)]
       (io/copy fileobj new-file)
       (.getAbsolutePath new-file))))

  (copy
    ([filename dest-dir-name dest-filename]
     (let [fileobj (io/file filename)
           new-file (io/file dest-dir-name dest-filename)]
       (io/copy fileobj new-file)
       (.getAbsolutePath new-file))))
  )

#_(defprotocol DeletableFingerprint
  "Protocol for deleting fingerprinted files.

   (delete-fingerprinted (io/file \"/tmp/foo-1234.bar\") \"foo.bar\")
   => true
   (delete-fingerprinted (io/file \"/tmp/foo.bar\") \"foo.bar\")
   => false
   (delete-fingerprinted (io/file \"/tmp/foo-1234.clj\") \"foo.bar\")
   => false
  "
  (delete-fingerprinted [file match-filename])
  )

#_(extend-protocol DeletableFingerprint
  java.io.File
  (delete-fingerprinted
    "Delete the file object if it is a fingerprinted version of the filename"
    [fileobj match-filename]
    (let [base-filename (name-without-suffix (.getName fileobj))]
      (if (and (= base-filename match-filename) (not= base-filename (.getName fileobj)))
        (do
          (println (str "Tagged for deletion: " (.getPath fileobj)))
          (io/delete-file (.getAbsolutePath fileobj) true)
          true)
        false))))
