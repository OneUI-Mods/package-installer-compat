#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

tools="${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools/37.0.0"
git diff --quiet HEAD
test -z "$(git ls-files --others --exclude-standard)"
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease

output=app/build/outputs/apk/release
version=$(jq -er '.elements[0].versionName' "$output/output-metadata.json")
name="PackageInstallerCompat-$version"
staging=$(mktemp -d)
trap 'rm -f "$staging/aligned.apk"; rmdir "$staging"' EXIT
mkdir -p dist

"$tools/zipalign" -P 16 -f 4 "$output/app-release-unsigned.apk" "$staging/aligned.apk"
"$tools/apksigner" sign --ks .signing/release.keystore \
    --ks-key-alias legacy-sdk-compat-release --ks-pass file:.signing/password \
    --debuggable-apk-permitted false --out "dist/$name-release.apk" "$staging/aligned.apk"
"$tools/zipalign" -c -P 16 4 "dist/$name-release.apk"
"$tools/apksigner" verify --verbose --print-certs "dist/$name-release.apk" > "dist/$name-signature.txt"
git archive --format=zip --prefix="$name/" --output="dist/$name-source.zip" HEAD
(
    cd dist
    shasum -a 256 "$name-release.apk" "$name-signature.txt" "$name-source.zip" > "SHA256SUMS-$version.txt"
)
printf 'Signed APK: %s\n' "dist/$name-release.apk"
