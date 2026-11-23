# Froggy Pilot Auto — Source for Android APK (offline)

This is a **new Froggy/eatery management game** project based on screenshots sampled from the eleven user-provided recordings `1(1).mp4` … `11.mp4`. It is **not** the older BubblePilot bubble-shooting game. The gameplay in the recordings features frogs, food orders, upgrade menus, recipe/level unlocks, coin-price buttons and dismissible reward/management panels.

## What is included

- Android Java application, `MainActivity` with explicit CLICK Accessibility and FLOAT overlay permission shortcuts. CAPTURE consent is shown on Android 8–10 only; Android 11+ uses Accessibility screenshot API. No root or AI API key.
- Draggable floating controls: ▶ Start, ■ Stop, × close, status. Stop invalidates all pending screenshots/tap callbacks, preventing additional taps after stopping. Game package locks on the first foreground third-party app seen after tapping ▶, and automation pauses when another app becomes foreground.
- Local `RestaurantVision`: detects filled green button regions, distinguishes **coin price icon** (yellow) from gem-related offers (blue), recognizes some modal panels / close icons, and can periodically open the green-arrow upgrade tab. The detector processes screenshots entirely on device, using no server and no networking permission.
- `tests/frames/`: 11 extracted 360x640 screenshots (one for each video) and two high-resolution reference frames. `tests/VisionTest.java` gives a repeatable, offline JVM regression; `tests/frame-results.txt` records local results.
- GitHub Actions: Java/JVM frame tests → Android compile → ZIP/dex/manifest/alignment/signature/version verification → emulator installation + activity launch for Android API 28, 30, 35 → publish artifact only when tests succeed.

## How to build on GitHub

1. Create a new repository; **extract the source ZIP contents to the repository root**. Upload all files including `.github/workflows/build-apk.yml` (the `.github` directory may be hidden in some file managers).
2. Commit/push to `main`, then visit **Actions → Build and verify Froggy Pilot APK → Run workflow**.
3. After all jobs finish, open the workflow run → Artifacts → **`FroggyPilot-Auto-InstallChecked-APK`** → download. Extract and install `app-debug.apk` on a compatible Android device (Android 8+).
4. If a job fails, read its log. Only the *InstallChecked* artifact has passed the scripted emulator install/launch smoke checks. These tests cannot verify real game interactions or permissions in the user's cloud phone.

Uses JDK 17, Gradle 8.9, Android Gradle Plugin 8.6.1, Android SDK platform 35, Build-Tools 35.0.0. The workflow does **not** try to install the removed `tools` SDK package. `scripts/verify-apk.sh` reads the actual package/version values from `app/build.gradle` rather than comparing against an obsolete version.

## How to operate

1. Install and open Froggy Pilot Auto.
2. Enable **CLICK** in system Accessibility and **FLOAT** in system overlay settings; on Android 8–10 additionally grant **CAPTURE** via the screen-share dialog.
3. Turn on/off the auto-coin-upgrade, continue and upgrade-tab options. Tap **START — MỞ THANH NỔI**.
4. Open the frog restaurant game, position the small floating control outside the gameplay area and tap **▶** to automate.
5. Tap **■** to stop (does not automatically resume), or **×** to stop and close the bar. If you switch away from the locked game package, automation pauses until the same game is back in front.

## Important limitations (read before use)

The eleven videos are long, with many missions and temporary menus. The screenshots were inspected and the Java recognizer was regression-tested on **selected** frames only. This is a **best-effort, rules-based automation prototype**: it is *not demonstrated to finish all stages autonomously*, solve every customer order, distinguish all reward/ad dialogs, or recognize each future UI variation. In particular, there is no full gameplay simulation or language OCR. The engine deliberately **does not purchase gem/real-money/ad offers**, even if that slows or blocks progress. A tap on a green button is not an assurance that the game will accept the purchase. Please test on your own game/version and monitor actions before allowing prolonged automation.

For reproducible proof of actual gameplay behavior, record a screen session from the built APK on the target Android/CloudPhone device, including gameplay, permissions and Start/Stop, then use that evidence for follow-up recognition improvements. Current ZIP creation does not imply emulator tests or real-world game testing have already passed; those checks execute when GitHub Actions runs.

## Folder overview

```
.github/workflows/build-apk.yml
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/froggypilot/{MainActivity,GameAccessibilityService,RestaurantVision,LegacyProjectionService,LocalSettings}.java
app/src/main/res/{values/strings.xml,xml/service_config.xml}
settings.gradle
build.gradle
gradle.properties
scripts/verify-apk.sh
tests/test.sh
tests/VisionTest.java
tests/frames/*.jpg
build-apk.sh
```
