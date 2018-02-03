(ns archivist.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log :only [error info debug]])
  (:gen-class))

(defn- make-temp-file []
  (let [sys-tmp-dir (System/getProperty "java.io.tmpdir")
        tmpdir (format "arch-%s-%s" (System/currentTimeMillis) (long (rand 0x100000000)))]
    (let [path (list sys-tmp-dir tmpdir "mail")]
      (apply io/make-parents path)
      (apply io/file path))))

(defn- add-header [headers line]
  (log/info (format "Remembering new header: '%s'" line))
  (if-let [[_ name value] (re-matches #"([^ \t]*):[ \t]*(.*)" line)]
    (assoc headers (keyword (clojure.string/lower-case name)) value)
    headers))

(defn- dump-mail [f]
  (log/info "dump-mail into " f)
  (with-open [file (io/writer f)]
    (binding [*out* file]
      (loop [line (read-line)
             accumulated-header ""
             headers {}]
        (log/info (format "line: '%s'" line))
        (if (nil? line)
          headers
          (do
            (log/info (format "accumulated-header: '%s'" accumulated-header))
            (if (nil? accumulated-header)
              ;; body
              (do
                (log/info (format "body line: '%s'" line))
                (println line)
                (recur (read-line) nil headers))
              ;; end of headers
              (if (= line "")
                (if (not (empty? accumulated-header))
                  (do
                    (log/info (format "storing last header: '%s'" accumulated-header))
                    (recur (read-line) nil (add-header headers accumulated-header)))
                  (do
                    (log/error "Huh? Empty headers?!")
                    (recur (read-line) nil headers)))
                ;; header continuation?
                (if (clojure.string/blank? (str (first line)))
                  (do
                    (log/info "Got a continuation")
                    (assert (not (empty? accumulated-header)))
                    (recur (read-line) (str accumulated-header line) headers))
                  ;; new header
                  (do
                    (log/info "New header!")
                    (if (empty? accumulated-header)
                      (recur (read-line) line headers)
                      (recur (read-line) line (add-header headers accumulated-header)))))))))))))

(defn- detach-attaches [f]
  )

(defn delete-recursively [f]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               ;;(io/delete-file f))]
               (log/info "DELETING " (.getAbsolutePath f)))]
    (func func f)))

(defn- subversion-commit [headers dir]
  (log/info "Headers are: " (keys headers)))

(defn -main [& args]
  (log/error "err")
  (log/info "info")
  (log/spyf "debug:" "true")
  ;; пишем stdin в файл, заодно вынимая subject
  ;; вынимаем из файла аттачи
  ;; коммитим
  (let [temp-file (make-temp-file)]
    (try
      (let [headers (dump-mail temp-file)]
        (detach-attaches temp-file)
        (subversion-commit headers temp-file))
      (finally (delete-recursively (io/file (.getParent temp-file)))))))
