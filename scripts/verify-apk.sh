#!/usr/bin/env bash
# Fail CI only for a genuinely corrupt/incomplete APK or mismatched package metadata.
# Read expected application metadata from app/build.gradle instead of hard-coding an old release.
set -euo pipefail
cd "$(dirname "$0")/.."

APK="$PWD/app/build/outputs/apk/debug/app-debug.apk"
GRADLE_FILE="$PWD/app/build.gradle"
SDK_TOOLS="${ANDROID_HOME:?ANDROID_HOME must point to Android SDK}/build-tools/35.0.0"
REPORT="$PWD/app/build/outputs/apk/debug/build-report.txt"

fail() { echo "ERROR: $*" >&2; exit 1; }

[[ -s "$APK" ]] || fail "APK does not exist or is empty: $APK"
[[ -f "$GRADLE_FILE" ]] || fail "Missing app/build.gradle"
for cmd in zipalign apksigner aapt; do
  [[ -x "$SDK_TOOLS/$cmd" ]] || fail "Missing SDK build tool: $SDK_TOOLS/$cmd"
done

# Reading exact package metadata from the current build avoids an accidental CI failure
# when versionCode and versionName change in future releases.
APP_ID=$(sed -nE "s/^[[:space:]]*applicationId[[:space:]]+['\"]([^'\"]+)['\"].*/\1/p" "$GRADLE_FILE")
VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+)([[:space:]]|$).*/\1/p' "$GRADLE_FILE")
VERSION_NAME=$(sed -nE "s/^[[:space:]]*versionName[[:space:]]+['\"]([^'\"]+)['\"].*/\1/p" "$GRADLE_FILE")
MIN_SDK=$(sed -nE 's/^[[:space:]]*minSdk[[:space:]]+([0-9]+)([[:space:]]|$).*/\1/p' "$GRADLE_FILE")
[[ -n "$APP_ID" && -n "$VERSION_CODE" && -n "$VERSION_NAME" && -n "$MIN_SDK" ]] || fail "Cannot read package/version/minSdk from app/build.gradle"

unzip -tq "$APK" || fail "APK ZIP structure is corrupt"
ZIP_LISTING="$(unzip -Z1 "$APK")"
grep -Fxq 'classes.dex' <<< "$ZIP_LISTING" || fail "Missing classes.dex"
grep -Fxq 'AndroidManifest.xml' <<< "$ZIP_LISTING" || fail "Missing AndroidManifest.xml"

"$SDK_TOOLS/zipalign" -c -v 4 "$APK" || fail "APK alignment check failed"
"$SDK_TOOLS/apksigner" verify --verbose --print-certs "$APK" || fail "APK signature verification failed"
BADGING="$("$SDK_TOOLS/aapt" dump badging "$APK")" || fail "aapt could not read package metadata"

# A mismatch in these values means the APK is not the build described by this source.
grep -Fq "package: name='$APP_ID'" <<< "$BADGING" || fail "Wrong applicationId (expected $APP_ID)"
grep -Fq "versionCode='$VERSION_CODE'" <<< "$BADGING" || fail "Wrong versionCode (expected $VERSION_CODE)"
grep -Fq "versionName='$VERSION_NAME'" <<< "$BADGING" || fail "Wrong versionName (expected $VERSION_NAME)"
grep -Fq "sdkVersion:'$MIN_SDK'" <<< "$BADGING" || fail "Wrong minSdk (expected $MIN_SDK)"

{
  echo "Froggy Pilot Auto — debug APK verification"
  echo "APK bytes: $(stat -c '%s' "$APK")"
  echo "Expected metadata from Gradle: $APP_ID $VERSION_NAME (code $VERSION_CODE); minSdk $MIN_SDK"
  sha256sum "$APK"
  grep -E '^package:|^sdkVersion:|^targetSdkVersion:' <<< "$BADGING"
  "$SDK_TOOLS/apksigner" verify --verbose "$APK"
} | tee "$REPORT"
echo "PASS: Real APK is signed, aligned, contains dex + manifest, and matches current Gradle metadata."
