# iOS アプリのビルド

iOS アプリは macOS と Xcode 15 以上が必要です。Linux の CI では Kotlin 共有モジュールと Android だけを検証します。

## 構成

- `shared/`: Kotlin Multiplatform。検出ルール・広告解析・分類器
- `iosApp/`: SwiftUI + CoreBluetooth。分類は `Shared.framework` の `SmartGlassesDetection` を呼ぶ
- Bundle ID: `jp.smartglasses.detector`
- 最低 OS: iOS 17

## Xcode でのビルド

1. JDK 17 と Android SDK（`compileSdk` 37）を入れておく（Kotlin フレームワークの Gradle ビルドで使う）
2. `iosApp/SmartGlassesDetector.xcodeproj` を開く
3. Signing Team を設定する
4. 実機またはシミュレータで Run する

Xcode の Run Script「Compile Kotlin Framework」が次を実行します。

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

このタスクは macOS でのみ iOS ターゲットを有効化します（`shared/build.gradle.kts`）。

## 検出の範囲（iOS の API 限界）

- 前面: フィルタなしの `scanForPeripherals(withServices: nil)`
- この iPhone に接続中: カタログの Service UUID で `retrieveConnectedPeripherals(withServices:)`
- バックグラウンド: `bluetooth-central`。Apple が無フィルタの背景スキャンを禁止しているため、カタログの Service UUID だけをフィルタする
- 位置情報権限は使わない（`NSBluetoothAlwaysUsageDescription` のみ）

Classic Bluetooth inquiry、HID Host、他人の端末に接続済みで広告を止めたグラスは、公開 API が無いため検出しません。
