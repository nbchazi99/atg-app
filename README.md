# ATG — Accurate Trading Group

Trading education, signals and VIP membership platform, rebuilt from the ZFA screenshot set.
The shipping client is a **Flutter app that runs on both Android and iOS from one codebase**; the
backend is **Java Spring Boot** with a single database, so every device sees the same accounts,
signals, rooms, wallets and requests. The original **Java Android app** is kept in `android/` as a
reference implementation.

## What is in the box

**Trader app**

- Sign in / register, persistent session, configurable server address.
- Home: greeting, points, live calls, promo card, quick actions, market tickers, daily fact, referral card
  and a Weltrade account-registration card (steps, partner link, risk notice).
- Signals: Live / Open / Inner / Closed filters, full level detail (entry, stop, targets, rule, participants).
- Markets: Favourites / Forex / Metals / Crypto, search, live pair detail with stats and calls, star favourites.
- Community: main floor + Deriv VIP + Weltrade VIP, open/closed/all feed, room chat with posting.
  VIP rooms and their feed posts stay locked until the matching membership is approved.
- Wallet: balance history, pending request counts, deposit and withdrawal entry points.
- Deposit: broker, client ID, amount, fee summary, wallet, mobile, payer reference.
- Withdrawal: four steps — payout, source and amount, proof upload, done.
- Academy: courses with progress, lessons with completion, downloadable lesson notes (`.txt` export),
  and optional video links that open in the device's video app or browser. Course purchase by payment proof.
- Tools: glossary (22 terms) and pip calculator.
- Profile: membership, purchases, transaction history, server setting, admin entry, sign out.

**Administrator (inside the same app, admin accounts only)**

- Requests: deposits, withdrawals and purchases with approve / reject and a note.
- Users: change membership (none, Deriv VIP, Weltrade VIP).
- Signals: create and delete calls.
- Courses: create (with lessons) and delete. Lesson lines use
  `Title | minutes | content | video URL`, where the video URL is optional.
- Settings: deposit fee, withdrawal fee, min/max amounts, daily fact, promotion copy.

Seed data on a fresh database: 15 market pairs, 7 signals, 3 rooms with chat, 3 courses, 2 accounts.

## Repository layout

```
mobile/      Flutter client (Android + iOS, shared Dart code) — the shipping app
backend/     Spring Boot API (JWT, JPA, H2 dev / PostgreSQL prod)
android/     Original Java Android app, kept as a reference implementation
```

`mobile/web/` is the standard Flutter web shell. It is not a shipping target; it is how the UI was
smoke-tested against the live API without a device (`flutter build web --release`).

## Requirements

- Flutter 3.47+ / Dart 3.13+ for the mobile client (`mobile/README.md` has the details).
- JDK 21 for the backend; Android SDK 36 and JDK 17+ for the Android build of either client.
- Gradle 8.14+ (or open a project in Android Studio).
- macOS + Xcode 16 + an Apple Developer account to produce the iOS IPA.
- Optional: PostgreSQL 14+ for production storage.

## Run the API

```bash
cd backend
gradle bootRun                       # or: ./gradlew bootRun
```

A prebuilt jar is also available:

```bash
java -jar build/libs/atg-api-1.0.0.jar
```

- Default `dev` profile uses embedded H2 stored in `backend/data/atg`, so there is nothing to install.
- Uploaded payment proofs go to `backend/uploads` and are served at `/uploads/**`.
- API root: `http://localhost:8080/api`.

Production with PostgreSQL:

```bash
ATG_DB_URL=jdbc:postgresql://localhost:5432/atg \
ATG_DB_USER=atg \
ATG_DB_PASSWORD=atg \
ATG_BASE_URL=https://api.example.com \
java -jar build/libs/atg-api-1.0.0.jar --spring.profiles.active=prod
```

`spring.jpa.hibernate.ddl-auto=update` creates the schema on first start.

## Run the app (Flutter — Android and iOS)

```bash
cd mobile
flutter pub get
flutter run                       # Android emulator / phone
flutter run -d ios                # iOS simulator, on a Mac
```

Builds:

```bash
flutter build apk --release       # Android APK
flutter build appbundle --release # Play Store
flutter build ipa --release       # iOS IPA (macOS + Xcode only)
```

Server address:

- The app is pinned to the hosted API, `https://atg-api-5de3.onrender.com/`, in
  `mobile/lib/session.dart`. There is no in-app editor; changing the host means a rebuild.
