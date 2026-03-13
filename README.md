# SmartGlassesDetector

[![Release](https://img.shields.io/github/v/release/ozekimasaki/smart-glasses-detector?display_name=tag)](https://github.com/ozekimasaki/smart-glasses-detector/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Privacy Policy](https://img.shields.io/badge/Privacy-Policy-F47C20)](https://smart-glasses-detector-policy.maigo999.workers.dev)

近くのスマートグラスを Bluetooth Low Energy で検出し、通知と履歴で確認できる Android アプリです。

## 主な機能

- BLE 広告データからスマートグラス候補を検出
- 検出時に通知、バイブレーション、音で案内
- 検出履歴を端末内に保存
- 調査用の診断ログを JSON で共有
- Android 12 以上では、設定に応じてユーザー開始中の探索をバックグラウンドで継続

## 対応環境

- `minSdk`: 26
- `targetSdk`: 35
- Android 12 以上: `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`
- Android 11 以前: Bluetooth 探索のため位置情報権限が必要

## プライバシー

- 通常利用時に取得情報を自動で外部サーバーへ送信しません
- 検出記録と設定は端末内に保存されます
- 調査ログは、ユーザーが共有を実行した場合のみ外部アプリへ渡ります

公開ポリシー:

- https://smart-glasses-detector-policy.maigo999.workers.dev

## ビルド

Windows:

```powershell
scripts\gradlew-safe.cmd assembleDebug
scripts\gradlew-safe.cmd assembleRelease
```

主な成果物:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## GitHub Release について

このリポジトリの Release はソースコードのスナップショットとして管理しています。現時点では署名済み配布 APK は含めていません。

## 開発メモ

- アプリ本体: [`app/`](app/)
- プライバシーポリシー静的サイト: [`privacy-site/`](privacy-site/)
- Play 公開チェック: [`docs/play-release-checklist.md`](docs/play-release-checklist.md)
