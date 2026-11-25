# 実装と検証範囲

- Samsung Android 17の抽出済み `services.jar` から、`InstallPackageHelper.preparePackage(InstallRequest)` が低target SDK判定に使うフラグと、リクエスト専用の `InstallArgs` が可変であることを確認しました。抽出物のSHA-256は `f91580934cb971715a117ac640be05c62668c8a30a7d9c1d25f17243a924bce7` です。
- Android 17エミュレーターのAOSPビルドでは同じ判定を確認しましたが、フラグのフィールドはfinalでした。この構造ではフックを登録しません。
- スイッチはAPI 102のリモート設定で保存されます。モジュールはパッケージ準備処理が始まるときに値を参照し、その処理中だけ低target SDKの例外フラグを立てます。処理が終了すると、自分が立てたビットだけを元に戻します。元の戻り値と例外は維持します。
- 0.2.2から設定と処理を低target SDKの互換性に限定しました。上書き更新では既存の低target SDK設定を引き継ぎます。
- 初回はVectorがAndroidシステムにモジュールを読み込む必要があります。読み込み済みのモジュールは、対応するVectorが署名済みAPKの更新を検出するとホット更新を試みます。

参照: [Androidのtarget SDK制限と公式例外](https://developer.android.com/about/versions/15/behavior-changes-all#minimum-target-api-level) · [AOSP InstallPackageHelper](https://android.googlesource.com/platform/frameworks/base/+/main/services/core/java/com/android/server/pm/InstallPackageHelper.java)
