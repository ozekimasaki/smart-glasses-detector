# CLAUDE.md - スマートグラス検出アプリ

## プロジェクト概要

- **アプリ名**: スマートグラス検出
- **パッケージ名**: `jp.smartglasses.detector`
- **minSdk**: 26 (Android 8.0) / **targetSdk**: 35 (Android 15) / **compileSdk**: 37
- **目的**: BLE スキャンで近くのスマートグラスを検出し通知するアプリ

## アーキテクチャ

MVVM + Clean Architecture の3層構造:

```
presentation/ → domain/ → data/
```

- **presentation/**: Jetpack Compose UI、ViewModel
- **domain/**: UseCase、Repository interface、Model
- **data/**: RepositoryImpl、BLE、Room、DataStore

DI は Hilt（`@Singleton` スコープ）、`SingletonComponent` に集約。

## 技術スタック

| カテゴリ | ライブラリ | バージョン |
|---------|-----------|-----------|
| 言語 | Kotlin | 2.4.20 |
| UI | Jetpack Compose (BOM) | 2026.09.00 |
| DI | Hilt | 2.60.1 |
| DB | Room | 2.8.5 |
| 設定永続化 | DataStore Preferences | 1.2.1 |
| 非同期 | Coroutines + Flow | (lifecycle 2.11.0) |
| ナビゲーション | Compose Navigation | 2.10.1 |
| ビルドシステム | Gradle KTS + libs.versions.toml | AGP 9.4.0 |
| コード生成 | KSP | 2.3.12 |

## 主要ファイル構造

```
app/src/main/java/jp/smartglasses/detector/
├── di/
│   ├── BluetoothModule.kt       # BluetoothManager/Adapter の DI 提供
│   ├── DatabaseModule.kt        # Room DB / DAO の DI 提供
│   └── RepositoryModule.kt      # Repository 実装の DI バインド
├── domain/
│   ├── model/
│   │   ├── SmartGlassesDevice.kt
│   │   ├── Manufacturer.kt      # DetectionMethod (COMPANY_ID / DEVICE_NAME)
│   │   └── DetectionLog.kt
│   ├── repository/
│   │   ├── BluetoothRepository.kt
│   │   └── DetectionLogRepository.kt
│   └── usecase/
│       ├── StartScanningUseCase.kt
│       ├── StopScanningUseCase.kt
│       ├── GetDetectionHistoryUseCase.kt
│       └── UpdateSettingsUseCase.kt
├── data/
│   ├── bluetooth/
│   │   ├── SmartGlassesDetector.kt   # BLE スキャン + 検出ロジック中核
│   │   └── BluetoothRepositoryImpl.kt
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   ├── DetectionLogEntity.kt
│   │   └── DetectionLogDao.kt
│   ├── repository/
│   │   └── DetectionLogRepositoryImpl.kt
│   └── preferences/
│       └── AppPreferences.kt         # DataStore ラッパー
├── service/
│   └── ScanningForegroundService.kt  # BLE バックグラウンド動作の核心
├── presentation/
│   ├── main/        # メイン画面 (スキャン開始/停止)
│   ├── history/     # 検出履歴画面
│   ├── settings/    # 設定画面
│   └── onboarding/  # 初回起動・権限説明
└── util/
    └── Constants.kt  # メーカー ID マップ / クールダウン定数 / Enum
```

## 検出ロジック

`SmartGlassesDetector` が2段階で検出:

1. **Company ID 検出** (`Constants.SMART_GLASSES_MANUFACTURER_IDS`):
   - Seiko Epson (0x0040), Apple (0x004C), Google (0x00E0), Amazon (0x0171),
     Meta (0x01AB / 0x058E), Huawei (0x027D), Lenovo (0x02C5), Meizu (0x03AB),
     Snapchat (0x03C2), TCL (0x0BC6), Luxottica (0x0D53)

2. **デバイス名パターン検出** (`Constants.SMART_GLASSES_NAME_PATTERNS`):
   - XREAL, Rokid, INMO, Looktech, LAWAKEN, Halliday, VITURE

**クールダウン**:
- 同一デバイス: 30秒 (`COOLDOWN_SAME_DEVICE_MS`)
- 同一メーカー: 15秒 (`COOLDOWN_SAME_MANUFACTURER_MS`)

## スキャン感度 (ScanSensitivity enum)

| 値 | BLE ScanMode | 用途 |
|----|-------------|------|
| `LOW_POWER` | SCAN_MODE_LOW_POWER | バッテリー節約 |
| `BALANCED` | SCAN_MODE_BALANCED | 標準（デフォルト） |
| `HIGH_ACCURACY` | SCAN_MODE_LOW_LATENCY | 高精度 |

## バックグラウンド動作

`ScanningForegroundService`:
- `ACTION_START` / `ACTION_STOP` インテントで制御
- `START_STICKY` で再起動対応
- Android 14+ は `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` 必須
- 検出時: DB 保存 → 通知 → バイブレーション

通知チャンネル:
- `scanning_channel`: 常駐通知（PRIORITY_LOW、スワイプ不可）
- `detection_channel`: 検出通知（PRIORITY_HIGH、ヘッドアップ）

## 設定 (AppPreferences / DataStore)

| キー | デフォルト | 型 |
|-----|----------|---|
| `background_enabled` | true | Boolean |
| `notification_enabled` | true | Boolean |
| `vibration_enabled` | true | Boolean |
| `sound_enabled` | true | Boolean |
| `sensitivity` | 1 (BALANCED) | Int (0/1/2) |
| `onboarding_completed` | false | Boolean |
| `is_scanning` | false | Boolean |

## 権限

```xml
BLUETOOTH_SCAN, BLUETOOTH_CONNECT           <!-- Android 12+ -->
ACCESS_FINE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN  <!-- Android 11以前 -->
FOREGROUND_SERVICE, FOREGROUND_SERVICE_CONNECTED_DEVICE
VIBRATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED
```

## デザイン方針

- Material Design 3 準拠
- カラーパレット: 暖色系（Orange / Amber / Coral）
- 背景: クリーム色 (`0xFFFFF8E1`)
- 非エンジニアユーザー向け: 技術用語なし、丸みのある UI

## 開発時の注意点

1. **BLE スキャンには `@SuppressLint("MissingPermission")` が必要** — 権限チェックは呼び出し元で行う
2. **`ManufacturerSpecificData` の読み方**: `data.get(0)` でインデックス 0 取得後、リトルエンディアン 2 バイトで Company ID を構築
3. **フォアグラウンドサービス**: Android 14 以降は `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` を `startForeground()` に渡す
4. **`isMinifyEnabled = true`**（release）。ProGuard ルールは `app/proguard-rules.pro`
5. **起動復帰**: `BootReceiver` が再起動・アップデート・Bluetooth ON で探索を再開する
6. **検出パイプライン**: Company ID / Service UUID / 広告ペイロード / デバイス名 / GAP Appearance / 未知メーカーヒューリスティック

## 実装済みの主要コンポーネント

- `presentation/main/MainScreen.kt` / `MainViewModel.kt`
- `presentation/history/HistoryScreen.kt` / `HistoryViewModel.kt`
- `presentation/settings/SettingsScreen.kt`
- `presentation/onboarding/OnboardingScreen.kt`
- `ui/theme/`（Color.kt, Theme.kt, Type.kt）
- `receiver/BootReceiver.kt`
- `MainActivity.kt`
- `DetectionMethod`（COMPANY_ID / DEVICE_NAME / SERVICE_UUID / PAYLOAD / APPEARANCE / HEURISTIC）
