# Play 公開前チェック

## Cloudflare Workers Static Assets

1. `privacy-site/` に移動する
2. `wrangler deploy` を実行する
3. 公開 URL `https://smart-glasses-detector-policy.maigo999.workers.dev` を確認する
4. `app/src/main/res/values/strings.xml` の `privacy_policy_url` が同じ URL になっていることを確認する

## Play Console

- プライバシーポリシー URL に Cloudflare の公開 URL を設定する
- Foreground service declaration は `connectedDevice` で提出する
- 説明文は「ユーザーが開始した Bluetooth 周辺機器探索を継続するため」に統一する
- Data safety は、通常時の自動送信がないことと、ユーザー操作による調査ログ共有があることをアプリ文言と矛盾しない形で確認する

## アプリ内確認

- Android 12 以上で背景探索トグルが有効
- Android 10/11 で背景探索トグルが無効
- プライバシー画面から公開ポリシー URL が開く
- 調査ログ共有の説明と実際の JSON 内容が一致する
