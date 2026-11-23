#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tests/test.sh
gradle --no-daemon :app:assembleDebug
./scripts/verify-apk.sh
printf 'APK ready at app/build/outputs/apk/debug/app-debug.apk\n'
