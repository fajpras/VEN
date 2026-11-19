# Status Proyek VEN — Daftar Modul, Verifikasi, dan Kendala

> **Tanggal verifikasi:** 19 November 2026
>
> **Branch aktif:** `main`
>
> **Commit terakhir:** hasil merge `267ca8d` (perapian koding bersih lokal) dengan `cd06eb2` (fitur hapus chat dari remote) — HEAD saat verifikasi
>
> **Perbaikan koding bersih (19 November 2026):** Seluruh aplikasi dirapikan tanpa mengubah perilaku fitur. Backend: penghapusan pencatatan OTP plaintext, 9 import tak terpakai, tag penutup PHP, variabel/komentar mati, dan penyatuan FQCN di `PameranController`. Android: penghapusan ~50 import tak terpakai, pembatasan logging `UnityBridge` hanya pada build debug, penghapusan seluruh operator `!!` (API, cache, navigasi), ekstraksi `sesiBerakhir` ke `util/SessionErrorExt.kt`, pemindahan alur feed pengguna ke `UserPostsFeedRoute.kt`, serta pemisahan 6 `Route` dari file `Screen.kt` menjadi file tersendiri. Verifikasi: **93/93 test backend** (454 assertion), **73/73 test Android** (termasuk 3 test baru `AiChatViewModelTest` dari remote), build/test Android sukses, `git diff --check` bersih. Perapian ini telah di-commit lokal sebagai `267ca8d` sebelum pull.
>
> **Perubahan terbaru:** AI Chat Android kini mendukung hapus percakapan per item ala Gemini (tombol ⋮ → "Hapus obrolan" → dialog konfirmasi), dengan pembersihan key prefs saat riwayat terakhir dihapus; diverifikasi 73/73 unit test dan smoke test emulator. Sebelumnya: Halaman profil ditata ulang mengikuti prinsip Konsistensi & Standar (Jakob's Law): kiri atas dikosongkan, username ditengah, tombol `+` dan menu (gear) dipindah ke kanan atas berdampingan; tab Posts/Exhibits dinaikkan ke bawah statistik; statistik disebar dengan `SpaceEvenly`; pill keahlian disatukan warnanya (`colors.primary`, tanpa bintang) dan dibungkus `FlowRow` agar tidak terpotong (bottom sheet disamakan); Email/Phone diberi `HorizontalDivider` dan affordance klik (`mailto:`/`tel:`) dengan ikon serta warna `primaryText`, ditambah tombol aksi Edit Profile & Share Profile. Form edit profil (`VexTextField` Boxed) diperbaiki: label lebih jelas, padding horizontal 24→16dp, field multiline tumbuh (`heightIn(min=100.dp)`) dengan align atas, jarak antar field 16→20dp. Resolusi 2 blok konflik merge di dokumen ini (ringkasan verifikasi Android & kendala prioritas tinggi) dengan mempertahankan hasil terverifikasi: build Android sukses, APK ~124,9 MB, 70/70 unit test. Kartu feed (`PostCard`) ditata ulang bergaya Instagram: avatar + username tebal dengan judul sebagai sub-judul, tombol aksi (Request Meeting, View 3D) dipindah ke bawah media tanpa latar lingkaran, caption jadi paragraf username + deskripsi, "see more" abu dan timestamp polos. Judul dibersihkan dari sisa markdown (`>`, `_`, `**`). Pill kategori dihapus dari kartu feed (`PostCard`), beserta parameter `category` dan import yang tak terpakai. `VexSkillChip` (dipakai ulang `VexTagChip` di profil & banner explore) dibuat lebih clean: latar tipis transparan dari warna kategori, teks & ikon memakai aksen warna kategori yang disesuaikan kontrasnya per tema, tanpa gradient/glow; tinggi 24→26dp. Penguatan keamanan aplikasi Android: token & data sesi kini terenkripsi AES-256-GCM via AndroidKeyStore (`SecurePrefs` + migrasi transparan `SessionManager`), logging HTTP level BODY hanya pada build debug, cleartext dimatikan untuk release (`network_security_config.xml` HTTPS-only; emulator tetap diizinkan pada src/debug), token dikecualikan dari cloud backup, kedua activity Unity tidak lagi `exported`, URL cleartext hardcode `10.0.2.2` dihapus dari kode, `proguard-rules.pro` disiapkan (minify masih nonaktif), cooldown 60 detik untuk kirim ulang OTP registrasi. Koreksi: fallback dummy data dihapus total dari jalur production (Schedule, History, Explore), `ScreenRouteTest` diperbaiki, tanggal awal Request Meeting kini dinamis (tidak lagi Sept 2026), pesan galat Request Meeting dilokalisasi, dead code dibersihkan (`com/ven/app/unity/UnityLauncher`, test template `com.example.venmobile`, warna template), `RegisterRoute` & `ProfileViewModel` dipindah ke file sendiri. Polish UI: tombol Outline adaptif tema, window background terang/gelap sesuai mode (tidak ada kilatan gelap di mode terang), Request Meeting memakai `VexTextArea` + indikator loading banner, warna hardcode dipindah ke token tema (`accentWarning`). **70 dari 70 unit test Android lulus.**
>
> **Lingkungan:** Windows + Laragon, PHP 8.3.30, Laravel 13.34.0, MySQL, Android Gradle 9.6

Dokumen ini merangkum hasil audit langsung terhadap repositori: modul apa yang sudah selesai, apakah benar-benar bisa dijalankan, bagian mana yang masih berupa mock, serta kendala yang ditemukan beserta rekomendasi perbaikannya.

> ⚠️ **Dokumen hidup:** setiap penambahan/perubahan fitur wajib memperbarui file ini pada commit yang sama. Lihat aturan di [`.agents/rules/status-proyek.md`](../.agents/rules/status-proyek.md).

---

## 1. Ringkasan Hasil Verifikasi

| Pemeriksaan | Perintah | Hasil |
|---|---|---|
| Konfigurasi backend | `php artisan about` | ✅ Laravel 13.34.0, PHP 8.3.30, DB MySQL, storage link aktif |
| Daftar route backend | `php artisan route:list` | ✅ 99 route terdaftar |
| Test backend | `php artisan test` | ✅ **93 test — 93 lulus** (454 assertion), diverifikasi per grup: 87 test utama + 4 AI Chat + 2 Audit Backup/Restore |
| Fallback galat API | `php artisan test --filter=PenanganKesalahanApiTest` | ✅ **9 test lulus** — 401, 403, 404, 405, 422, 500 tanpa kebocoran detail internal + redirect browser & JSON untuk klien API |
| Halaman galat web | `GET /halaman-tidak-ada` | ✅ Blade 404 minimalis (tanpa kartu/dekorasi), mandiri (tanpa aset Vite), merek "Value Exhibition Network", respons `text/html`, tanpa tautan ke beranda |
| Akar situs | `GET /` | ✅ HTTP **404** — panel admin tidak terekspos, tidak redirect ke login/beranda |
| API dibuka dari browser | `GET /api/pameran/25` + `Accept: text/html` | ✅ `302` → `/404`, halaman 404 kustom ditampilkan; klien API (`application/json`, wildcard, tanpa Accept) tetap menerima JSON |
| Smoke test API live | `artisan serve` + login `fajri@ven.test` | ✅ Login, `/api/pameran`, `/api/karya/rekomendasi`, `/api/profil`, `/api/pencarian`, `/api/ai/tanya`, `/api/undangan` merespons normal |
| Admin Web Panel | `GET /admin/login` | ✅ HTTP 200 |
| Build Android | `.\gradlew.bat assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| Artefak APK | `app/build/outputs/apk/debug/app-debug.apk` | ✅ Ada, 125.974.239 byte (~126,0 MB) |
| Test Android | `.\gradlew.bat testDebugUnitTest` | ✅ **73 test — 73 lulus** (3 test baru `AiChatViewModelTest`; 2 test template `com.example.venmobile` dihapus; `ScreenRouteTest` diperbaiki) |
| Audit & perapian koding bersih | `git diff --check` + build/test | ✅ Tidak ada `dd/dump`, `!!`, `TODO/FIXME`, import tak terpakai, atau logging debug di rilis; struktur `Route`/`Screen` dipisah sesuai konvensi |

**Catatan lingkungan:** perintah `php` tidak tersedia di PATH. Gunakan path Laragon:

```powershell
C:\laragon\bin\php\php-8.3.33-Win32-vs16-x64\php.exe artisan test
```

---

## 2. Modul yang Sudah Selesai dan Berjalan

### 2.1 Backend API (`backend-api/backend`)

| No | Modul | Implementasi / Endpoint | Status |
|:--:|---|---|:--:|
| 1 | Autentikasi & OTP | `auth/daftar/kirim-otp`, `auth/daftar`, `auth/masuk`, Google OAuth, `auth/lupa-kata-sandi`, `auth/reset-kata-sandi`, `auth/keluar`, throttle | ✅ |
| 2 | Keamanan data | AES-256-GCM + Blind Index (`google_id`), Bcrypt, view `v_pengguna_publik`, log keamanan | ✅ |
| 3 | Profil & minat | `profil` (GET/PUT), foto, ganti email via OTP, ganti kata sandi, status akun, kategori & minat | ✅ |
| 4 | Pameran | CRUD `pameran`, template `model-3d`, rekomendasi, upload banner, ruangan, `room-dto`, `exhibitions` (dari Unity) | ✅ |
| 5 | Karya | CRUD `karya`, upload media, penyajian file bertanda tangan, rekomendasi, update posisi objek | ✅ |
| 6 | Diskusi & Undangan | `undangan` (index/store/show/status/tanggapi), voice token, otomatisasi ruangan privat pameran, pencegahan self-invitation & duplicate active, eager load banner & pengguna | ✅ |
| 7 | Pencarian terpadu | `pencarian` (akun, karya, pameran) | ✅ |
| 8 | AI Chat | `ai/tanya` → Google Gemini (`gemini-2.5-flash-lite`), grounding konteks pameran & karya, penolakan out-of-scope | ✅ |
| 9 | Admin API | pengguna, sanksi (`lockForUpdate`), moderasi karya/pameran, log aktivitas & keamanan | ✅ |
| 10 | Admin Web Panel | Login, akun, posts, exhibitions, activity-log (Blade + Tailwind) | ✅ |
| 11 | 7 Pilar Audit DB | View, hashing, enkripsi, transaksi ACID, locking, backup otomatis, restore verification | ✅ |
| 12 | Command & Scheduler | `ven:setup-db`, `ven:initialize-db`, `ven:backup-db`, `ven:restore-db`, `ven:enkripsi-google-id`, `ven:uji-transaksi-pameran` | ✅ |
| 13 | Penanganan galat API terpusat | `app/Exceptions/PenanganKesalahanApi.php` + `bootstrap/app.php`: semua `/api/*` selalu JSON `{data, message}` untuk 401/403/404/405/413/419/422/429/5xx, tanpa redirect `route('login')` dan tanpa stack trace. Bila endpoint dibuka langsung dari browser (`Sec-Fetch-Mode: navigate`, atau Accept `text/html` + User-Agent Mozilla), browser dialihkan ke `/404` agar tidak melihat JSON mentah | ✅ |
| 14 | Halaman galat web | `resources/views/errors/404.blade.php`: halaman 404 minimalis (logo kecil, angka 404 gradien, satu tombol, tanpa kartu/dekorasi), mendukung mode gelap, mandiri tanpa aset Vite, dan memakai nama aplikasi **Value Exhibition Network** | ✅ |

Dokumentasi teknis: `backend-api/backend/docs/`.

### 2.2 Aplikasi Android (`ven-mobile`)

| No | Modul | Bukti Implementasi | Status |
|:--:|---|---|:--:|
| 1 | Splash, Login, Register, OTP, Forgot/Reset Password | `ui/screens/auth/*`, `ApiLogin`, `ApiRegister` | ✅ |
| 2 | Onboarding minat | `InterestViewModel` → `ApiProfil.kategori()` / `minat()` | ✅ |
| 3 | Home feed & rekomendasi | `HomeViewModel` → `ApiHome` (`karya/rekomendasi`, `pameran/rekomendasi`), pull-to-refresh, double-tap tab | ✅ |
| 4 | Explore & pencarian | `ExploreViewModel` → `ApiPencarian`, filter kategori, grid 3 kolom | ✅ |
| 5 | Profil & User Profile | `ProfileUiState` → `ApiProfil` (profil, jumlah karya/pameran, karya milik) | ✅ |
| 6 | Settings akun | Ganti email, ganti kata sandi, status akun, tema gelap/terang | ✅ |
| 7 | Buat pameran (template & info) | `CreateExhibitViewModel`, upload banner via `ApiPameran` | ✅ |
| 8 | Viewer 3D Unity | `UnityLoadingScreen` + `UnityLauncher` + endpoint `room-dto`, fallback DTO anti black screen (room Template01 berjalan) | ⚠️ fallback aktif; live DB `ven` belum ada kolom `objek.id_pameran` |
| 9 | AI Chat | `AiChatViewModel` → `ApiAiChat` + `AiChatHistoryPreferences`/`AiChatHistoryStore` + drawer riwayat chat persisten + hapus percakapan per item (menu ⋮, dialog konfirmasi), live Gemini | ✅ |
| 10 | Theme engine | `ThemePreferences`, Material 3 | ✅ |
| 11 | Multi-bahasa | `AppLanguage`, `VenApplication`, `locales_config.xml`, `AppCompatDelegate`, 321 keys (English default & Indonesian) | ✅ |
| 12 | Schedule (Jadwal Undangan) | `ScheduleViewModel` → `ApiUndangan` (4 tab: Approved, Waiting, Request, History; aksi Terima/Tolak & Join Room 3D Viewer) | ✅ |
| 13 | Request Meeting (UC-17) | `RequestMeetingViewModel` → `ApiUndangan.createUndangan`, validasi tanggal/jam lampau, input jam/menit/AM-PM, catatan | ✅ |
| 14 | Riwayat Undangan di Settings | `HistoryScreen` tersambung ke `ScheduleViewModel` (menampilkan riwayat undangan riil dari API) | ✅ |
| 15 | Penguatan keamanan mobile | `util/SecurePrefs` (AES-256-GCM AndroidKeyStore) + `SessionManager` terenkripsi; `ApiClient` logging BODY hanya debug & `setBaseUrl` menolak non-HTTPS di release; `network_security_config.xml` (release HTTPS-only), `backup_rules`/`data_extraction_rules` mengecualikan `ven_auth_pref` & `ven_ai_chat_pref`, activity Unity `exported=false`, `proguard-rules.pro` siap (minify off), cooldown OTP registrasi | ✅ |
| 16 | Polish UI & konsistensi design token | Tombol `VexButton.Outline` adaptif tema (perbaikan mode terang), `themes.xml` light/night (window background sesuai mode), `RequestMeetingScreen` memakai `VexTextArea` + loading state banner + spacing/typography token, token `accentWarning` baru, warna hardcode dihapus dari `CarouselExhibitions`/`CreateOptionsBottomSheet`; pill kategori `VexSkillChip` (juga dipakai `VexTagChip`) memakai latar tint transparan dan aksen warna kategori adaptif tema; pill kategori pada kartu feed (`PostCard`) dihapus; kartu feed ditata ulang bergaya Instagram (header, aksi bawah media, caption paragraf); halaman profil ditata ulang (top bar kanan, tab Posts/Exhibits, pill keahlian seragam & wrap, kontak interaktif + divider, tombol Edit/Share Profile); form edit profil diperbaiki padding & label-nya | ✅ |

### 2.3 Artefak Pendukung

- **Postman:** `postman/` berisi koleksi *VEN API - Complete Routes Test*, *VEN Audit Keamanan & Database* (7 folder), *VEN API - Pengguna & Auth Tests*, beserta environment.
- **Dokumentasi:** `backend-api/backend/docs/` (7 pilar + panduan test) dan `ven-mobile/docs/DESIGN.md`.
- **APK debug:** `ven-mobile/app/build/outputs/apk/debug/app-debug.apk`.
- **Unity:** modul `unityLibrary` sudah di-include di `settings.gradle.kts` dan activity Unity terdaftar di `AndroidManifest.xml`.

---

## 3. Bagian yang Masih Mock / Belum Tersambung ke Backend

| Fitur | Lokasi | Kondisi |
|---|---|---|
| Create Post / unggah karya | `.../create/post/CreatePostViewModel.kt:76-78` (`delay(400)`) | Simulasi; belum ada `ApiKarya` di mobile |
| Publish pameran dari editor Compose | `.../create/exhibit/editor/ExhibitEditorViewModel.kt:156-158` (`delay(800)`) | Simulasi; hanya jalur Unity yang menyimpan ke database |
| Privacy Policy / Help | `SettingsPlaceholderScreen.kt` | Masih placeholder |

---

## 4. Kendala dan Rekomendasi Perbaikan

### Prioritas Tinggi

| No | Kendala | Bukti | Dampak | Rekomendasi |
|:--:|---|---|---|---|
| 1 | Database `ven` belum memuat kolom `objek.id_pameran` | Uji live Unity membuka `GET /api/pameran/4/room-dto` menghasilkan **HTTP 500** (`Unknown column 'objek.id_pameran'`). Migrasi `2026_11_11_000001_add_id_pameran_to_objek_table.php` sudah jalan di `ven_testing` tetapi belum di `ven` | Endpoint `room-dto` gagal; Unity beralih ke fallback RoomDto lokal | Jalankan migrasi kolom `id_pameran` pada database aplikasi `ven` setelah persetujuan pengguna |

### Prioritas Sedang

| No | Kendala | Dampak | Rekomendasi |
|:--:|---|---|---|
| 2 | Modul Create Post dan Publish editor masih simulasi | Fitur terlihat jalan tetapi data belum tersimpan ke server | Buat `ApiKarya` di mobile, lalu hubungkan ViewModel ke endpoint yang sudah ada |
| 3 | Source Unity (`.cs`) tidak ada di repo, hanya binary `il2cpp` | Integrasi Unity tidak dapat diaudit dari source; verifikasi butuh perangkat/emulator. Token Bearer masih dikirim via intent extra ke proses `:unity` (`UnityLauncher` → `UnityBridge`) — risiko eksternal sudah ditekan (activity Unity kini `exported="false"`), tetapi token tetap tampil di intent dump | Simpan proyek Unity sebagai submodule/folder terpisah atau dokumentasikan prosedur build; jangka panjang: sampaikan token lewat mekanisme IPC terbatas |
| 4 | Perubahan binary Unity belum di-commit | `libil2cpp.so` (arm64) termodifikasi dan `compile-data.json` masih untracked | Tinjau ulang lalu commit secara terkontrol (hindari commit binary besar tanpa kebutuhan) |
| 5 | R8/obfuskasi (`isMinifyEnabled`) masih nonaktif | APK release mudah dibedah; `proguard-rules.pro` sudah disiapkan (keep rules Gson/Retrofit/UnityBridge) | Aktifkan `isMinifyEnabled = true` lalu smoke test build release di perangkat fisik (Unity + Gson) sebelum rilis |

### Prioritas Rendah / Lingkungan

| No | Kendala | Rekomendasi |
|:--:|---|---|
| 6 | `BASE_URL` mobile default `http://10.0.2.2:8000/api/` | Hanya untuk emulator; ubah `ven-mobile/.env` untuk perangkat fisik. Pastikan URL HTTPS dipakai saat rilis (release build menolak base URL non-HTTPS) |
| 7 | APK berukuran ~126 MB karena Unity binary | Pertimbangkan Android App Bundle atau ABI split |
| 8 | `php` tidak ada di PATH | Gunakan path Laragon (`C:\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe`) atau tambahkan ke environment variable |
| 9 | `GOOGLE_SERVER_CLIENT_ID` di `.env` berformat non-standar (`glowing-icon-510605-m1.apps.googleusercontent.com`) | Client ID Google OAuth umumnya berawalan angka; verifikasi nilai Web Client ID di Google Cloud Console agar login Google tidak gagal di produksi |

### Kendala yang Sudah Diperbaiki

| Tanggal | Kendala | Perbaikan | Bukti |
|---|---|---|---|
| 19 Nov 2026 | Riwayat AI Chat hanya bisa ditambah dan dibuka; tidak ada cara menghapus percakapan sehingga riwayat menumpuk permanen di perangkat | Fitur hapus per item ala Gemini: tombol ⋮ pada tiap item drawer membuka menu "Hapus obrolan" → dialog konfirmasi (`Batal`/`Hapus`). `AiChatHistoryPreferences` mengekspos interface `AiChatHistoryStore` + `deleteConversation` (commit sinkron; key prefs dihapus bila riwayat kosong). `AiChatViewModel.onDeleteConversation` membatalkan job AI berjalan dan mereset state bila obrolan aktif dihapus; respons AI terlambat diabaikan lewat guard `currentChatId` agar obrolan tidak muncul kembali | `gradlew :app:testDebugUnitTest :app:assembleDebug` BUILD SUCCESSFUL; 73/73 test lulus (0 failure, 0 error; 3 test baru `AiChatViewModelTest`); APK 125.974.239 byte; smoke test emulator-5554: hapus 1 dari 2 riwayat → prefs tersisa 1, hapus item terakhir → key user terhapus, riwayat user lain utuh |
| 19 Nov 2026 | Halaman profil menyalahi kebiasaan umum: `+` di kiri atas, ikon menu dua garis ambigu, pill keempat terpotong di tepi kanan, nama/stat menumpuk di kiri dengan whitespace kanan berlebih, Email/Phone polos tanpa divider/affordance, empat pill keahlian berbeda warna + bintang, serta tidak ada tombol aksi utama maupun tab konten | Top bar profil: kiri dikosongkan, username ditengah, `SettingsProfile` (gear) + `Add` di kanan berdampingan; `VexIconTabs` dinaikkan ke bawah statistik; stat `SpaceEvenly`; chip keahlian `FlowRow` wrap, warna `colors.primary`, tanpa bintang (juga di bottom sheet); Email/Phone diberi `HorizontalDivider` + baris klik `mailto:`/`tel:` dengan ikon & `primaryText`; tombol `Button` Edit Profile (ke `Screen.SettingsProfile`) + `OutlinedButton` Share Profile (ACTION_SEND). Form edit profil: label `bodyMedium`, padding horizontal 24→16dp, multiline `heightIn(min=100.dp)` align atas, jarak field 16→20dp | `gradlew :app:testDebugUnitTest :app:assembleDebug --offline` BUILD SUCCESSFUL; 70/70 test lulus (0 failure, 0 error); APK 124.950.674 byte |
| 19 Nov 2026 | Tampilan kartu feed kurang enak dilihat: tombol aksi berupa lingkaran di header, judul menusuk caption, spasi tidak konsisten, dan judul mengandung sisa markdown (`> _UIUX_...`) | Layout ulang ala Instagram: avatar 40dp + username Bold dan judul sebagai sub-judul di header; aksi dipindah ke bawah media sebagai `IconButton` polos; caption hanya username + deskripsi; "see more" abu (`textMuted`), timestamp tanpa bullet, padding 16dp. `sanitizeCaptionTitle` membersihkan `>`, `_`, `**`. Import `BorderStroke`/`Surface`/`CircleShape`/`size` dihapus | `gradlew :app:testDebugUnitTest :app:assembleDebug --offline` BUILD SUCCESSFUL; 70/70 test lulus; APK 124.935.979 byte |
| 19 Nov 2026 | Dua blok konflik merge belum terselesaikan di `docs/STATUS-PROYEK-VEN.md` (ringkasan verifikasi Android & kendala prioritas tinggi), sehingga angka build/test bertentangan dengan isi dokumen lain | Konflik diselesaikan dengan mempertahankan sisi `0a6fc129`: build Android sukses, APK ~124,9 MB, 70/70 test, dan kendala aktif `objek.id_pameran` pada DB `ven`. Sisi `HEAD` (NDK invalid, 65/66 test, 4 kendala usang) dibuang karena sudah tercatat diperbaiki | Tidak ada marker `<<<<<<<`/`=======`/`>>>>>>>` tersisa; angka konsisten dengan header, modul Android 15–16, dan Kesimpulan |
| 19 Nov 2026 | Pill kategori di kartu feed (`PostCard`) dianggap berlebihan; pill `VexSkillChip` berwarna solid/gradient terlalu berat secara visual | Blok pill dihapus dari `PostCard`, termasuk parameter `category` serta import `VexSkillChip`/`CategoryColorMap` dan pemanggilnya di `HomeScreen`/`PostsFeedScreen`. `VexSkillChip` (dipakai ulang `VexTagChip`) kini memakai latar tint transparan (±11–15% alpha), border aksen ±30%, teks & ikon berwarna kategori yang dicerahkan di tema gelap atau digelapkan di tema terang; tinggi 26dp. `CategoryColorMap` tidak diubah | `gradlew :app:testDebugUnitTest :app:assembleDebug --offline` BUILD SUCCESSFUL; 70/70 test lulus (0 failure, 0 error); APK 124.936.371 byte |
| 19 Nov 2026 | Halaman daftar admin hanya punya satu mode tampilan (posts card-only; exhibitions/logs/accounts list-only) dan jumlah item per halaman kecil (4–5), tidak bisa menampilkan 100+ data | Toggle **Card/List** + pemilih **per page (12/25/50/100/200)** di 4 halaman (posts, exhibitions, logs, accounts). Logika allowlist terpusat di `App\Support\AdminListOptions` (nilai di luar daftar jatuh ke default), controller tetap tipis, query string (search/filter) dipertahankan saat berpindah mode/per page, halaman direset ke 1. Pagination existing sudah `withQueryString()` | `php artisan view:cache` sukses; `AdminListOptionsTest` 4 test / 14 assertion; `AdminListViewTest` 2 test / 34 assertion (render Card & List pada 4 halaman); `AdminPanelAksesTest` 3 test / 5 assertion |
| 19 Nov 2026 | Panel admin (Blade/Tailwind) memakai shadow berlebih (`shadow-2xl`, `shadow-lg`, `active:scale`), radius `rounded-3xl`, heading `font-bold`, serta placeholder hardcode (`29/09/26`, `127.0.0.1`, `Admin 1`, `user`) dan gambar placeholder eksternal (Unsplash) yang bisa gagal dimuat | Polish konsistensi UI admin: radius `rounded-2xl`/`rounded-xl`, shadow direduksi/dihapus, transisi `transition-colors`, heading `font-semibold`, fallback teks jadi em-dash (`—`), gambar placeholder diganti inline SVG data-URI (tidak bergantung jaringan) pada 9 view (accounts, auth/login, exhibitions, posts, logs, layouts app/pagination/sidebar/topbar) | `php artisan view:cache` sukses; `AdminPanelAksesTest` 3 test / 5 assertion lulus |
| 19 Nov 2026 | README (root & backend) memuat angka usang: backend "37 tests, 176 assertions", Android tanpa angka, versi tertulis "Laravel 11/11/12" padahal terverifikasi Laravel 13.34.0 | README root dirapikan menyeluruh: badge jumlah test (87 backend / 70 Android), versi Laravel 13, dokumentasi fitur terbaru (Request Meeting UC-17, Unity viewer, multi-bahasa, hardening keamanan mobile), tabel dokumentasi & status proyek, panduan `.env` mobile; README backend disesuaikan (Laravel 13, 87 tests / 406 assertions) | README root & `backend-api/backend/README.md` kini konsisten dengan angka terverifikasi pada dokumen ini |
| 19 Nov 2026 | Token otentikasi & data pengguna tersimpan plaintext di SharedPreferences dan ikut cloud backup; logging HTTP level BODY menyala di build release (password/OTP/JWT tercetak di logcat); cleartext HTTP aktif di semua build type; activity Unity `exported=true`; URL `http://10.0.2.2:8000` hardcode di kode | `SessionManager` kini mengenkripsi token & JSON pengguna via `util/SecurePrefs` (AES-256-GCM AndroidKeyStore, migrasi transparan dari plaintext); interceptor logging hanya ditambahkan saat `BuildConfig.DEBUG`; `setBaseUrl` menolak non-HTTPS di release; manifest memakai `network_security_config.xml` HTTPS-only (debug src-set tetap mengizinkan emulator/LAN) + `backup_rules`/`data_extraction_rules` mengecualikan prefs sensitif; activity Unity ditimpa `exported="false"`; fallback URL kini dari `BuildConfig.BASE_URL`/`AppConfig.baseUrl` | `gradlew :app:testDebugUnitTest` 70/70 lulus; `assembleDebug` BUILD SUCCESSFUL; grep tidak menemukan lagi URL cleartext hardcode di `app/src/main` |
| 19 Nov 2026 | Fallback dummy data tampil di jalur production (Schedule/History/Explore menampilkan data palsu saat API gagal/kosong); `RequestMeetingViewModel` memakai tanggal awal hardcode Sept 2026; pesan galat Request Meeting hardcode Indonesia; dead code (`com/ven/app/unity/UnityLauncher`, test template `com.example.venmobile`, warna template, import tak terpakai); `RegisterRoute` & `ProfileViewModel` menempel di file lain | Fallback dummy dihapus total (empty state asli); `RequestMeetingViewModel` menerima `today: LocalDate = LocalDate.now()` (injeksikan tanggal tetap di test); galat validasi jadi enum `RequestMeetingValidationError` dipetakan ke `stringResource`; dead code dihapus; `RegisterRoute.kt` & `ProfileViewModel.kt` dipisah | `gradlew :app:testDebugUnitTest` 70/70 lulus (termasuk `ScreenRouteTest` yang diperbaiki); `assembleDebug` BUILD SUCCESSFUL |
| 19 Nov 2026 | Tombol `VexButton.Outline` memakai warna hardcode putih/hitam (rusak di mode terang); window background tema selalu gelap (kilatan gelap bagi pengguna mode terang); `RequestMeetingScreen` menulis ulang text field, mengabaikan `isLoading`, dan memakai warna/spacing hardcode; warna hardcode di `CarouselExhibitions` & `CreateOptionsBottomSheet` | `Outline` kini transparan dengan token tema (`colors.text`/`colors.border`); `themes.xml` dipecah light (`values/`) & night (`values-night/`); `RequestMeetingScreen` memakai `VexTextArea`, indikator loading banner, fallback warna `colors.primary`, token `VexSpace`/typography; token baru `accentWarning` ditambahkan ke kedua palet | `assembleDebug` BUILD SUCCESSFUL; preview Dark/Light tersedia pada semua komponen yang diubah |
| 18 Nov 2026 | Unit test Android usang | `app/src/test/.../ui/navigation/ScreenRouteTest.kt:20` diperbarui sesuai format route `create_exhibit_editor` terbaru (query `title/description/bannerUrl/tipe`) | `gradlew :app:testDebugUnitTest` — `ScreenRouteTest` lulus; 70/70 test lulus |
| 18 Nov 2026 | Modul Schedule & Request Meeting (UC-17) sebelumnya masih mock/simulasi | Dibuat `ApiUndangan`, `ScheduleViewModel` (4 tab: Approved, Waiting, Request, History; aksi Terima/Tolak & Join Room 3D Viewer), `RequestMeetingViewModel` terhubung ke `POST /api/undangan` dengan validasi jadwal lampau; endpoint `POST /api/undangan` diperluas menerima `id_pameran`/`id_karya` dan otomatis membuat ruangan privat | `tests/Feature/DiskusiUndanganTest.php` 8 test lulus; `ScheduleViewModelTest.kt` 5 test lulus; live smoke test emulator sukses mengajukan undangan ke pameran Alex Pratama dan muncul di tab Waiting |
| 18 Nov 2026 | Fitur AI Chat dan Multi-bahasa hilang pasca-merge branch Unity | Merestorasi source dari commit `6f338e9` & `234c250`: `AiChatHistoryPreferences`, `AiChatViewModel`, `AiChatScreen`, `ChatHistoryDrawer`, `AppLanguage`, `VenApplication`, `locales_config.xml`, `DateFormatter`, dan sinkronisasi 321 resource string. Mempertahankan konfigurasi `unityLibrary/build.gradle` (NDK 27.2, CMake 3.22.1) | APK build sukses (`assembleDebug`), Android unit test 70/71 lulus; smoke test emulator membuktikan switch English/Indonesian persisten dan AI Chat live Gemini terhubung |
| 18 Nov 2026 | Endpoint `/api/ai/tanya` menghasilkan **HTTP 500** `Unknown column 'nama' in 'field list'` saat memuat konteks pameran & karya | `AIChatController::buildKonteks`: relasi `kategori` pada tabel `karya` menggunakan nama kolom `kategori`, bukan `nama`. Diperbaiki menjadi `kategori:id_kategori,kategori` dan `$k->kategori->kategori` | Ditambahkan regression test `test_ai_chat_memuat_konteks_karya_dengan_relasi_kategori` di `AIChatGeminiTest.php`; backend suite 79/79 test lulus (381 assertion); uji live emulator menjawab pertanyaan pameran dan menolak pertanyaan out-of-scope |
| 13 Nov 2026 | Request `/api/*` tanpa header `Accept: application/json` menghasilkan **HTTP 500** `Route [login] not defined.` | `bootstrap/app.php`: `redirectGuestsTo` mengembalikan `null` untuk prefix `api`; `app/Exceptions/PenanganKesalahanApi.php` merender semua galat API sebagai JSON `{data, message}` (401/403/404/405/413/419/422/429/5xx) tanpa membocorkan detail internal. Halaman web 404 memakai Blade kustom `resources/views/errors/404.blade.php` | `tests/Feature/PenanganKesalahanApiTest.php` — 7 test lulus; `tests/Feature/Halaman404Test.php` — 2 test lulus; smoke test live `GET /api/profil` tanpa header → `401 application/json`, `GET /halaman-tidak-ada` → `404 text/html` |
| 13 Nov 2026 | Membuka endpoint `/api/*` langsung di browser menampilkan **JSON 401 mentah** (membingungkan pengguna), dan halaman 404 masih terlalu ramai (kartu besar + dekorasi) serta memakai nama "Virtual Exhibition Navigator" | `PenanganKesalahanApi` mendeteksi navigasi browser dan mengalihkan ke `/404`; klien API tetap menerima JSON. Blade 404 dirombak menjadi minimalis (logo kecil, angka 404 gradien, satu tombol, footer ringkas) dengan merek **Value Exhibition Network** | `PenanganKesalahanApiTest` — 9 test lulus (termasuk redirect browser dan JSON untuk `application/json`, wildcard, serta tanpa Accept); `Halaman404Test` — 2 test lulus. Smoke test live: browser → `302 /404`; `application/json`/`*/*`/tanpa Accept → `401 application/json`; `/halaman-tidak-ada` → `404 text/html` |
| 13 Nov 2026 | Membuka `http://localhost:8000/` langsung menampilkan/ mengarahkan pengunjung ke beranda & login admin sehingga panel admin mudah ditemukan | `routes/web.php`: route `/` mengembalikan `abort(404)` (tanpa redirect). Blade 404 tidak lagi memuat logo sebagai tautan maupun tombol "Kembali ke Beranda" agar tidak ada jalan pintas ke beranda | `AdminPanelAksesTest` — 3 test lulus (`GET /` → 404); `Halaman404Test` — 2 test lulus (tanpa "Kembali ke Beranda"); `PenanganKesalahanApiTest` — 9 test lulus. Smoke test live: `GET /` → `404 text/html` |

---

## 5. Kesimpulan

- **Backend:** 100% test lulus (**87 test, 406 assertion**). Seluruh modul inti (auth, profil, pameran, karya, undangan, pencarian, admin, audit keamanan, backup/restore, AI live Gemini) merespons normal pada smoke test live.
- **Android:** alur utama selesai dan berhasil di-build (APK debug 125.974.239 byte, ~126,0 MB). Autentikasi, home, explore, profil, settings, create exhibit, Schedule (Approved, Waiting, Request, History), Request Meeting (UC-17), AI Chat live dengan hapus percakapan per item, Multi-bahasa (EN/ID), dan viewer Unity sudah aktif tersambung API. Pill kategori dihapus dari kartu feed; halaman profil ditata ulang (top bar kanan, tab Posts/Exhibits, pill keahlian seragam & wrap, kontak interaktif, tombol Edit/Share Profile) dan form edit profil diperbaiki padding-nya; `VexSkillChip`/`VexTagChip` di explore memakai latar tint transparan dengan aksen warna kategori adaptif tema tanpa mengubah `CategoryColorMap`.
- **Keamanan mobile:** token & data sesi terenkripsi AES-256-GCM (AndroidKeyStore), logging HTTP hanya di debug, release HTTPS-only dengan pengecualian emulator pada debug, prefs sensitif dikecualikan dari backup, activity Unity non-exported, `proguard-rules.pro` siap (minify menunggu smoke test perangkat).
- **Dummy data dihapus total** dari jalur production — Schedule/History/Explore menampilkan empty state asli saat API kosong atau gagal; tersisa hanya sebagai bahan `@Preview`.
- **Belum selesai:** Create Post (unggah karya dari mobile) dan publish pameran dari editor Compose.
- **Catatan integrasi Unity:** UnityPlayerGameActivity terbuka normal (room 3D berjalan dengan fallback RoomDto), namun migrasi `id_pameran` pada tabel `objek` belum diterapkan di database aplikasi `ven` (baru di `ven_testing`).
- **Test Android:** **73/73 lulus** — 3 test baru `AiChatViewModelTest` (hapus obrolan non-aktif, aktif, dan tidak dikenal); `ScreenRouteTest` diperbaiki; 2 test template bawaan dihapus sehingga total berubah dari 71 menjadi 70, lalu 73 setelah penambahan test hapus obrolan.

---

## 6. Cara Verifikasi Ulang

```powershell
# Backend
cd backend-api/backend
$php = "C:\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe"
& $php artisan about
& $php artisan route:list
& $php artisan test
& $php vendor\phpunit\phpunit\phpunit tests/Feature/PenanganKesalahanApiTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/Halaman404Test.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/AdminPanelAksesTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/AIChatGeminiTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/DiskusiUndanganTest.php

# Android — build & unit test
cd ..\..\ven-mobile
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest

# Smoke test API live (terminal terpisah)
cd ..\backend-api\backend
& $php artisan serve --host=127.0.0.1 --port=8000
# Login: POST http://127.0.0.1:8000/api/auth/masuk
# Body: {"identitas":"fajri@ven.test","kata_sandi":"password123"}
# Undangan: GET/POST/PATCH http://127.0.0.1:8000/api/undangan (Bearer token)
# AI Tanya: POST http://127.0.0.1:8000/api/ai/tanya (Bearer token)
# Halaman 404 web: GET http://127.0.0.1:8000/halaman-tidak-ada
# Browser membuka API: GET http://127.0.0.1:8000/api/pameran/25 + Accept text/html → 302 /404
# Klien API: GET http://127.0.0.1:8000/api/pameran/25 (+ Accept application/json) → 401 JSON
```

**Database:** test wajib memakai `ven_testing`; database aplikasi adalah `ven`. Jangan arahkan test ke `ven`.