- To run the API locally for development, point that constant at your machine and rebuild.

Cleartext HTTP is enabled for local development (`usesCleartextTraffic="true"` on Android, an ATS
exception in `mobile/ios/Runner/Info.plist`). Use HTTPS and a real JWT secret before shipping.

## Build iOS without a Mac

Xcode only runs on macOS, so the iOS app is compiled on a **cloud macOS runner** from this repository:

- `.github/workflows/ios.yml` — GitHub Actions `macos-14` job: analyzes, tests, runs
  `flutter build ios --release --no-codesign`, packages `Runner-unsigned.ipa` and uploads it as a
  workflow artifact. Free for public repositories (private repos spend macOS minutes at a 10x rate).
- `codemagic.yaml` — the same unsigned build on Codemagic (`mac_mini_m2`, 500 free minutes/month).

Steps:

1. Put this folder in a GitHub repository (`git init`, commit, push).
2. Open **Actions → iOS build → Run workflow** (or push to `main`). The first run downloads the
   Flutter SDK and takes 10–20 minutes.
3. Download the `Runner-unsigned-ipa` artifact, then install it on an iPhone from Windows with
   **Sideloadly** (or AltStore): connect the phone, drag the `.ipa`, sign in with an Apple ID.
   - Free Apple ID: app works for 7 days, max 3 sideloaded apps, re-sign weekly; on iOS 16+ first
     enable **Settings → Privacy & Security → Developer Mode**.
   - Paid Apple Developer Program ($99/year): 1-year signing, up to 100 devices, and TestFlight /
     App Store distribution through Codemagic or a rented Mac.
4. For TestFlight, connect an App Store Connect API key to Codemagic and switch the build to
   `flutter build ipa --release` (notes in `codemagic.yaml`). A $99/year Apple Developer membership is
   required for any distribution beyond personal sideloading, and App Store rules may require Apple
   In-App Purchase for courses and VIP sales.

If you want to use the iOS **simulator** or Xcode interactively, rent a Mac in the cloud for an hour
(MacinCloud, MacStadium, Scaleway Apple silicon) and run `flutter run -d ios` there.

### Original Java Android client

Open `android/` in Android Studio and press Run, or `cd android && gradle assembleDebug`
(APK at `android/app/build/outputs/apk/debug/app-debug.apk`).

## Accounts

There are no demo accounts. Traders register from the sign-in screen against the hosted production
API. On an empty database the server creates the administrator from the `ADMIN_EMAIL` and
`ADMIN_PASSWORD` environment variables (set in the Render dashboard, never committed); if they are
unset it starts with no admin and logs a warning. An admin replaces their own login from
**Admin → Account**, which writes to the database and keeps the session signed in.

## Business rules

- **Deposit** — a 10% admin fee is added on top: `$25.00 + $2.50 = $27.50` paid. Minimum `$12.50`,
  maximum `$454.54`.
- **Withdrawal** — a 7% fee is deducted: `$25.00 − $1.75 = $23.25` received. Minimum `$12.50`;
  a proof image is required.
- Every deposit, withdrawal, course purchase and VIP purchase is **reviewed manually** by an admin.
  Approving settles the wallet transaction and applies the membership or course enrolment.
- VIP membership costs `$30.00`; course prices come from the course record.
- VIP rooms are enforced server-side: without the matching membership the room list marks them
  `locked`, their posts are hidden from the feed, and messages return `403`. Admins bypass the lock.
- Fees and limits are stored in `app_settings` and can be changed live from **Admin → Settings**.
- Money is stored as `BigDecimal` in the API; the client only formats it.

## Live updates across Android, iOS and web

Every client talks to the same API and the same database. On top of that, each write is pushed to the
other connected devices over a WebSocket, so a message posted on an iPhone appears on an Android
phone in under a second — no pull-to-refresh, no polling.

- `ws://<api>/ws?token=<JWT>` — the same bearer token the REST API issues. The hub validates it with
  the existing `JwtService`; a missing or expired token is closed with code `1008` before the socket
  is registered.
- The server broadcasts **invalidation events, never content**: `{"type":"event","scope":"room:3",
  "kind":"message"}`. Each client re-reads through the normal REST endpoints, so role checks, VIP
  locks and per-user wallets stay enforced server-side. A locked room cannot leak through the socket.
