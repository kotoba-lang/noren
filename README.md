# noren

**暖簾（のれん）—— 店が外に掛けている面のこと。この repo は、事業者の公開面を
「測る・処方する・建てる」判断そのもの**を持つ純 `.cljc` ライブラリである。
名前が機能を示さないので最初に名乗る（superproject `CLAUDE.md` の規約）。

I/O は 1 バイトも無い。取得も送信も課金もしない —— それを回すのは
[`cloud-itonami/loop-noren`](https://github.com/cloud-itonami/loop-noren) で、
この repo は `loop-*` が持ってはいけない **domain scoring truth** の側にあたる
（`manifest/repository-rules.edn`、ADR-2607299000）。

設計: superproject **ADR-2608111400**。

## 何を持っていて、何を持っていないか

| ns | 答える問い |
|---|---|
| `noren.prospect` | この事業者に、いま接触してよいか（業種 gate / 特定電子メール法 3 条 1 項 4 号の形 / 観測の鮮度） |
| `noren.diagnose` | その公開面は、店の仕事をどれだけ引き受けられているか |
| `noren.prescribe` | 直すのか建て替えるのか。**測った軸だけで**何を言えるか |
| `noren.governor` | その提案を外へ出してよいか（HARD 10 / SOFT 2） |
| `noren.build` | 貰った事実だけで面を建てる |
| `noren.plan` | 月額いくらで、何が付いていて、今日その capability を使ってよいか |

**UI 品質は測り直していない。** `kotoba-lang/design-quality` が HIG/WCAG の
12 軸を持っているので、`noren.diagnose` は同じ `{:id :title :weight :check}` の
形で presence 軸を足すだけで、重み正規化も finding 収集も機構ごと借りる。

## 3 つの規律

**1. 測っていないことは言えない。** `:claims` は 1 文につき 1 つの軸 id を
背負い、governor はその軸が `:evidence` に無ければ `:hold` にする。文面を
LLM に書かせても、LLM が触れるのは claim の並べ方だけで claim は増やせない。

**2. 貰っていない事実は書かない。** 営業時間も電話も住所もメニューも、brief に
無ければ節ごと出さず `:missing` に積む。診断で `:hours` が落ちた店に、想像した
営業時間の載ったサイトを見せるのは提案ではなく事故である。

**3. 建てた面は自分の基準を通る。** 相手のサイトを採点する側が、自分の出す面を
採点していなければ話にならない。テストが `build → diagnose` を実際に通し、
presence ≥ 90 / UI ≥ 80 / `:healthy` を要求する（`app-css` を空にすると
`:repairable` に落ちることを確認済み —— 落ちない gate は劇場）。

## 接触してよい形（特定電子メール法）

日本の特定電子メール法はオプトイン規制（法 3 条 1 項）で、広告宣伝メールは
原則として同意が要る。この loop が当たる例外は **自らメールアドレスを公表して
いる団体・営業を営む個人**（同項 4 号）ただ 1 つ。したがって:

- アドレスを知っているだけでは足りない。**どこで公表されていたか（URL）と
  いつ見たか**が無ければ、例外に当たる根拠が持てない → `:no-publication-evidence`
- サイトに「営業メールお断り」があれば公表アドレスでも送れない（4 号ただし書き）
- 本文には送信者の名称・住所・受信拒否の通知先が要る（法 4 条）
- 一度断られた相手は恒久に送らない。再送は最短 60 日、生涯 2 回まで

`noren.governor` はこれを**判定に必要な事実が渡されていない場合も含めて**
HARD で止める。検査に必要な材料が無いことは、検査に通ったことではない。

## 使う

```clojure
(require '[noren.prospect :as prospect] '[noren.diagnose :as diagnose]
         '[noren.prescribe :as prescribe] '[noren.governor :as gov])

(prospect/eligible p "2026-08-11")
;; => {:ok? true :reasons [] :surface :storefront}

(-> (diagnose/diagnose html {:surface :storefront :url url :year 2026})
    prescribe/prescribe)
;; => {:action :rebuild :claims [{:claim/axis :hours :claim/text "営業時間の…"}] …}

(gov/review {:prospect p :prescription rx :message m
             :history hs :suppression #{} :now "2026-08-11"})
;; => {:decision :commit|:hold :violations [...] :warnings [...] :receipt {...}}
```

## テスト

```bash
nbb --classpath "src:test:../design-quality/src:../jp-go-digital-design-system/src:../html/src:../css/src" run_tests.cljs
clojure -M:test    # 同じ .cljc を JVM でも
```

9 tests / 67 assertions。HARD rule は 10 本すべて「実際に `:hold` になること」を
1 本ずつ壊して確かめている。
