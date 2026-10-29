# Phish DB — Android

Native Android client for the Phish setlist database. Ships with a copy of the
desktop database (`data/phish.db`) so search works offline from first launch,
then adds new shows weekly from the Phish.net v5 API.

Logo: `donut.png` from the parent repo.

## Stack

- **Language:** Kotlin 2.1
- **UI:** Jetpack Compose, Material 3
- **DB:** Room 2.6, seeded from a bundled asset
- **Networking:** Retrofit + Moshi + OkHttp
- **Async:** Kotlinx coroutines + Flow
- **Background sync:** WorkManager (weekly)
- **Min SDK:** 26 · **Target SDK:** 35

## Features

- Bundled database: every show, song, venue, and setlist from the desktop app
- Weekly update every Sunday ~12:00 AM (needs a network connection): refreshes
  last year and this year; **Settings → Update now** runs it on demand
- Search tab: free-text query, filter sheet (song, tour, venue, date, year,
  state, city), result-type chips (All / Performances / Shows / Songs)
- Show detail: sets ordered 1–4, Encore, Encore 2, Encore 3; tracktimes;
  transitions; jam-chart marks; footnotes
- Song detail: aggregate stats + chronological performance list
- Stats tab: counts, date range, last-update timestamp
- Launcher icon from `donut.png`

## Build

1. **API key.** The build reads `PHISH_API_KEY` from the desktop project's
   `.env` (one folder up) and bakes it into `BuildConfig`. Without it the app
   still works, but the weekly update reports an error.
2. **Bundled database.** From the desktop project root, copy the current
   database into the app's assets (the build fails with a reminder if missing):

   ```
   .\phish.ps1 android-db
   ```

   This builds `app/src/main/assets/databases/phish.db` (~48 MB, git-ignored)
   using the Room schema in `app/schemas/`, without the desktop full-text index.
3. **Build.** The repository ships without a Gradle wrapper jar. Open the
   `android/` folder in Android Studio, or from the command line with
   Gradle 8.11+:

   ```
   cd android
   gradle wrapper
   ./gradlew assembleDebug
   ```

## Installing

The bundled database is copied in only when the app has no database yet. To
put a newer bundled copy on a phone that already has the app, uninstall it
first, then install the new APK. Otherwise the weekly update keeps the
existing copy current.
