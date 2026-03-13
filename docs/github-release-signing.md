# GitHub Release 用の安全な署名手順

このプロジェクトでは、署名鍵を GitHub やリポジトリに保存しない運用を前提にしています。

## 方針

- `keystore` ファイルはローカルだけで保管する
- `keystore.properties` は Git 管理しない
- GitHub Release に公開するのは署名済み APK / AAB だけ
- `*.jks`、`*.keystore`、`keystore.properties` は絶対に push しない

## 1. keystore を作成する

例:

```powershell
keytool -genkeypair `
  -v `
  -keystore release-keystore.jks `
  -alias release `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

推奨:

- repo の外に保存する
- パスワードはパスワードマネージャーで管理する
- keystore のバックアップを安全な場所に 1 つだけ持つ

## 2. ローカル設定を作る

`keystore.properties.example` を `keystore.properties` にコピーして、実値を入れます。

例:

```properties
storeFile=C:/secure/android/release-keystore.jks
storePassword=your-store-password
keyAlias=release
keyPassword=your-key-password
```

このファイルは `.gitignore` 済みです。

## 3. 署名済み成果物を作る

APK:

```powershell
scripts\gradlew-safe.cmd assembleRelease
```

AAB:

```powershell
scripts\gradlew-safe.cmd bundleRelease
```

出力先:

- APK: `app/build/outputs/apk/release/`
- AAB: `app/build/outputs/bundle/release/`

`keystore.properties` が存在しない場合、release は未署名のままです。

## 4. 署名を確認する

Android SDK Build Tools が使える環境なら、以下で確認できます。

```powershell
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## 5. GitHub Release に公開する

既存 tag に APK を追加する例:

```powershell
gh release upload v1.0 app\build\outputs\apk\release\app-release.apk --clobber
```

新しい tag を作る例:

```powershell
gh release create v1.0.1 `
  app\build\outputs\apk\release\app-release.apk `
  --title "v1.0.1" `
  --notes "Signed APK release."
```

## 6. 注意点

- GitHub に公開してよいのは APK / AAB のみです
- `keystore.properties`、`.jks`、`.keystore` は Release asset にも含めないでください
- Play Store 用の本番配布は APK より AAB を優先してください
