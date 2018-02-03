(ns archivist.core
  (:require 
            [clojure.tools.logging :as log :only [error info debug]])
  (:gen-class))

(defn- make-temp-file []
  (let [tmpdir (System/getProperty "java.io.tmpdir")
        tmpname (format "arch-%s-%s" (System/currentTimeMillis) (long (rand 0x100000000)))]
    (if-let [f (clojure.java.io/file tmpdir tmpname)]
      f
      (log/error "Can't create temp file"))))

(defn- add-header [headers line]
  ;;FIXME: deal with multiline headers
  (if-let [[name value] (re-matches #"([^ \t]*):[ \t]*(.*)" line)]
    (assoc headers (keyword name) value)
    headers))

(defn- dump-mail [f]
  (log/debug "dump-mail " f)
  (with-open [file (clojure.java.io/writer f)]
    (binding [*out* file]
      (loop [headers '()
             reading-headers? true]
        (when-let [line (read-line)]
          (println line)
          (recur
           (if reading-headers?
             (add-header headers line)
             headers)
           (and reading-headers? (not= line ""))))))))

(defn- detach-attaches [f]
  )

(defn- subversion-commit [headers dir]
  (log/debug "Headers are: "headers))

(defn -main [& args]
  (log/error "err")
  (log/info "info")
  (log/spyf "debug:" "true")
  (System/exit 0)
  ;; пишем stdin в файл, заодно вынимая subject
  ;; вынимаем из файла аттачи
  ;; коммитим
  (let [tmpfile (make-temp-file)
        headers (dump-mail tmpfile)
        tmpdir (detach-attaches tmpfile)]
    (subversion-commit headers tmpdir)))
