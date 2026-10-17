# Sivanantha Android ERP Viewer v1.3 — Fixed record display and PDF

**What changed:** Visible v1.3 version banner; PDF button at the top and bottom of every record; `items_json` hidden from normal fields and parsed into individual cards for Yarn Issue, Warp Issue and Grey Inward; readable detail labels. Supabase read-only sync and cache logic retained.

## Build
1. Replace the **contents at the repository root**, not in a nested folder, with this ZIP's contents.
2. Confirm `app/build.gradle` says `versionName '1.3.0'` and `versionCode 4`.
3. In GitHub Actions run `Build Android APK` and download `app-debug.apk` artifact.
4. Install the APK. Open a Yarn Issue DC and check for the **v1.3 • PDF & Items Fixed** banner and **GENERATE PDF** button above the record fields.

Debug builds from different GitHub signing environments may require uninstalling the previous APK. If uninstalling, save your connection details first; cached data will be removed.

This source is not device-tested. Generated PDF layout is modeled on Windows ERP, not guaranteed pixel-identical. The Android viewer must use a Supabase account with read-only RLS.
