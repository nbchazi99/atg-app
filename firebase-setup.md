# Firebase setup — bring your own project

Every Firebase project has its own service account key and client config files.
This repo hardcodes **nothing** about a project except defaults — follow this
guide, paste your files, and the whole stack runs against your account.

## What is project-specific

| File / setting | Used by | Required for |
|---|---|---|
| `backend/keys/firebase-adminsdk.json` | Backend (Firestore, Storage, push) | **Everything** — data lives here |
| `FIREBASE_STORAGE_BUCKET` (env/.env) | Backend uploads | Optional — derived from the key automatically |
| `mobile/android/app/google-services.json` | Android app | Push notifications on Android |
| `mobile/ios/Runner/GoogleService-Info.plist` | iOS app | Push notifications on iOS |
| `.firebaserc` | `firebase deploy` (hosting) | Deploying the web build |
| `DISCHUB_*` in `backend/.env` | Payment gateway | Payments (independent of Firebase) |

The Flutter app itself never sees the service account key — it only talks to
the backend over REST/WebSocket.

## Backend: 30-second setup

1. Firebase console → your project → **Project settings → Service accounts →
   Generate new private key**.
2. Save the downloaded JSON as:

   ```
   backend/keys/firebase-adminsdk.json
   ```

   (the directory and filename are already configured; the file is gitignored)
3. Start the API:

   ```powershell
   cd backend
   .\gradlew.bat bootRun --console=plain
   ```

On startup you should see:

```
Firebase project <your-project-id> — storage bucket derived as <your-project-id>.firebasestorage.app
Firebase initialized from service account keys/firebase-adminsdk.json
```

That's it — Firestore, Storage and FCM all target the key's project, and the
storage bucket is derived from the key's `project_id`.

### Optional environment (backend/.env, gitignored)

```bash
# Point at a key somewhere else (default: keys/firebase-adminsdk.json)
FIREBASE_SERVICE_ACCOUNT=path/to/other-key.json

# Override the derived bucket only if it lives in another project
FIREBASE_STORAGE_BUCKET=some-other-bucket.firebasestorage.app

# Admin account seeded on first boot against an empty database
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=change-me
```

Real environment variables (e.g. on Render) always win over `.env`.

### First boot against an empty database

The server creates the Community room and the two VIP rooms, and seeds the
administrator from `ADMIN_EMAIL` / `ADMIN_PASSWORD` — **only while the
database has no users**; later changes to those variables are ignored. No
signals or courses — an admin publishes those from **Admin → …**. To add or
recover an admin later, register a normal account and promote it from
**Admin → Users → grant full admin** (the user must sign in again afterwards).

## Creating a fresh Firebase project (checklist)

1. <https://console.firebase.google.com> → **Add project** → disable Analytics
2. **Build → Firestore Database → Create database** → production mode → pick a region
3. **Build → Storage → Get started** → production mode (needed for course/chat uploads)
4. **Project settings → Service accounts → Generate new private key**
5. Paste the key as `backend/keys/firebase-adminsdk.json` and start the backend

## Android / iOS push config (per developer)

For **push notifications on a device build**, replace the bundled config with
your own project's files (Project settings → Your apps):

- Android → download `google-services.json` → `mobile/android/app/google-services.json`
- iOS → download `GoogleService-Info.plist` → `mobile/ios/Runner/GoogleService-Info.plist`

Everything except push keeps working even with the old files, because the app
talks to your backend, not directly to Firebase. On web, Firebase init is
optional and fails soft (push is not configured for web).

## Deploying the web app

```powershell
firebase use <your-project-id>     # rewrites .firebaserc
Push-Location mobile
flutter build web --release --web-content-hash
Pop-Location
firebase deploy --only hosting
```

## Troubleshooting

| Symptom | Cause & fix |
|---|---|
| `keys\firebase-adminsdk.json (The system cannot find the path specified)` | Key missing — paste it at `backend/keys/firebase-adminsdk.json` |
| Startup warning `bucket … belongs to a different project than the service account` | `FIREBASE_STORAGE_BUCKET` is stale — clear it to derive automatically |
| `Bucket … does not exist` on upload | Storage not enabled on the project (checklist step 3), or wrong bucket |
| Push never arrives | Device build still has the old `google-services.json` / `GoogleService-Info.plist` |
| `No service account at … Application Default Credentials` | Production mode: ADC supplies the identity (Render/Cloud Run) |
