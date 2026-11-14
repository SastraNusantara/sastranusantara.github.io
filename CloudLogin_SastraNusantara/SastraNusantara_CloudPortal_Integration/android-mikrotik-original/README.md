# MAMANG KONEKSI MIKROTIK MANAGER

Starter project: responsive PWA website + Android WebView wrapper.

## Website
Open `web/index.html` locally or publish the contents of `web/` to GitHub Pages/hosting. HTTPS is required for full PWA install/offline service-worker behavior.

## Android
Open the `android/` folder in Android Studio. Build > Build APK. The Android wrapper loads the local PWA assets bundled in `app/src/main/assets/`.

## Current scope
- 22 catalog entries in six main categories.
- RouterOS v6/v7 selector.
- Parameter forms and generated script output for core starter templates (basic setup, hotspot vouchers, PPPoE, failover, PCQ, port forwarding, NTP, backup, Netwatch).
- Copy, download `.rsc`, local save, search, dark/light theme, responsive navigation.
- Remaining catalog entries are clearly marked as starter/checklist templates, not complete production scripts. Expand and validate each against the target RouterOS release and topology before use.

## Safety
No router credentials are collected and no direct router connection is implemented. Never run generated scripts on a production router without backup, review, Safe Mode, and lab testing. This project is not affiliated with MikroTik SIA.
