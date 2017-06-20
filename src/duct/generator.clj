(ns duct.generator
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [stencil.core :as stencil]))

(defn- find-project-keys [config]
  (let [prefix (str (:duct.core/project-ns config) ".")]
    (filter #(str/starts-with? (namespace %) prefix) (keys config))))

(defn- ns-path
  ([key] (ns-path key java.io.File/separator))
  ([key sep]
   (when-let [ns (namespace key)]
     (str (str/replace ns "." sep) ".clj"))))

(defn- ns+name-path
  ([key] (ns+name-path key java.io.File/separator))
  ([key sep]
   (when-let [ns (namespace key)]
     (str (str/replace ns "." sep) sep (name key) ".clj"))))

(defn- key-namespace-exists? [key]
  (or (io/resource (ns-path key "/"))
      (io/resource (ns+name-path key "/"))
      (io/resource (str (ns-path key "/") "c"))
      (io/resource (str (ns+name-path key "/") "c"))))

(defn- ns-parts [key]
  (str/split (namespace key) #"\."))

(defn- key-path [key]
  (if (> (count (ns-parts key)) 2)
    (ns-path key)
    (ns+name-path key)))

(defn- key-namespace [key]
  (if (> (count (ns-parts key)) 2)
    (namespace key)
    (str (namespace key) "." (name key))))

(defn- make-parent-dirs [file]
  (.mkdirs (.getParentFile (io/file file))))

(defn- render-resource [path params]
  (stencil/render-string (slurp (io/resource path)) params))

(defn- generate-template [key]
  (render-resource "duct/generator/template/default.clj"
                   {:namespace (key-namespace key)
                    :key       key}))

(defn generator
  ([] (generator {}))
  ([options]
   (let [source-path (:source-path options "src")]
     (fn [config]
       (doseq [k (find-project-keys config)]
         (when-not (key-namespace-exists? k)
           (let [file (io/file source-path (key-path k))]
             (println "Generating" (str file))
             (make-parent-dirs file)
             (spit file (generate-template k)))))
       config))))
