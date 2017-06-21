(ns {{namespace}}
  (:require [integrant.core :as ig]))
{{#keys}}

(defmethod ig/init-key {{keyword}} [_ options]
  ;; FIXME
  )
{{/keys}}
