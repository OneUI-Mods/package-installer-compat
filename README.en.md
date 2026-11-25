# Legacy SDK Compatibility

[日本語](README.md)

A Vector module for installing legacy-target APKs through Samsung's Android package installer. Root and a Vector build with libxposed API 102 are required.

1. Install the signed APK.
2. Enable the module in Vector, scope it to **Android System**, and reload the framework once.
3. Open the module app and turn on **Allow low target SDK installs**. The switch defaults to off. After the initial load, switch changes apply to subsequent installations.
4. Open the APK in Android's regular package installer.

Android still checks signatures, downgrade rules, and device policy. Installation does not guarantee that a legacy app can run. The hook is inactive when it cannot confirm a compatible framework structure.

For build instructions and verification scope, see the [development notes](docs/development.md).