- Scopes: `rooms`, `room:<id>`, `signals`, `courses`, `settings`, `users` (admin list),
  `requests` (admin queue), plus `wallet:<userId>` and `home:<userId>` for the owner's own numbers.
- Published from: room messages, deposit / withdrawal / purchase creation, every admin decision,
  membership changes, signal create-update-delete, course create-delete-progress, settings changes,
  profile edits and registrations.
- Client (`mobile/lib/realtime.dart`): reconnects with exponential backoff (1–30 s), sends a 20 s
  heartbeat, debounces bursts (a message updates the room list and the chat with one refetch per
  screen), and resyncs every open screen when the socket comes back. The dot in the header shows the
  state: green `LIVE`, grey `SYNC` while reconnecting.
- The socket carries a JWT in the query string and is not encrypted by itself — put the API behind
  TLS and use `wss://` in production.

## API map

All endpoints are under `/api`. Send `Authorization: Bearer <token>`; `/api/admin/**` requires the
`ADMIN` role.

- Auth: `POST /auth/login`, `POST /auth/register`
- Me / home: `GET /me`, `PUT /me`, `GET /home`, `GET /settings/public`
- Signals: `GET /signals?status=&symbol=`
- Markets: `GET /markets?category=forex|metals|crypto|favourites`, `GET /markets/{symbol}`,
  `POST /markets/{symbol}/favourite`
- Rooms: `GET /rooms`, `GET /rooms/feed?filter=open|closed|all`, `GET|POST /rooms/{id}/messages`
- Wallet: `GET /wallet`, `GET|POST /deposits`, `GET|POST /withdrawals`
- Uploads: `POST /uploads` (multipart `file`)
- Academy: `GET /courses`, `GET /courses/{id}`, `POST /courses/{id}/complete`, `GET|POST /purchases`
- Admin: `GET /admin/users`, `PATCH /admin/users/{id}/membership`, `GET /admin/requests`,
  `POST /admin/deposits/{id}/decide`, `POST /admin/withdrawals/{id}/decide`,
  `POST /admin/purchases/{id}/decide`, `GET|POST /admin/signals`, `PUT|DELETE /admin/signals/{id}`,
  `POST|DELETE /admin/courses`, `GET|PUT /admin/settings`
- Realtime: `GET /ws?token=<JWT>` (WebSocket upgrade, JSON events; `{"type":"ping"}` keeps it alive)

## Responsive, clean UI

- `mobile/lib/responsive.dart` defines the breakpoints (600 / 900 / 1280) and `PageBody`, which
  centres page content and caps its measure (720–1080 px depending on the screen). A phone renders
  exactly as before; a tablet, foldable or desktop web window keeps a readable column instead of
  stretching cards and long lines across the screen.
- Signals switch to a two-up card grid from 900 px. Chat, forms and lessons stay in a comfortable
  single column. The admin dashboard takes the widest cap because of its dense request rows.
- Navigation is native per platform (`NavigationBar` on Android, `CupertinoTabBar` on iOS) and every
  other screen is shared. The Community FAB animates in and out, list states cross-fade, and the header
  shows whether live updates are connected.
- Mouse and trackpad dragging work on web/desktop builds (`AtgScrollBehavior`) with a subtle
  scrollbar, and all lists stay lazy.
- The original Java client is kept as a reference and uses `dimens.xml` with `values-sw600dp`.

## Verification status

- `flutter analyze` reports **no issues** and `flutter test` passes (format helpers: money, price,
  pct, initials, membership labels).
- Both APKs build: `mobile/build/app/outputs/flutter-apk/app-debug.apk` and `app-release.apk`
  (package `com.atg.atg_mobile`, label `ATG`, `INTERNET`, minSdk 24 / targetSdk 36). The release APK is
  currently signed with the debug key — add a release keystore (`android/key.properties`) before
  uploading to Play.
- The whole trader and admin flow was walked at runtime in a browser against the live API (the same
  Dart code, built as `flutter build web --release` and served locally, used as a harness because this
  machine has no emulator or phone): sign-in for both roles, home, signals and detail, markets,
  favourites and pair detail, room chat posting, rooms feed, wallet, deposit, the 4-step withdrawal,
  VIP purchase, and every admin tab (requests, users, signals, courses, settings).
