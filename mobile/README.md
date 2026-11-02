# ATG Mobile (Flutter)

Cross-platform client for **ATG — Accurate Trading Group**, sharing one codebase between Android and iOS.
It talks to the Spring Boot API in `../backend` (JWT auth, single PostgreSQL/H2 database), so both
platforms read and write the same accounts, signals, rooms, wallets and requests.

## Requirements

- Flutter 3.35+ (Dart 3.13) — this project was built with Flutter 3.47.x
- Android SDK (for APK) — `flutter doctor --android-licenses`
- macOS + Xcode 16 + an Apple Developer account (for an iOS IPA)

Add Flutter to the path for the current shell (Windows Git Bash):

```bash
export PATH="/c/src/flutter/bin:$PATH"
export ANDROID_HOME="C:/Users/User/AppData/Local/Android/Sdk"
```

## Run

Start the backend first (`../backend`, port 8080), then:

```bash
flutter pub get
flutter run                     # attached device / emulator
flutter run -d emulator-5554    # a specific Android emulator
flutter run -d ios              # on a Mac, with the iOS simulator
```

Server address defaults and can be changed inside the app (Home → server row → *Profile → Server address*):

| Platform        | Default API base URL      |
| --------------- | ------------------------- |
| Android emulator| `http://10.0.2.2:8080/`   |
| iOS simulator   | `http://127.0.0.1:8080/`  |
| Physical phone  | `http://<LAN-IP>:8080/`   |

Cleartext HTTP is allowed in development (`usesCleartextTraffic` on Android,
`NSAllowsArbitraryLoads` in `ios/Runner/Info.plist`). Switch the backend to HTTPS and remove both
flags before shipping.

## Build

Android APK:

```bash
flutter build apk --debug
flutter build apk --release       # signed with android/key.properties if configured
```

iOS IPA (macOS only):

```bash
flutter build ios --release --no-codesign   # verify it compiles
open ios/Runner.xcworkspace                 # set team + bundle id, then Archive
flutter build ipa --release                 # requires -DARB signing identity
```

The iOS build cannot be produced on Windows; the Dart code, assets and plugins are already
platform-agnostic, so the same source compiles unchanged on a Mac.

## iOS project state

- Bundle id `com.atg.atg_mobile` (Runner and RunnerTests), display name `ATG`, deployment target 15.0,
  iPhone + iPad.
- Swift Package Manager integration (`FlutterGeneratedPluginSwiftPackage` in the Xcode project), so no
  `Podfile` is needed; Flutter resolves the plugin package on the first Mac build.
- `Info.plist`: camera, photo-library and local-network usage strings, plus an ATS exception so the
  dev server can use plain HTTP. Remove the ATS exception when the API is on HTTPS.
- `Assets.xcassets`: full `AppIcon` set (15 sizes) and `LaunchImage` (1x/2x/3x) built from the supplied
  gold-bull logo by `python tool/build_brand_assets.py <logo.png>` (Pillow). The same script writes the
  Android `ic_launcher` squares, adaptive `ic_launcher_foreground` layers and per-density
  `splash_logo` bitmaps, plus `assets/images/atg_mark.png` for the in-app logo and splash.
- Launch screen: brand black (`#0A0A0A`) with the gold bull, wordmark and tagline. On Android 12+ the
  system splash uses the adaptive foreground; the Flutter splash (`lib/screens/splash_screen.dart`)
  rolls the bull in, then text fades up, then the app fades in after ~2.2 s.
- `PrivacyInfo.xcprivacy` declares UserDefaults, file-timestamp and disk-space access (required-reason
  APIs used by preferences and lesson-note export) and is in the Runner Resources build phase.
- Physical iPhone: the default server `http://127.0.0.1:8080/` only works on the simulator. On a phone
  use **Server** on the sign-in screen and enter `http://<your-computer-LAN-IP>:8080/`.
- On a Mac: `flutter pub get`, `open ios/Runner.xcworkspace`, pick a signing team, then
  `flutter run -d ios` (simulator) or `flutter build ipa --release` (device / TestFlight).

### Without a Mac

The repository ships `.github/workflows/ios.yml` and `codemagic.yaml`, which build the same app on a
cloud macOS runner. The GitHub workflow uploads `Runner-unsigned.ipa`; install it from Windows with
Sideloadly (free Apple ID = 7-day build, paid developer account = 1 year). See the root README section
“Build iOS without a Mac” for the full steps and signing options.

## Test

```bash
flutter analyze    # must report "No issues found!"
flutter test
```

The first Android build downloads the NDK, CMake and an SDK platform, so it can take 10–15 minutes;
later builds are incremental. Verified output: `build/app/outputs/flutter-apk/app-debug.apk` and
`app-release.apk` (package `com.atg.atg_mobile`, label `ATG`, minSdk 24, targetSdk 36). The release
APK is debug-signed for now; configure `android/key.properties` with a real keystore before publishing.

