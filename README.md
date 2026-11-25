# Legacy SDK Compatibility

[English](README.en.md)

SamsungのAndroidシステムで、純正パッケージインストーラーに低target SDKと同一署名ダウングレードの互換性設定を追加するVectorモジュールです。root環境とlibxposed API 102対応Vectorが必要です。

1. 署名済みのAPKをインストールします。
2. Vectorでモジュールを有効にして「Androidシステム」を適用範囲に選び、初回だけフレームワークを読み込み直します。
3. アプリを開き、必要な機能をオンにします。各機能の初期値はオフです。読み込み後はスイッチの変更が以降のインストールに反映されます。
4. 普段のファイルマネージャーなどから、Androidのパッケージインストーラーを開きます。

利用できる設定:

- 低いtarget SDKのインストールを許可
- 同一署名の旧バージョンへのダウングレードを許可

署名者の一致、端末ポリシーなどはAndroidが引き続き検証します。異なる署名のAPKによる上書きには対応しません。古いアプリをインストールできても実行時や保存データの互換性は別です。対応する構造を確認できない端末ではフックは無効です。

## 開発

JDK 27、Android SDK 37.2、Build Tools 37.0.0を使用します。 `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease` で検証します。配布用APKは `bash tools/build-release.sh` で作成します。実装と検証範囲は [開発ノート](docs/development.md) を参照してください。
