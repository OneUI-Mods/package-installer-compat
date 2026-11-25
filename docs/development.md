# 実装と検証範囲

- Samsung Android 17の抽出済み `services.jar` から、`InstallPackageHelper.preparePackage(InstallRequest)` が低target SDK判定に使うフラグと、リクエスト専用の `InstallArgs` が可変であることを確認しました。抽出物のSHA-256は `f91580934cb971715a117ac640be05c62668c8a30a7d9c1d25f17243a924bce7` です。
- Android 17エミュレーターのAOSPビルドでは同じ判定を確認しましたが、フラグのフィールドはfinalでした。この構造ではフックを登録しません。
- スイッチはAPI 102のリモート設定で保存されます。モジュールはパッケージ準備処理が始まるときに値を参照します。その処理中だけ既存の低target SDK例外フラグを追加し、終了または例外発生時に自分が追加したビットを外します。元の戻り値・例外は維持します。
- Vectorを注入したGalaxyでの新規導入やフレームワークのホット更新は未検証です。ホット更新コールバックを実装し、自動ホット更新は無効にしています。

参照: [Androidのtarget SDK制限と公式例外](https://developer.android.com/about/versions/15/behavior-changes-all#minimum-target-api-level) · [AOSP InstallPackageHelper](https://android.googlesource.com/platform/frameworks/base/+/main/services/core/java/com/android/server/pm/InstallPackageHelper.java)
