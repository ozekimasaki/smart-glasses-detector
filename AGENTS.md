# AGENTS.md - スマートグラス検出アプリ

このリポジトリで作業するコーディングエージェント向けのガイドです。実際のコードと設定に基づいて記述しています。人間向けの概要は [`README.md`](README.md) を参照してください。

## プロジェクト概要

- **アプリ名 / 表示名**: スマートグラス検出
- **パッケージ名 / `applicationId`**: `jp.smartglasses.detector`
- **目的**: BLE 広告を監視して近くのスマートグラスを検出し、通知・履歴・診断ログで確認できるアプリ（Android / iOS）
- **`minSdk`**: 26 (Android 8.0) / **`targetSdk`** / **`compileSdk`**: 36 / 37（Android 16 / API 37）
- **iOS**: 17+ / Bundle ID `jp.smartglasses.detector`（`iosApp/`）
- **`versionCode` / `versionName`**: `app/build.gradle.kts` で管理（現行 48 / 1.1.38）
- モジュール: `:shared`（KMP 検出頭脳）+ `:app`（Android）+ `iosApp/`（SwiftUI）

## アーキテクチャ

MVVM + Clean Architecture の 3 層:

```
presentation/ → domain/ → data/
```

- **presentation/**: Jetpack Compose UI と ViewModel
- **domain/**: model / repository interface / usecase / service interface
- **data/**: repository 実装、BLE、Room、DataStore、エクスポート
- `:shared`: 分類器・広告解析・検出ルール（Android / iOS で共有）
- DI は Hilt（`SingletonComponent`）。`@AndroidEntryPoint` / `@HiltViewModel` を使用。

## エントリポイント

- `SmartGlassesDetectorApp.kt`: `@HiltAndroidApp` を付与した `Application`
- `MainActivity.kt`: `@AndroidEntryPoint` な `ComponentActivity`。`AppNavigation()` で Compose の `NavHost` を構築
- `service/ScanningForegroundService.kt`: BLE 探索を継続するフォアグラウンドサービス
- `AndroidManifest.xml`: 権限、`MainActivity`、`ScanningForegroundService`（`foregroundServiceType="connectedDevice"`）、`BootReceiver`、`FileProvider` を宣言
- `iosApp/`: SwiftUI。CoreBluetooth が `SmartGlassesDetection`（`:shared`）を呼ぶ

## ディレクトリ構成（`app/src/main/java/jp/smartglasses/detector/`）

```
di/            # AppModule, BluetoothModule, DatabaseModule, RepositoryModule
domain/
  model/       # SmartGlassesDevice, Manufacturer(+DetectionMethod), DetectionLog,
               # DiagnosticLog, DiagnosticLogDeduplication, BluetoothScanFailure
  repository/  # BluetoothRepository, DetectionLogRepository,
               # DiagnosticLogRepository, SettingsRepository (interface)
  service/     # ScanServiceController, ScanResumePolicy, ScanFailurePolicy,
               # ConnectedDevicePolicy
  usecase/     # StartScanningUseCase, StopScanningUseCase,
               # GetDetectionHistoryUseCase, UpdateSettingsUseCase,
               # ResumeScanningIfNeededUseCase, ClearStoredDetectionDataUseCase
data/
  bluetooth/   # SmartGlassesDetector, DetectionCooldownGate,
               # BluetoothRepositoryImpl（分類器は :shared）
  database/    # AppDatabase(Room), DetectionLog(Dao/Entity), DiagnosticLog(Dao/Entity)
  preferences/ # AppPreferences (DataStore ラッパー)
  repository/  # DetectionLogRepositoryImpl, DiagnosticLogRepositoryImpl,
               # SettingsRepositoryImpl
  service/     # ScanServiceControllerImpl
  export/      # DiagnosticLogExporter
receiver/      # BootReceiver（再起動・アップデート・Bluetooth ON で探索復帰）
presentation/
  navigation/  # Screen (onboarding/main/history/settings/about/privacy)
  main/ history/ settings/ onboarding/ about/ privacy/ components/
ui/theme/      # Color, Theme, Type
util/          # Constants (検出ルール / クールダウン / ScanSensitivity),
               # BackgroundScanSupport
MainActivity.kt, SmartGlassesDetectorApp.kt
```

## セットアップ

- JDK 17 以上（ビルドは `compileOptions` / JVM 11）
- Android SDK Platform 37（`compileSdk = 37`）。`targetSdk` は 36
- Gradle Wrapper（Gradle 9.7.1、AGP 9.4.0）。built-in Kotlin を使用
- 依存バージョンは [`gradle/libs.versions.toml`](gradle/libs.versions.toml) のバージョンカタログで一元管理。

## ビルド / テスト / Lint / 型チェック（実在コマンド）

Linux / macOS では `./gradlew`、Windows では `scripts\gradlew-safe.cmd`（[`docs/build-environment.md`](docs/build-environment.md)）を使用します。

```bash
# ビルド
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease

# ユニットテスト（:shared commonTest + app/src/test）
./gradlew :shared:jvmTest
./gradlew :app:testDebugUnitTest

# 計測テスト（app/src/androidTest, 要エミュレータ/実機）
./gradlew connectedAndroidTest

# Android Lint
./gradlew lint
./gradlew lintDebug
./gradlew lintRelease

# クリーン
./gradlew clean
```

- **型チェック**: Kotlin の型検査はコンパイル時に行われます。専用タスクはないため、`compileDebugKotlin` などのコンパイル、または `assembleDebug` / `test` で確認します。
- ktlint / detekt / spotless などのフォーマッタ・静的解析ツールは**設定されていません**。存在しない lint コマンドを追加・記載しないでください。

## コーディング規約

- `kotlin.code.style=official`（`gradle.properties`）に従う。インデントは 4 スペース。
- 依存関係の追加・更新は必ず `gradle/libs.versions.toml` を経由し、`build.gradle.kts` では `libs.*` エイリアスで参照する。バージョンを直書きしない。
- レイヤー依存は `presentation → domain → data` の一方向を維持する。`domain` はフレームワーク非依存の interface / model を置く。
- DI は Hilt を使用。新しい依存は該当する `di/` モジュール（`AppModule` / `BluetoothModule` / `DatabaseModule` / `RepositoryModule`）で提供・バインドする。
- UI は Jetpack Compose + Material 3。テーマは `ui/theme/` を使用する。
- 検出対象メーカーやクールダウン等の定数は `shared/.../util/Constants.kt`（`SMART_GLASSES_DETECTION_RULES`、`COOLDOWN_*`）に集約する。RSSI 下限は `DetectionRssiPolicy` が感度と照合クラスごとに決める。
- 設定キーとデフォルト値は `data/preferences/AppPreferences.kt` に定義（DataStore Preferences）。

## 注意点

1. **BLE スキャンには権限チェックが必要**: 実行前に `BLUETOOTH_SCAN`（Android 12+）や位置情報権限（Android 11 以前）を確認する。Android 12+ の `BLUETOOTH_SCAN` は `neverForLocation` を付け、位置情報権限なしで広告を受け取る。`@SuppressLint("MissingPermission")` を使う場合は呼び出し元で権限を担保する。拡張広告は `ScanSettings.setLegacy(false)` で拾い、端末が非対応なら legacy に落とす。
2. **フォアグラウンドサービス**: `ScanningForegroundService` は `foregroundServiceType="connectedDevice"` で宣言済み。Android 14 以降は `startForeground()` に `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` を渡す。
3. **release の署名**: `app/build.gradle.kts` はルートの `keystore.properties` があれば release 署名を設定する。存在しない場合 release は未署名になる。`keystore.properties` と keystore は**コミットしない**（テンプレートは `keystore.properties.example`）。
4. **リリースビルドの縮小**: release は `isMinifyEnabled = true` / `isShrinkResources = true`。ProGuard/R8 ルールは `app/proguard-rules.pro` を編集する。難読化で壊れやすいクラス（リフレクション利用箇所等）に注意する。
5. **起動・Bluetooth 復帰**: `BootReceiver` が `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / `USER_UNLOCKED` / Bluetooth ON を受け、探索中かつバックグラウンド許可時にフォアグラウンドサービスを再開する。Play の制約のため、ロック中の起動完了インテントは使わない。Android 8+ の暗黙ブロードキャスト制限対策として `SmartGlassesDetectorApp` でも Bluetooth 状態を動的登録する。探索中に権限や Bluetooth が落ちてもサービスは維持し、ハードウェア探索だけ止めて復帰を待つ。
6. **診断ログ**: `data/export/DiagnosticLogExporter` が JSON でエクスポートし、`FileProvider`（`${applicationId}.fileprovider`）経由で共有する。検出記録と調査ログは設定画面 / 記録画面から削除できる。
7. **接続中機器**: 広告を止めたグラスを拾うため、A2DP / Headset / LE Audio / GATT の接続中デバイスも分類する。ヘッドホン Class だけでは検出しない。
8. **テスト用エミュレータ**: `tools/ble_smartglasses_emulator.py` で BLE 広告を模擬送信できる（`Constants.kt` のメーカー定義に対応）。
9. **iOS**: 前面は無フィルタ BLE スキャン、背景はカタログ Service UUID のみ（Apple の制限）。位置情報キーは付けない。Xcode ビルドは [`docs/ios-build.md`](docs/ios-build.md)。

## ドキュメント

- 開発計画: [`PROJECT_PLAN.md`](PROJECT_PLAN.md)
- ビルド環境（Windows ラッパー）: [`docs/build-environment.md`](docs/build-environment.md)
- 署名手順: [`docs/github-release-signing.md`](docs/github-release-signing.md)
- Play 公開チェック: [`docs/play-release-checklist.md`](docs/play-release-checklist.md)
- iOS ビルド: [`docs/ios-build.md`](docs/ios-build.md)
