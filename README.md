# Package Installer Compatibility

インストールにはAndroid 17（API 37）以降が必要です。

[English](README.en.md)

Samsungの純正パッケージインストーラーで、古いtarget SDKを指定するAPKをインストールできるようにするVectorモジュールです。root環境とlibxposed API 102対応Vectorが必要です。

1. 署名済みのAPKをインストールします。
2. Vectorでモジュールを有効にして「Androidシステム」を適用範囲に選び、初回だけフレームワークを読み込み直します。
3. アプリを開き、「低いtarget SDKのインストールを許可」をオンにします。初期値はオフです。読み込み後はスイッチの変更が以降のインストールに反映されます。
4. 普段のファイルマネージャーなどから、Androidのパッケージインストーラーを開きます。

署名、アプリのバージョン、端末ポリシーはAndroidが通常どおり検証します。古いアプリがインストールできても、そのアプリが正常に動くとは限りません。対応する構造を確認できない端末ではフックは無効です。

## 開発

JDK 27、Android SDK 37.2、Build Tools 37.0.0を使用します。 `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease` で検証します。配布用APKは `bash tools/build-release.sh` で作成します。実装と検証範囲は [開発ノート](docs/development.md) を参照してください。
