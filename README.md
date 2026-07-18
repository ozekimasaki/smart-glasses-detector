# SmartGlassesDetector

[![Release](https://img.shields.io/github/v/release/ozekimasaki/smart-glasses-detector?display_name=tag)](https://github.com/ozekimasaki/smart-glasses-detector/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Privacy Policy](https://img.shields.io/badge/Privacy-Policy-F47C20)](https://smart-glasses-detector-policy.maigo999.workers.dev)

近くのスマートグラスを Bluetooth Low Energy (BLE) で検出し、通知と履歴で確認できる Android アプリです。ボタン 1 つで探索を開始でき、技術用語を使わない画面設計で一般ユーザーでも扱えることを目指しています。

## 概要

- **パッケージ名**: `jp.smartglasses.detector`
- **表示名**: スマートグラス検出
- **アーキテクチャ**: MVVM + Clean Architecture（`presentation` → `domain` → `data`）
- **UI**: Jetpack Compose + Material Design 3
- BLE 広告（アドバタイズ）を監視し、Company ID とデバイス名パターンの 2 段階でスマートグラス候補を判定します。

## 主な機能

- BLE 広告データからスマートグラス候補を検出（Company ID / デバイス名パターン）
- 検出時に通知、バイブレーション、音で案内（設定で個別に切り替え可能）
- 検出履歴を端末内（Room）に保存し、履歴画面で確認
- スキャン感度（省電力 / 標準 / 高精度）の切り替え
- 調査用の診断ログを JSON 形式でエクスポート・共有
- フォアグラウンドサービスによるバックグラウンド探索の継続
- 初回起動時のオンボーディングと権限説明

## 対応環境

- `minSdk`: 26 (Android 8.0)
- `targetSdk` / `compileSdk`: 35 (Android 15)
- Android 12 以上: `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`
- Android 11 以前: BLE 探索のため位置情報権限（`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`、`maxSdkVersion=30`）が必要
- `android.hardware.bluetooth_le` を必須機能として要求

## 技術スタック

| カテゴリ | ライブラリ | バージョン |
|---------|-----------|-----------|
| 言語 | Kotlin | 2.0.21 |
| UI | Jetpack Compose (BOM) | 2024.12.01 |
| DI | Hilt | 2.53.1 |
| DB | Room | 2.6.1 |
| 設定永続化 | DataStore Preferences | 1.1.1 |
| ナビゲーション | Navigation Compose | 2.8.5 |
| ビルドシステム | Gradle KTS + `libs.versions.toml` (AGP 8.7.3) | Gradle 9.3.1 |
| コード生成 | KSP | 2.0.21-1.0.28 |

依存関係のバージョンは [`gradle/libs.versions.toml`](gradle/libs.versions.toml) で集中管理しています。

## セットアップ

1. Android Studio（AGP 8.7.3 に対応するバージョン）で本リポジトリを開くか、コマンドラインで Gradle Wrapper を利用します。
2. JDK 17 以上を用意します（ビルドは `sourceCompatibility` / `jvmTarget = 11` を使用）。
3. Android SDK Platform 35 をインストールします。
4. 依存関係は初回ビルド時に自動で解決されます。

## ビルド

Gradle Wrapper を使用します。

Linux / macOS:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
```

Windows では、ビルド環境の差異を吸収するラッパースクリプトを利用できます（詳細は [`docs/build-environment.md`](docs/build-environment.md)）。

```powershell
scripts\gradlew-safe.cmd assembleDebug
scripts\gradlew-safe.cmd assembleRelease
scripts\gradlew-safe.cmd bundleRelease
```

主な成果物:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

`keystore.properties` がない状態では、release ビルドは未署名になります（Play Console にアップロードできる署名済み成果物は作れません）。署名設定のテンプレートは [`keystore.properties.example`](keystore.properties.example) を参照してください。

## 開発コマンド

Linux / macOS の例（Windows では `scripts\gradlew-safe.cmd <task>`）:

```bash
# ユニットテスト（app/src/test）
./gradlew test
./gradlew testDebugUnitTest

# Android Lint
./gradlew lint
./gradlew lintDebug
./gradlew lintRelease

# 計測テスト（要: エミュレータ / 実機）
./gradlew connectedAndroidTest

# クリーン
./gradlew clean
```

Kotlin コンパイル（型チェックを兼ねる）は `assembleDebug` などのビルドタスク実行時に行われます。専用の ktlint / detekt / spotless などの設定は本リポジトリには含まれていません。

## プロジェクト構成

```
.
├── app/                     # Android アプリ本体（:app モジュール）
│   ├── src/main/java/jp/smartglasses/detector/
│   │   ├── di/              # Hilt モジュール
│   │   ├── domain/          # model / repository interface / usecase / service
│   │   ├── data/            # bluetooth / database(Room) / preferences / repository / export
│   │   ├── presentation/    # Compose 画面と ViewModel（main/history/settings/onboarding/about/privacy）
│   │   ├── service/         # ScanningForegroundService
│   │   ├── ui/theme/        # Compose テーマ
│   │   ├── util/            # Constants など
│   │   ├── MainActivity.kt
│   │   └── SmartGlassesDetectorApp.kt
│   └── build.gradle.kts
├── gradle/libs.versions.toml # 依存バージョンカタログ
├── scripts/                 # Windows 向け Gradle ラッパースクリプト
├── tools/                   # BLE エミュレータ（テスト用 Python スクリプト）
├── privacy-site/            # プライバシーポリシー静的サイト（Cloudflare Workers）
├── docs/                    # ビルド環境・署名・Play 公開手順のドキュメント
├── AGENTS.md                # コーディングエージェント向けガイド
├── CLAUDE.md
└── PROJECT_PLAN.md          # 開発計画書
```

## テスト用 BLE エミュレータ

`tools/ble_smartglasses_emulator.py` は、Raspberry Pi 等からスマートグラスの BLE 広告を模擬送信し、検出動作を確認するための Python スクリプトです（`Constants.kt` のメーカー定義に対応）。

## プライバシー

- 通常利用時に取得情報を自動で外部サーバーへ送信しません
- 検出記録と設定は端末内に保存されます
- 診断ログは、ユーザーが共有を実行した場合のみ外部アプリへ渡ります

公開ポリシー: https://smart-glasses-detector-policy.maigo999.workers.dev

## GitHub Release について

このリポジトリの Release は公開配布にも使えますが、署名鍵は GitHub や repo に保存しません。署名済み APK / AAB を公開する場合は、ローカルの `keystore.properties` と keystore を使って生成してから Release に添付します。

- 署名手順: [`docs/github-release-signing.md`](docs/github-release-signing.md)
- Play 公開チェック: [`docs/play-release-checklist.md`](docs/play-release-checklist.md)

## 開発メモ

- アプリ本体: [`app/`](app/)
- プライバシーポリシー静的サイト: [`privacy-site/`](privacy-site/)
- 開発計画書: [`PROJECT_PLAN.md`](PROJECT_PLAN.md)

## ライセンス

本リポジトリには LICENSE ファイルが含まれていません。利用条件についてはリポジトリ所有者にお問い合わせください。
