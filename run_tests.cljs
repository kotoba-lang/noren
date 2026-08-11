#!/usr/bin/env nbb
;; nbb run_tests.cljs — noren のテスト（第一の runtime は cljs / nbb）
;;
;;   nbb --classpath "src:test:../design-quality/src:../jp-go-digital-design-system/src:../html/src" run_tests.cljs
;;
;; JVM 側は `clojure -M:test`（同じ .cljc を走らせる）。
(require '[clojure.test :as t] 'noren.noren-test)

(let [{:keys [fail error]} (t/run-tests 'noren.noren-test)]
  (js/process.exit (if (pos? (+ fail error)) 1 0)))
