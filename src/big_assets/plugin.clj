(ns big-assets.plugin
  (:require
   [robert.hooke]
   [leiningen.compile]
   [big-assets.core :as core]))

(defn run [f & args]
  (apply core/run args)
  (apply f args))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.compile/compile #'run))
