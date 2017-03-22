(ns big-assets.core
  "Plugin to leiningen compile, enables automatic fingerprinting of assets.
   
   Usage:

     1. In project.clj

          :big-assets {:src-resources-dir  \"src-assets\"
                       :dest-resources-dir \"resources-gen\"
                       :dest-src-dir       \"src-gen\"
                       :asset-map-ns       \"myproject.assets\"
                       }


     where
       src-resources-dir  = directory of original assets that need to be fingerprinted
       dest-resources-dir = directory where fingerprinted assets are written
       dest-src-dir       = directory where generated clojure source will be written
       asset-map-ns       = namespace that will contain the mapping of asset to fingerprinted file

     2. Define a clean target that will clean up the generated directories

          :clean-targets [[:big-assets :dest-resource-dir] [:big-assets :dest-src-dir]]

     3. Add the dest-resources-dir to your :resource-path

          :resource-paths [... \"resources-gen\"]

     4. Add the dest-src-dir to your :source-paths

          :source-paths [... \"src-gen\"]

  "
  (:require
   [clojure.string :as cs]
   [clojure.java.io :as io]
   [leiningen.core.main :as log]
   [big-assets.files :as files]
   [big-assets.message-digest :as md]
   [clojure.pprint :as pp]))

(defn extract-dirpath-from-ns
  [destdir namespace]
  (let [ix (cs/last-index-of namespace ".")
        parent-dirpath (-> (subs namespace 0 ix)
                           (cs/replace "." "/"))
        filename (str (subs namespace (inc ix) (count namespace)) ".clj")
        fobj (io/file (str destdir "/" parent-dirpath "/" filename))]
    [fobj namespace]))

(defn generate-fingerprints
  "Return map of filename -> fingerprint"
  [file-obj-list]
  (reduce
   (fn [acc x]
     (assoc acc (.getName x) (-> x slurp md/digest)))
   {}
   file-obj-list))

(defn generate-filename-mapping
  "Return map between filename and fingerprinted filename

   if forward?
     filename -> fingerprinted filename
     fingerprinted-filename -> filename
  "
  [forward? kv-filename-fingerprint]
  (->> kv-filename-fingerprint
       (map (fn [[filename fingerprint]]
              (let [fp-filename (files/name-with-suffix filename fingerprint)]
                (if forward?
                  [filename fp-filename]
                  [fp-filename filename]))))
       (into {})))

(defn mapping-file-contents
  [namespace forward-mapping reverse-mapping]
  (format "(ns %s \"This file is auto-generated, do NOT edit!\")\n\n(def fwd-map %s)\n\n(def rev-map %s)"
          namespace (str forward-mapping) (str reverse-mapping)))

(defn run [project-map & args]
  (let [{:keys [:src-resources-dir :dest-resources-dir :dest-src-dir :asset-map-ns]
         :as config} (:big-assets project-map)
        file-obj-list (files/list-files src-resources-dir)
        fingerprints (generate-fingerprints file-obj-list)
        forward-mapping (generate-filename-mapping true fingerprints)
        reverse-mapping (generate-filename-mapping false fingerprints)
        [fobj new-namespace] (extract-dirpath-from-ns dest-src-dir asset-map-ns)
        ]
    (log/debug (with-out-str (pp/pprint config)))
    (files/mkdir dest-src-dir)
    (files/mkdir (.getParent fobj))
    (log/info (str "Writing " (.getPath fobj)))
    (spit (.getPath fobj) (mapping-file-contents new-namespace forward-mapping reverse-mapping))
    (files/mkdir dest-resources-dir)
    (doseq [[filename fp-filename] forward-mapping]
      (let [src-filename (str src-resources-dir "/" filename)]
        (log/info (str "Generating " fp-filename))
        (files/copy src-filename dest-resources-dir fp-filename)
        ))))