- Live updates verified against the running API, twice over:
  - **Socket level** — a Node script opened two authenticated sockets (learner + admin) on
    `/ws?token=…`. A bad token was closed with `1008`, a room message delivered `rooms` and `room:1`
    to both, a deposit delivered `requests`, `wallet:2` and `home:2` to the creator and `requests` to
    the admin, and approving it replayed the same scopes plus `home:2`. A membership change delivered
    `users`, `home:2` and `rooms`.
  - **UI level** — two independent browser origins (`localhost:5050` as the learner,
    `127.0.0.1:5050` as the admin, separate `localStorage`, one API and one database). With the admin
    parked on **Admin → Requests**, a deposit created for the learner appeared as a pending card with
    Approve / Reject and no refresh. With the learner parked on **Wallet**, the admin's approval flipped
    that row from `PENDING` to `COMPLETED` on screen. With the learner inside the ATG Trading Floor chat,
    a message posted by the desk appeared as a bubble. Every screen header shows the socket state
    (`LIVE` / `SYNC`), and the fake "99+" room badge was replaced by a dot driven by real room events.
- The chat's 6-second polling loop was deleted; messages now arrive by push, and REST remains the only
  authorization path (clients refetch what they are allowed to see instead of trusting payloads).
- Admin settings were silently not persisted with `spring.jpa.open-in-view=false`; the mutation now
  runs in a `@Transactional` service method and is saved explicitly.
- The home screen ends with a Weltrade registration card: numbered steps, a **Create a Weltrade account**
  button that opens `https://track.gowt.me/visit/?bta=46667&brand=weltrade` in the system browser
  (`url_launcher`, `LaunchMode.externalApplication`; the link answers `302` to the Weltrade
  registration page with the affiliate code), and the notice *Accurate Trading Group. Trading carries
  risk.* The former "Are you a Wella?" prompt was folded into this card, and the **Rooms** tab is now
  **Community** across the bottom bar, home quick actions, feed header and signal detail.
- Branding comes from the supplied gold-bull logo (`mobile/assets/brand/atg-bull-logo.png`), processed by
  `mobile/tool/build_brand_assets.py` (Pillow). It writes the five Android launcher mipmaps plus adaptive
  `ic_launcher_foreground` layers, the 15 iOS `AppIcon` sizes, per-density `splash_logo` bitmaps and the
  transparent `assets/images/atg_mark.png` used by `AtgLogo`. Launch screens are brand black: Android 12+
  uses the adaptive foreground, older Android and iOS centre the full lockup. The Flutter splash
  (`lib/screens/splash_screen.dart`) rolls the bull in for one full turn, lifts the two-tone wordmark and
  green tagline in, then fades to the app after ~2.2 s; the in-app logo uses the same mark on cream.
- End-to-end money path verified with real records: deposit `DEP-1` ($25 + 12% = $28) approved,
  withdrawal `W-1` ($20 − 7% = $18.60 to EcoCash) approved with an uploaded proof, VIP purchase `P-1`
  ($30, proof uploaded) approved — after which the account switched to **Weltrade VIP**, the
  Weltrade VIP room reported `locked: false` and Deriv VIP stayed `locked: true`. Admin also updated
  a user's membership, created and deleted a test signal, and saved settings.
- Proof uploads were broken on web/any non-`dart:io` path because the client used
  `MultipartFile.fromPath`; it now reads bytes (`MultipartFile.fromBytes`) so the same code works on
  Android, iOS and web. Both APKs above include that fix.
- The iOS project is configured but **not compiled here** (no macOS/Xcode). It now has the full app
  icon set and launch image in `Assets.xcassets`, built from the supplied gold-bull logo by
  `mobile/tool/build_brand_assets.py` (the same script writes the Android launcher icons, adaptive
  foregrounds and splash bitmaps),
  a `PrivacyInfo.xcprivacy` privacy manifest in the Runner resources, Swift Package Manager plugin
  integration (no Podfile), bundle id `com.atg.atg_mobile`, iOS 15 deployment target, camera /
  photo-library / local-network usage strings and an ATS exception for the dev HTTP server. On a Mac:
  `flutter run -d ios` for the simulator, `flutter build ipa --release` for a device build.
- The original Java client builds with `gradle assembleDebug`.
- VIP room gating is also enforced server-side: locked rooms are flagged in `GET /rooms`, their posts
  are filtered from the feed, and messages return `403`.
- The production database starts empty: no seeded signals, courses, rooms or demo traders. Content
  appears only when an admin publishes it.
