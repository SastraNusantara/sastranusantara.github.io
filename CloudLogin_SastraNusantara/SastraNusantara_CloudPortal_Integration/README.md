# Sastra Nusantara Cloud Portal — Integration Foundation

Paket ini menyatukan struktur web Billing Sastra Nusantara Ku dan Mamang Koneksi MikroTik Manager di satu origin HTTPS, dengan Supabase Auth sebagai login bersama.

## Isi
- `web/index.html` — portal login + dashboard modul.
- `web/shared/cloud-config.js` — tempat konfigurasi Supabase URL dan publishable/anon key.
- `web/shared/cloud-auth.js` — client auth bersama, pemeriksaan profil, dan proteksi modul.
- `web/billing/` — source HTML Billing asli dengan Cloud Auth guard.
- `web/mikrotik/` — source PWA MikroTik Manager dengan Cloud Auth guard.
- `supabase/migrations/0001_sn_profiles.sql` — tabel profil/role dan RLS.
- `docs/SETUP_CLOUD_LOGIN_ID.md` — panduan konfigurasi.

## Status
Ini adalah fondasi integrasi source, belum deployment dan belum build APK. Cloud Login belum dapat dipakai sebelum Supabase project dibuat, konfigurasi diisi, profil pengguna dibuat, dan situs di-host pada satu origin HTTPS.

## Batasan yang disengaja
Data operasional Billing tetap menggunakan penyimpanan lokal yang sudah ada. Cloud Login hanya memusatkan identitas dan hak akses. Sinkronisasi pelanggan, tagihan, pembayaran, dan data lain lintas perangkat belum diaktifkan.
