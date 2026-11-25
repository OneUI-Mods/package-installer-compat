# Legacy SDK Compatibility

[日本語](README.md)

A Vector module adding low-target-SDK and same-signature downgrade controls to Samsung's Android package installer. Root and a Vector build with libxposed API 102 are required.

1. Install the signed APK.
2. Enable the module in Vector, scope it to **Android System**, and reload the framework once.
3. Open the module app and enable the compatibility options you need. Every option defaults to off. After the initial load, switch changes apply to subsequent installations.
4. Open the APK in Android's regular package installer.

Available controls:

- Allow low target SDK installs
- Allow downgrades to an older APK with the same signing identity

Android still verifies the signing identity and device policy. Replacing an installed app with an APK from a different signer is intentionally unsupported. Installation does not guarantee runtime or stored-data compatibility. The hook is inactive when it cannot confirm a compatible framework structure.

For build instructions and verification scope, see the [development notes](docs/development.md).
