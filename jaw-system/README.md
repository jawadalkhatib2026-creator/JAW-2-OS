# Jaw System

An Apple TV-style home screen (launcher) for Android TV. English + Arabic (follows the TV language).

- Big clock and date, rounded app cards that grow when focused
- Jaw Settings > Add apps: pick which apps appear on the home screen. Hold OK on an app to remove it
- Settings tile: the TV's original settings
- TV channel tiles: add a channel (name, optional link, the app that opens it, optional logo picture)
- Wallpaper from your own picture (Jaw Settings > Wallpaper)
- Free up memory button (Jaw Settings) closes background apps
- Connection check, speed test, Wi-Fi settings and app-cache shortcuts (Jaw Settings)
- Liquid Glass look, wallpaper from phone via QR (also TV files and ready-made wallpapers), Jaw Share (phone to TV via QR), Jaw AppStore, Jaw Accounts + admin panel, account type (Jaw / Google / Both)
- Jaw Gallery (photos from phone via QR), Jaw Files (USB/SD browser), Jaw Music (USB/SD player), Samsung-style Jaw AppStore
- Jaw Gallery (photos from phone via QR), Jaw Files (USB/SD browser), Jaw Music (USB/SD player), Samsung-style Jaw AppStore
- Samsung-style home bar, animated logo intro, first-run setup, Jaw Settings (date/time, home launcher, check for updates)
- Jaw Settings tile: theme, 12/24-hour clock, reset

## Build the APK
- Android Studio: open this folder, Build > Build APK(s)
- Or push this folder to GitHub: the "Build APK" action produces `jaw-system-apk` under Actions > Artifacts

## Install on the TV
Send the APK to the TV (USB stick, or `adb install app-release.apk`), open it once, then press Home and pick Jaw System as the home app.

## Jaw AppStore servers
The store reads `store/catalog.json` from your GitHub repo (main branch) and installs APKs from the https links in it.
Each entry: `{"cat":0,"name":"My App","pkg":"com.example.app","apk":"https://.../app.apk"}` (cat 0 video, 1 music, 2 tools, 3 games).
Only host APKs you are allowed to distribute. The admin can also add apps on a single TV from the Admin panel.