## Live updates

Both platforms share one database, and the client keeps itself current:

- `lib/realtime.dart` opens `ws(s)://<server>/ws?token=<JWT>` after sign-in and closes on sign-out or
  server change (driven by `Session` notifications).
- The server only sends invalidation scopes (`rooms`, `room:<id>`, `signals`, `courses`, `settings`,
  `users`, `requests`, `wallet:<userId>`, `home:<userId>`); screens then refetch through the normal
  API, so authorization and VIP gating stay server-side.
- Screens subscribe with `realtime.bind({...scopes}, reload)`: home, signals, markets' ticker data,
  rooms, room chat (the old 6-second polling timer is gone), wallet, profile, academy, course detail
  and every admin tab.
- Reconnect uses exponential backoff (1 s → 30 s), a 20 s heartbeat keeps intermediaries from closing
  idle sockets, bursts are debounced, and a reconnect triggers a resync of open screens.
- The header dot is the status: green `LIVE`, grey `SYNC` while reconnecting.

## Responsive layout

`lib/responsive.dart` holds the breakpoints (600 / 900 / 1280) and `PageBody`, which centres content
and caps its width per screen (820–1080). Phones are unchanged; tablets and desktop web keep a
readable column. Signals become a two-column grid from 900 px. Web/desktop builds also accept
mouse and trackpad drags (`AtgScrollBehavior`).

## Runtime verification

There is no emulator or phone on this machine, so the UI was walked in a browser against the live API
using the same Dart code (`flutter build web --release`, served locally — `web/` exists for that
purpose and is not a shipping target). Covered: both roles' sign-in, home, signals and detail, markets
with favourites, room chat posting, rooms feed, wallet, deposit, 4-step withdrawal with proof upload,
VIP purchase, and every admin tab. Approving the test records changed the account to Weltrade VIP and
the room list reflected it (`Weltrade VIP` unlocked, `Deriv VIP` locked). The release and debug APKs
include the same code; still, run the app once on a real device before shipping.

Proof uploads used `MultipartFile.fromPath`, which only exists on `dart:io` and failed outside mobile;
they now use `MultipartFile.fromBytes`, so the same code path works on Android, iOS and web.

Live updates were checked with two independent browser origins (learner on `localhost:5050`, admin on
`127.0.0.1:5050`, separate `localStorage`, one API and database): a deposit created for the learner
showed up as a pending card in the admin's Requests tab, approving it flipped the learner's wallet row
from `PENDING` to `COMPLETED` on screen, and a desk message appeared in an open chat — all without a
manual refresh. A Node script also asserted the raw socket behaviour, including `1008` for a bad JWT.

The bottom of Home carries the Weltrade registration card (three steps, the affiliate link opened with
`url_launcher` in the system browser, and the "Accurate Trading Group. Trading carries risk." notice),
and that tab is now labelled **Community** rather than Rooms.

The splash was checked in the same harness: on reload the black brand screen appears, the gold bull
rolls in (a full turn, easing out), the wordmark and green tagline fade up, then the app fades in
after ~2.2 s. The in-app mark shows in the home header, the sign-in card and the community card.

## Structure

```
lib/
  main.dart, theme.dart, widgets.dart, format.dart, responsive.dart
  models.dart, api.dart, session.dart, realtime.dart, app_state.dart
  screens/
    splash_screen.dart, auth_screen.dart, shell.dart, home_screen.dart
    signals_screen.dart, signal_detail_screen.dart
    markets_screen.dart, pair_detail_screen.dart
    rooms_screen.dart, room_chat_screen.dart
    wallet_screen.dart, deposit_screen.dart, withdraw_screen.dart
    academy_screen.dart, course_detail_screen.dart, lesson_screen.dart
    glossary_screen.dart, pip_calculator_screen.dart
    profile_screen.dart, admin_screen.dart
assets/images/
  atg_mark.png               # gold bull mark (transparent) used by AtgLogo and the splash
tool/
  build_brand_assets.py      # logo -> app icons, launch images, Android splash bitmaps
```

Navigation uses `CupertinoTabBar` + iOS page transitions on iOS and `NavigationBar` + Android
transitions on Android; every other screen is shared.

## Store policy note

Deposits, withdrawals and VIP/course payments are reviewed manually by an admin against uploaded
proofs, matching the original app. When publishing to the App Store or Google Play, check their
rules: digital courses and VIP memberships sold inside the app may need Apple In-App Purchase or
Google Play Billing, while the deposit/withdrawal desk may require an external-purchase
entitlement.
