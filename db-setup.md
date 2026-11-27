# Database & Firebase setup (for devs)

Walks you through creating **your own** Firebase project and running the whole
stack against it. Every developer gets their own project — nothing is shared,
and swapping projects later is just replacing one file.

> Reference: [firebase-setup.md](firebase-setup.md) has the full table of
> project-specific files, optional env vars and a troubleshooting matrix.

## Time: ~10 minutes

### 1. Create the Firebase project

1. Go to <https://console.firebase.google.com> and sign in with your Gmail.
2. **Add project**.
3. Project name: `ATG Dev` (or whatever you like).
4. Project ID: choose a custom one — it must be globally unique, e.g.
   `atg-app-dev-6908` (lowercase letters, digits, hyphens). Jot down the final
   ID.
5. Google Analytics: **disable** (not needed) → **Create project**.

### 2. Enable Firestore (the database)

1. In the new project: **Build → Firestore Database → Create database**.
2. Start in **production mode** — fine, the backend uses the Admin SDK, which
   bypasses security rules.
3. Pick a region close to you → **Enable**.

### 3. Enable Storage (course & chat uploads)

1. **Build → Storage → Get started**.
2. **Start in production mode** → pick the same region → **Enable**.

Skipping this is the usual cause of `Bucket … does not exist` when an admin
uploads a course asset.

### 4. Generate the service account key

1. ⚙️ **Project settings → Service accounts**.
2. **Generate new private key → Generate key**.
3. Save the downloaded JSON — this is a secret, never commit it.

### 5. Install the key

Save it as exactly:

```
backend/keys/firebase-adminsdk.json
```

That's the whole integration — the backend reads the key's `project_id` at
startup and derives the storage bucket automatically
(`<project_id>.firebasestorage.app`). **No bucket environment variable is
needed.**

The path is gitignored; `FIREBASE_SERVICE_ACCOUNT` can point somewhere else if
you prefer.

### 6. Seed your admin account

Edit `backend/.env` (gitignored):

```bash
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-me
```

On first boot against an empty database the server creates the Community room,
both VIP rooms, and this administrator.

**Important seeder rules:**

- The seeder runs **only when the database has no users**. Changing
  `ADMIN_EMAIL` / `ADMIN_PASSWORD` later does nothing — they are read once, on
  the very first boot.
- Forgot the seeded password or need another admin? Register a normal account
  from the app, sign in with the existing admin, then **Admin → Users →**
  select the user → **Grant admin access**. That user must sign out and sign
  in again for the new role to take effect.
- With no `ADMIN_EMAIL` / `ADMIN_PASSWORD` set, the server starts with **no
  admin** and logs a warning — you then promote via the step above (or restart
  on an empty database with the variables set).

### 7. Run the backend

```powershell
cd backend
.\gradlew.bat bootRun --console=plain
```

Confirm the startup log shows **your** project:

```
Firebase project <your-project-id> — storage bucket derived as <your-project-id>.firebasestorage.app
Firebase initialized from service account keys/firebase-adminsdk.json
Started AtgApplication in … seconds
```

Sanity checks:

```powershell
Invoke-WebRequest http://localhost:8080/api/settings/public   # 200
# log in with the admin you seeded:
Invoke-WebRequest http://localhost:8080/api/auth/login -Method Post `
  -ContentType 'application/json' `
  -Body '{"email":"you@yourdomain.com","password":"…"}'       # 200 + token
```

### 8. Run the app

```powershell
cd mobile
flutter pub get
flutter run -d chrome --web-port=5050
```

Sign in with the seeded admin. The client talks only to your local backend —
it never needs the Firebase key itself.

## Swapping projects later

Replace `backend/keys/firebase-adminsdk.json` with the new project's key and
restart. Delete `.env`'s `FIREBASE_STORAGE_BUCKET` line if you had one (it is
optional and only exists to override the derived value). If the old bucket is
still configured for a different project, the backend warns at startup instead
of failing later.

## Push notifications (optional, per device)

Android and iOS device builds carry their own Firebase config files — replace
them with your project's downloads (Project settings → Your apps):

- `mobile/android/app/google-services.json`
- `mobile/ios/Runner/GoogleService-Info.plist`

Everything except push works without touching them.

## Never commit

- `backend/keys/` — service account keys (gitignored)
- `backend/.env` — secrets and local config (gitignored)
- `google-services.json` / `GoogleService-Info.plist` are currently tracked:
  only replace them with a **non-production** project's files.
