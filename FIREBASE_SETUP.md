# Firebase setup (upload notes & tasks)

The code is wired but **inactive until you add a Firebase project**. The
`com.google.gms.google-services` plugin fails the build when `google-services.json`
is missing, so it stays commented out in `app/build.gradle`.

## One-time activation

1. **Firebase console** → https://console.firebase.google.com → *Add project*.
2. Inside the project → *Add app* → **Android**, package name:
   `com.shumidub.todoapprealm.alpha8`
   (add the release applicationId too, if/when you have one).
3. Download **`google-services.json`** → put it in **`app/`**.
4. In **Authentication → Sign-in method**, enable **Email/Password**.
5. Create a **Realtime Database** (Build → Realtime Database → Create). This
   provisions the DB URL into `google-services.json`.
6. In `app/build.gradle`, **uncomment**:
   ```gradle
   apply plugin: 'com.google.gms.google-services'
   ```
7. Build & run.

## Recommended security rules (Realtime Database)

Each user can only read/write their own node:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read":  "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid"
      }
    }
  }
}
```

## How it works

- **Menu → Backup / Sync → “Upload to Firebase”.**
- If not signed in, an email/password dialog appears (sign in or register).
- On success the whole local DB (folders, tasks, notes — the same payload as the
  JSON backup) is written to `users/{uid}/backup`, with `users/{uid}/updatedAt`.
- Upload only. Restore-from-Firebase is not implemented (local JSON restore still works).

## Build note

This project must build with **JDK 17–21** (Gradle 8.10 doesn't support JDK 25+).
If you hit `Unsupported class file major version`, point Gradle at a JDK ≤21, e.g.:
```
JAVA_HOME=/path/to/jdk-21 ./gradlew assembleDebug
```
