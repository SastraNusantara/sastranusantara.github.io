# Panduan menyiapkan Cloud Login

## 1. Buat project Supabase
1. Buka https://supabase.com/dashboard lalu buat project baru.
2. Simpan Project URL dan publishable key (atau anon key legacy).
3. Jangan pernah menaruh `service_role` key atau secret key di HTML, JavaScript, GitHub, maupun APK.

## 2. Buat skema profil
1. Di Supabase buka SQL Editor.
2. Jalankan isi `supabase/migrations/0001_sn_profiles.sql`.
3. Pastikan RLS aktif pada `public.sn_profiles`.

RLS hanya mengizinkan pengguna membaca profil miliknya sendiri. Aplikasi browser tidak diberi izin untuk mengubah role atau hak akses. Ini mengikuti pola keamanan RLS Supabase.

## 3. Buat akun pengguna
Untuk tahap awal, buat pengguna melalui Supabase Dashboard:
1. Authentication → Users → Add user.
2. Masukkan email dan password pengguna.
3. Salin UUID pengguna.
4. Jalankan SQL berikut di SQL Editor (ganti nilai contoh):

```sql
insert into public.sn_profiles
(user_id, display_name, role, billing_access, mikrotik_access, is_active)
values
('UUID_DARI_AUTH_USERS', 'Administrator Utama', 'super_admin', true, true, true);
```

Contoh role yang didukung: `super_admin`, `admin`, `noc`, `technician`, `cashier`, `staff`.
Hak akses Billing dan MikroTik diatur lewat kolom `billing_access` dan `mikrotik_access`.

## 4. Isi konfigurasi frontend
Edit `web/shared/cloud-config.js`:
- `supabaseUrl`: Project URL Supabase.
- `supabaseAnonKey`: publishable/anon key.
- `portalBaseUrl`: `/` bila portal dipasang di root domain/repository path yang sama. Bila GitHub Pages memakai path project, gunakan path tersebut, misalnya `/nama-repo/`.

Contoh:
```js
window.SN_CLOUD_CONFIG = {
  supabaseUrl: "https://abcdefgh.supabase.co",
  supabaseAnonKey: "sb_publishable_...",
  portalBaseUrl: "/nama-repo/"
};
```

## 5. Host semua modul pada satu origin HTTPS
Struktur hosting harus seperti ini:
- `/` → `web/index.html`
- `/billing/` → `web/billing/index.html`
- `/mikrotik/` → `web/mikrotik/index.html`
- `/shared/` → shared config/auth.

Contoh GitHub Pages project site: `https://USERNAME.github.io/NAMA-REPO/`.
Jangan host Billing dan MikroTik pada domain berbeda jika ingin satu sesi browser/localStorage bersama.

## 6. Set Supabase Auth URLs
Di Supabase → Authentication → URL Configuration:
- Site URL: alamat portal HTTPS yang sebenarnya.
- Redirect URLs: tambahkan alamat portal dan modul sesuai deployment.
Login password biasa tidak membutuhkan redirect email confirmation, tetapi konfigurasi URL tetap penting untuk alur reset password dan tautan autentikasi lain.

## 7. Uji sebelum dipakai
- Login dengan akun yang punya kedua hak akses.
- Uji akun Billing saja dan MikroTik saja.
- Uji logout dari Billing; sesi portal dan MikroTik harus ikut berakhir.
- Coba akses URL modul secara langsung tanpa login; harus diarahkan kembali ke portal.
- Uji di Chrome desktop, Android Chrome, dan Android WebView.
- Uji semua fitur Billing lama (pelanggan, pembayaran, cetak, backup/restore, impor/ekspor) karena login cloud tidak otomatis menguji data lokal.

## 8. Catatan Android
Agar kedua modul memakai sesi yang sama di Android, pendekatan yang disarankan adalah satu aplikasi Android/WebView utama yang membuka portal HTTPS dan berpindah antar-path dalam WebView yang sama.
Dua APK terpisah biasanya memiliki penyimpanan WebView terisolasi; login pada satu APK tidak otomatis menjadi sesi di APK lain.
Setelah URL hosting tersedia, `MainActivity.kt` pada proyek Android utama perlu diarahkan ke URL portal HTTPS. Jangan ubah URL sebelum portal berhasil di-deploy.

## 9. Yang belum termasuk
- Migrasi data Billing ke database cloud.
- Form manajemen akun admin dari dalam aplikasi.
- Edge Function untuk membuat/menonaktifkan akun pengguna secara aman.
- MFA, audit log, backup cloud, dan pengujian keamanan formal.
- Build APK release dan publikasi Play Store.
