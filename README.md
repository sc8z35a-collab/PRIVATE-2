# PocketPet Ultra

スマホの中で独自ペットを育てる、完全オフライン型Android育成ゲームです。Kotlin + Jetpack Composeで実装しています。

## 主な機能

- 複数ペット: モチ / ルミ / ノリ / ピコ / ソラ
- 名前変更、レベル、経験値、3段階成長、親密度
- 空腹 / ごきげん / 清潔 / 元気のリアルタイム状態
- アプリを閉じている間も最大48時間分の状態変化を反映
- ごはん、遊ぶ、なでる、掃除、睡眠
- ゲーム内コイン / ジェム（実課金なし）
- フード、おもちゃ、服、家具、ルームテーマ、ペットのたまごを購入できるショップ
- インベントリ、服装変更、ルームテーマ変更
- 10秒スターキャッチ・ミニゲーム
- デイリーログイン、デイリークエスト、実績
- SharedPreferences + JSONによるローカル自動保存
- バックアップ文字列のコピー / 復元
- 初期化導線
- セーブ整合性・通貨・数量・ペットID等の自己診断
- 負数通貨、オーバーフロー、ユニーク商品の二重購入、報酬二重受取を防止
- ネットワーク権限なし

## APK

GitHub Actionsの `Android APK` ワークフローが、push時に以下を実行します。

1. JDK 17 / Android SDK 35 / Gradle 8.9を準備
2. `testDebugUnitTest` でゲームルールを検証
3. `assembleDebug` でAndroid APKをビルド
4. `PocketPet-Ultra-APK` というArtifactとして `app-debug.apk` を保存

Debug APKはAndroidの標準debug keyで署名されるため、そのまま実機へインストールできます。初回はAndroid側で「不明なアプリのインストール」を許可する必要がある場合があります。

## ローカルビルド

Android Studioでリポジトリを開いて同期後、`app` を実行してください。CLIならGradle 8.9とJDK 17で以下を実行できます。

```bash
gradle testDebugUnitTest assembleDebug
```

APK出力先:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## データ方針

ゲーム進行は端末内のみで完結します。広告SDK、分析SDK、外部API、実課金処理は入れていません。
