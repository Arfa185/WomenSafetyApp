# Women Safety App

Android app (Java + XML + Firebase) matching the resume project:

- Real-time location tracking among users in **private groups joined via unique codes**
- **One-click SOS**: sends location by SMS to pre-defined contacts and calls the first one
- **Firebase**: Authentication (email/password), Realtime Database for live sync

## Project structure

```
app/src/main/java/com/example/womensafety/
  ui/      LoginActivity, RegisterActivity, HomeActivity,
           CreateGroupActivity, JoinGroupActivity, GroupActivity, SosContactsActivity
  model/   MemberLocation, SosContact
  util/    CodeGenerator (join-code generation)
```

## One-time setup (required before this builds/runs)

1. **Create a Firebase project** at https://console.firebase.google.com
2. **Add an Android app** to it with package name `com.example.womensafety`
3. Download the real **`google-services.json`** and replace the placeholder at
   `app/google-services.json` (the one in this project is a dummy — the app will not
   connect to Firebase until you swap it in)
4. In the Firebase console, enable:
   - **Authentication → Sign-in method → Email/Password**
   - **Realtime Database** (start in test mode for development, then apply the rules below)
5. Open the project in Android Studio, let Gradle sync, then Run.

## Suggested Realtime Database rules

Test-mode defaults leave data world-readable/writable, which is fine for development but
not for anything real. A reasonable starting point:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "groups": {
      "$code": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

This restricts a user's own contact list to themselves, while allowing any signed-in
user to read/join a group by its code (needed since a new member won't yet be listed
under that group). Tighten further if you productionize this (e.g. only allow writing
your own `members/{uid}` node).

## How the pieces fit together

- **Join codes**: `CreateGroupActivity` generates a random 6-character code, checks it's
  not already taken in `/groups/{code}`, and writes the creator as the first member.
  `JoinGroupActivity` looks up `/groups/{code}` directly and adds the joining user.
- **Live location**: `GroupActivity` uses `FusedLocationProviderClient` to get location
  updates every 10s and writes them to `/groups/{code}/members/{uid}`. A `ValueEventListener`
  on that same path updates every member's screen in real time as others move.
- **SOS**: reads `/users/{uid}/contacts`, sends each one an SMS with a Google Maps link to
  the last known location, and places a phone call to the first contact.

## Running via GitHub Codespaces (no local disk space needed)

If Android Studio's disk footprint is a problem, you can build (and get a real APK)
entirely in the cloud:

1. **Push this project to a GitHub repo** (create an empty repo on GitHub, then from
   this folder: `git init && git add . && git commit -m "init" && git remote add origin <your-repo-url> && git push -u origin main`).
2. On the repo page: **Code → Codespaces → Create codespace on main**. This opens a
   full VS Code environment in your browser, running on GitHub's servers.
3. In the Codespace's terminal, run the included setup script:
   ```
   bash setup-codespace.sh
   ```
   This installs JDK 17 and the Android SDK command-line tools into the cloud
   container (not your machine).
4. Generate the Gradle wrapper and build:
   ```
   gradle wrapper --gradle-version 8.4
   ./gradlew assembleDebug
   ```
   A successful build proves the code compiles correctly — genuinely useful to show
   in an interview even without running the UI.
5. The built APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Download it
   from the Codespaces file explorer (right-click → Download), then install it on
   your **own Android phone** (enable "install from unknown sources" first). This
   is the realistic way to actually see it run — a GUI emulator inside Codespaces is
   possible but slow and fiddly since cloud VMs usually lack hardware acceleration.
6. Don't forget: swap in your real `google-services.json` (see setup steps above)
   before building, or Firebase calls will fail at runtime even though it compiles.

Free GitHub accounts get a monthly Codespaces hour allowance, which is plenty for
this kind of occasional build/verify use.

## Known simplifications (minimal version)

- Member locations are shown as a plain list (lat/lng + timestamp), not a map view —
  wiring in the Google Maps SDK is a natural next step but needs its own API key.
- SOS assumes SMS/CALL permissions are granted; it prompts once but doesn't handle
  "don't ask again" flows.
- No group leave/delete flow yet.
