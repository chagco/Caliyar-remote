# Caliyar (caliyar-remote) — Android TV / Google TV, protokol v2

Aplikasi remote control untuk Android TV / Google TV yang berjalan lewat WiFi
(bukan IR), menggunakan protokol resmi yang sama dipakai app "Google TV" —
**Android TV Remote Service v2**. Tidak perlu ADB / developer mode di TV,
cukup layanan bawaan "Android TV Remote Service" yang sudah ada di hampir
semua Android TV / Google TV.

## Dapatkan APK jadi tanpa install Android Studio (GitHub Actions)

Proyek ini sudah dilengkapi `.github/workflows/build-apk.yml` yang otomatis
meng-compile APK setiap kali proyek di-upload ke GitHub.

1. Buat akun gratis di **github.com** (kalau belum punya).
2. Buat repository baru (public atau private, bebas).
3. Upload **semua isi folder `TVRemote`** ini ke repository tersebut
   (bisa lewat drag & drop di halaman repo, atau "Add file > Upload files").
4. Setelah upload, buka tab **Actions** di repo tersebut — build akan
   berjalan otomatis (sekitar 3-5 menit).
5. Setelah selesai (tanda centang hijau), klik build tersebut, lalu di
   bagian bawah ada **Artifacts > caliyar-remote-apk** — unduh, ekstrak
   zip-nya, di dalamnya ada `app-debug.apk`.
6. Pindahkan `app-debug.apk` ke HP, aktifkan "Izinkan instal dari sumber
   tidak dikenal", lalu instal seperti biasa.

## Cara pakai

1. Buka proyek ini di **Android Studio** (Giraffe/Koala ke atas), biarkan
   Gradle sync (perlu koneksi internet untuk mengunduh dependency pertama kali).
2. Pastikan HP dan Android TV terhubung ke **WiFi yang sama**.
3. Cari alamat IP TV di TV: **Setelan > Jaringan & Internet > [nama WiFi]**.
4. Jalankan app di HP, masukkan IP tersebut, tekan **Pasangkan**.
5. TV akan menampilkan kode 6 digit di layar — masukkan kode itu di HP.
6. Setelah berhasil, langsung diarahkan ke layar remote (D-pad, volume, home,
   back, power, play/pause, dll).

Sertifikat pairing disimpan secara lokal di HP (`filesDir`), jadi setelah
pairing pertama, tombol **Hubungkan** langsung connect tanpa perlu pairing
ulang — sama seperti perilaku app resmi.

## Cara kerja teknis

- **Port 6467 (TLS)** — pairing: pertukaran sertifikat client (dibuat sendiri,
  self-signed) dan server, lalu client menghitung SHA-256 dari
  modulus+eksponen kedua sertifikat + sebagian kode PIN, dan mengirim hash itu
  sebagai bukti bahwa user benar-benar melihat kode di layar TV.
- **Port 6466 (TLS)** — remote control: setelah pairing, koneksi baru dengan
  sertifikat yang sama langsung dipercaya TV; client mengirim pesan protobuf
  `RemoteKeyInject` untuk setiap tombol yang ditekan.
- Semua pesan protobuf memakai framing **varint-length-prefixed** (sama
  seperti convention protobuf).
- File `.proto` di `app/src/main/proto/` mengikuti skema yang sudah
  didokumentasikan publik dari reverse-engineering protokol ini (dipakai
  banyak proyek open-source seperti `androidtvremote2` dan Home Assistant).

## Catatan

- Field `code1` pada `RemoteConfigure` (di `TvRemoteClient.kt`) memakai nilai
  konstan `622` yang umum dipakai implementasi open-source lain; jika ada TV
  tertentu yang menolaknya, coba ubah ke `1`.
- Beberapa TV lama (Android 9 ke bawah) kadang punya masalah kompatibilitas
  TLS pada implementasi Android TV Remote Service versi lama — kalau gagal
  connect terus, pastikan TV sudah update software-nya.
- Build APK release perlu signing key sendiri (belum disertakan di sini).
