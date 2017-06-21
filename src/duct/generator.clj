(ns duct.generator
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [stencil.core :as stencil]))

(defn- find-project-keys [config]
  (let [prefix (str (:duct.core/project-ns config) ".")]
    (filter #(str/starts-with? (namespace %) prefix) (keys config))))

(defn- ns-parts [key]
  (str/split (namespace key) #"\."))

(defn- key-namespace [key]
  (if (> (count (ns-parts key)) 2)
    (namespace key)
    (str (namespace key) "." (name key))))

(defn- key-namespace-exists? [key]
  (let [ns-path   (-> key namespace (str/replace "." "/") (str/replace "-" "_"))
        name-path (-> key name (str/replace "-" "_"))]
    (or (io/resource (str ns-path ".clj"))
        (io/resource (str ns-path "/" name-path ".clj"))
        (io/resource (str ns-path ".cljc"))
        (io/resource (str ns-path "/" name-path ".cljc")))))

(defn- namespace-path [ns]
  (-> ns
      (str/replace "." java.io.File/separator)
      (str/replace "-" "_")
      (str ".clj")))

(defn- make-parent-dirs [file]
  (.mkdirs (.getParentFile (io/file file))))

(defn- render-resource [path params]
  (stencil/render-string (slurp (io/resource path)) params))

(defn- generate-template [ns ks]
  (render-resource "duct/generator/template/default.clj"
                   {:namespace ns
                    :keys (for [k ks] {:keyword k})}))

(defn generator
  ([] (generator {}))
  ([options]
   (let [source-path (:source-path options "src")]
     (fn [config]
       (doseq [[ns ks] (->> (find-project-keys config)
                            (remove key-namespace-exists?)
                            (group-by key-namespace))]
         (let [file (io/file source-path (namespace-path ns))]
           (println "Generating" (str file))
           (make-parent-dirs file)
           (spit file (generate-template ns ks))))
       config))))
