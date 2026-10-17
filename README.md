# SIVANANTHA Android ERP Viewer v1.4.1

Based on the existing v1.4 viewer.

Changes:
- Custom navy/gold S app launcher icon.
- Supabase refresh token encrypted with Android Keystore (AES/GCM); viewer password is NOT saved.
- On app startup, load offline records immediately and refresh automatically if a saved session exists.
- Manual Refresh also renews the session.
- Sign out keeps cached records.
- If the session expires or is revoked, sign in again via Settings.
- Internet failure leaves the offline cache untouched.

Build: Upload ALL project files to GitHub repository root, including app and gradle.properties; run Build Android APK.
First launch after upgrade: enter the viewer password ONCE in Settings, then reopen the app to test auto sync.

Security: This app stores a rotating refresh token encrypted by Android Keystore, not the password. A phone with a compromised OS may still expose locally displayed business data.

Not compiled or tested on a physical Android device in this environment.
