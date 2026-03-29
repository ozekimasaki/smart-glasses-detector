# Play 公開前チェック

## 1. ローカル設定

- `keystore.properties` と release keystore をローカルに用意する
- `keystore.properties` が Git 管理対象でないことを確認する
- 今回の公開版は `versionCode = 9` / `versionName = 1.0.8` / Git tag `v1.0.8`
- `app/src/main/res/values/strings.xml` の `privacy_policy_url` が公開ポリシー URL と一致していることを確認する

## 2. プライバシーポリシー公開

1. `privacy-site/` に移動する
2. `wrangler deploy` を実行する
3. 公開 URL `https://smart-glasses-detector-policy.maigo999.workers.dev` を確認する
4. Play Console のプライバシーポリシー URL に同じ URL を設定する
5. 公開ポリシーの最終更新日が最新であることを確認する

## 3. リリース前のローカル検証

Windows:

```powershell
scripts\gradlew-safe.cmd testDebugUnitTest
scripts\gradlew-safe.cmd lint
scripts\gradlew-safe.cmd bundleRelease
scripts\gradlew-safe.cmd assembleRelease
```

確認ポイント:

- `app/build/outputs/bundle/release/app-release.aab` が生成される
- `app/build/outputs/apk/release/app-release.apk` が生成される
- lint に blocking error がない
- テストが通る

## 4. Play Console 入力メモ

### ストア掲載情報

- アプリタイトル: 30 文字以内
- 短い説明: 80 文字以内
- 詳細な説明: 4,000 文字以内
- 連絡先メールアドレス: 必須
- Web サイト / 電話番号: 必要に応じて設定

### このアプリで統一する説明

- Foreground service declaration は `connectedDevice`
- 説明文は「ユーザーが開始した Bluetooth 周辺機器探索を継続するため」に統一する
- Data safety は、通常時にデータを自動送信しないこと、ユーザー操作による診断ログ共有があること、検出記録に機器アドレス・メーカー名・推定距離を含めて端末内保存していることと矛盾しない形で入力する

### 今回の版で入力する値

- リリース名: `1.0.8`
- リリースノート見出し: `診断ログ永続化の修正`
- アプリ カテゴリ候補: `ツール`
- 連絡先メールアドレス: 開発者が常時受信できる公開用アドレスを設定する
- プライバシーポリシー URL: `https://smart-glasses-detector-policy.maigo999.workers.dev`
- App access: `なし`（ログイン不要、特別なテスト手順不要）
- 広告: `なし`
- Foreground service declaration: `connectedDevice` を選択し、「ユーザーが開始した Bluetooth 周辺機器探索を継続するため」と記載する
- Data safety の説明文: 「検出記録と設定は端末内保存のみ。通常利用で外部自動送信なし。調査ログはユーザーが共有操作を行った場合のみ外部アプリへ渡る」に統一する
- 新規個人 developer account の場合は、production 申請前に closed testing（12 人・14 日）要件の有無を Play Console 上で確認する

## 5. ストア掲載文のたたき台

### アプリタイトル

`スマートグラス検出`

### 短い説明

`近くのスマートグラスをBluetoothで検出し、通知と履歴で確認できます`

### 詳細な説明

`スマートグラス検出` は、Bluetooth Low Energy を使って近くのスマートグラス候補を見つけ、通知と記録で確認できるアプリです。

- 周辺のスマートグラス候補を検出
- 検出時に通知・バイブレーション・音でお知らせ
- 検出履歴を端末内に保存
- 必要に応じて診断ログを JSON で共有

通常利用時に、取得した情報を自動で外部サーバーへ送信することはありません。記録や設定は端末内に保存され、診断ログはユーザーが共有を実行した場合のみ外部アプリへ渡されます。

## 6. 画像素材

- Play ストア掲載用アイコン: 512 x 512 PNG
- Feature graphic: 1024 x 500 JPEG または 24-bit PNG
- スマートフォン用スクリーンショット: 2 枚以上

メモ:

- Play Console の掲載用アイコンは launcher icon とは別にアップロードする
- スクリーンショットは実際の UI を使い、過度な宣伝文句やランキング表現を避ける

## 7. アプリ内確認

- Android 12 以上で背景探索トグルが有効
- Android 10/11 で背景探索トグルが無効
- プライバシー画面から公開ポリシー URL が開く
- 調査ログ共有の説明と実際の JSON 内容が一致する
- 通知権限未許可時の挙動を確認する

## 8. 公開手順

1. signed AAB を internal testing にアップロードする
2. 実機で権限、通知、履歴、診断ログ共有、バックグラウンド探索を確認する
3. Play Console の Data safety、コンテンツ レーティング、Foreground service declaration を提出する
4. ストア素材と説明文を反映する
5. 問題がなければ production へ昇格する

### 最終チェック項目

- アップロード対象が `app/build/outputs/bundle/release/app-release.aab` である
- Git の release tag が `v1.0.8` で push 済みである
- Store listing の説明文がアプリ内プライバシー説明と矛盾しない
- Data safety で「自動送信なし」と「ユーザー操作による共有のみ」が両立している
- 新規個人 developer account の場合、closed testing 要件を満たしてから production に進む
