# Windows ビルド環境メモ

## 推奨コマンド

通常の `gradlew` の代わりに、次を使います。

```powershell
scripts\gradlew-safe.cmd lintRelease
scripts\gradlew-safe.cmd assembleRelease
```

## このラッパーが行うこと

- `GRADLE_USER_HOME` を repo 内の `.gradle-user` に固定
- `ANDROID_USER_HOME` を repo 内の `.android-user` に固定
- `KOTLIN_DAEMON_DIR` を repo 内の `.kotlin-daemon` に固定
- 現在の Windows ユーザーの `%USERPROFILE%\.android\analytics.settings` を事前作成
- `powershell -NoProfile` で起動し、シェルプロファイル起因のノイズを減らす

## metrics 警告について

Android Gradle Plugin の metrics 初期化は、実行ユーザーのホーム配下 `.android` を参照します。
通常の Windows 実行環境では、このラッパーで `%USERPROFILE%\.android\analytics.settings` を用意することで再発を抑えられます。

ただし、Codex sandbox のようにビルドプロセスが別ユーザーのホーム
`C:\Users\CodexSandboxOffline\.android`
を参照する環境では、repo からそのパスへ書き込めないため警告が残ることがあります。
その場合は runner 側で当該ディレクトリを作成してください。
