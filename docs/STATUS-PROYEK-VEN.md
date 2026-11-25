# Status Proyek VEN — Daftar Modul, Verifikasi, dan Kendala

> **Tanggal verifikasi:** 25 November 2026
>
> **Branch aktif:** `main`
>
> **Commit terakhir:** commit ini (optimasi loading panel admin web, di atas multiplayer Edgegap); baseline sebelum perubahan adalah `192d4c8`. Verifikasi dilakukan pada working tree sebelum commit.
>
> **Perubahan 25 November 2026 (multiplayer real-time Unity — Edgegap + Netcode):**
> - **Backend:** `App\Services\Edgegap\LayananEdgegapServer` (deploy dedicated server Edgegap per ruangan dengan env `ROOM_ID` + tag `room-{id}`, memakai ulang deployment aktif via cache/tag agar semua pengunjung satu ruangan masuk ke instance yang sama), `Pengguna\RoomServerController` + rute `POST /api/room-server/{roomKey}`, `POST /api/pameran/{pameran}/server`, `POST /api/ruangan/{ruangan}/server` (`throttle:30,1`, ruangan privat hanya untuk pemilik/peserta disetujui/admin). `config/services.php` menambah blok `vivox` & `edgegap`; `.env.example` menambah `VIVOX_*` dan `EDGEGAP_*`. `PenyusunRoomDto` kini mengirim `viewerName` (nama panggilan) untuk nametag avatar.
> - **Unity (`unityLibrary`):** disinkronkan ulang dari ekspor branch Unity gabungan (Editor/PlayScene `integration-test` + Vivox + Netcode) — `ArtworkDialog`, `GridPlacer`, tekstur `PlaceableObject`, `NativeBridge`, `TemplateLoader` kembali utuh, ditambah `RoomServerStarter` (server headless otomatis pindah ke `PlayScene`), `RoomServerClient` (minta server ke Laravel lalu connect UDP) dan `RoomMultiplayerSync` (sinkron posisi/rotasi avatar + nametag via `CustomMessagingManager`). `unityLibrary/build.gradle`: dependensi `:VivoxNative@aar`, sintaks Gradle 10-ready, `import GradleException`, dan `Il2CppOutputProject` dikecualikan dari indexing IDE.
> - **Verifikasi:** `phpunit tests/Feature/EdgegapServerTest.php tests/Feature/DiskusiRuanganRoomDtoTest.php` **8/8 lulus (32 assertion)**; `:app:assembleDebug` **BUILD SUCCESSFUL**; uji live 2 akun di Pameran #13 — kedua klien mendapat deployment Edgegap yang sama (`Status.READY`, UDP `gameport`) dan avatar pengguna lain terlihat berhadapan dengan nametag-nya. Server Edgegap berhenti sendiri saat kosong (120 s tanpa pemain / 30 s setelah pemain terakhir keluar).
>
> **Perubahan 25 November 2026 (optimasi loading panel admin web):**
> - **Ikon sidebar tanpa flash:** logo + 4 ikon navigasi di `resources/views/admin/layouts/sidebar.blade.php` kini dirender sebagai inline SVG dari aset yang sama (`public/assets/admin/*.svg`) dengan `currentColor`, sehingga tidak ada request gambar terpisah setiap navigasi dan ikon langsung tampil bersama HTML. File aset tetap dipertahankan (dipakai favicon, halaman login, dan 404).
> - **Transisi halaman halus:** `resources/css/app.css` menambahkan cross-document View Transitions (`@view-transition { navigation: auto; }` + fade 120–180 ms) dan menghormati `prefers-reduced-motion`.
> - **Indikator navigasi:** `resources/js/app.js` menambahkan progress bar tipis 2 px (`#BA18F5 → #D257FF`) yang muncul saat klik link internal atau submit form, lalu reset pada `pageshow` (back/forward cache). `app.js` dimasukkan kembali ke input Vite tetapi hanya dimuat di layout admin; halaman login tetap tanpa JS.
> - **Font self-hosted:** Plus Jakarta Sans 400–700 diunduh lewat `bunny()` di `vite.config.js` dan dirender dengan `Vite::fonts()` di layout admin maupun login; link Google Fonts dihapus sehingga tidak ada request render-blocking eksternal. `--font-sans` Tailwind diset ke Plus Jakarta Sans.
> - **Gambar daftar:** avatar akun, thumbnail post, dan banner exhibition diberi `loading="lazy"` + `decoding="async"` agar tidak berebut bandwidth dengan render halaman.
> - **Verifikasi:** `npm run build` sukses (Vite 8.3.1; `app-DHGNlqQs.css` 85,29 kB, `app-C-ZIkHAa.js` 1,45 kB, 4 WOFF2 Plus Jakarta Sans self-hosted); full backend suite **113/113 test lulus** (531 assertion), termasuk `AdminListViewTest` dan `AdminPanelAksesTest`. Smoke test visual di browser belum dijalankan pada sesi ini.
>
> **Perubahan 24 November 2026 (sinkronisasi aset Unity `export_output` + VivoxNative):**
> - **Sinkronisasi `ven-mobile/unityLibrary`:** menyinkronkan penuh hasil ekspor `D:\UNITY\export_output\unityLibrary` (`libs/VivoxNative.aar`, `src/main/assets`, `src/main/cpp`, `src/main/Il2CppOutputProject`, `src/main/java`, `src/main/jniLibs`, `src/main/jniStaticLibs`, `src/main/res`, `src/main/resources`, dan `proguard-unity.txt`).
> - **Konfigurasi portabel dipertahankan:** `ven-mobile/unityLibrary/build.gradle` tetap memakai auto-discovery NDK dari Android SDK + proteksi anti-overwrite `libil2cpp.so` (`outPath.length() < 1000`) dengan tambahan dependensi `implementation(name: 'VivoxNative', ext:'aar')`; `ven-mobile/unityLibrary/src/main/AndroidManifest.xml` menambahkan izin `android.permission.ACCESS_NETWORK_STATE`.
> - **Verifikasi:** `:unityLibrary:buildIl2Cpp` selesai bersih (ExitCode 0, `libil2cpp.so` 92.438.136 byte) dan `:app:assembleDebug` **BUILD SUCCESSFUL** (`app-debug.apk` 145.076.188 byte / ~145,1 MB).
>
> **Perubahan 24 November 2026 (SEO & Lighthouse 100/100/100/100 — desktop & mobile):**
> - **Halaman publik satu-satunya** (`GET /admin/login`, root sengaja 404) kini lolos audit Lighthouse 12 penuh: **Performance 100, Accessibility 100, Best Practices 100, SEO 100** pada profil *mobile* (FCP/LCP 0,77–0,87 s, TBT 0–61 ms, CLS 0) dan *desktop* (FCP/LCP 0,23–0,27 s, TBT 17 ms, CLS 0). Audit dijalankan dua kali tiap profil via `npx lighthouse` pada `https://VEN.test/ven/backend-api/backend/public/admin/login`.
> - **`resources/views/admin/auth/login.blade.php`:** tambah `<meta name="description">`, `<main>` landmark + `<h1 sr-only>`, `<label sr-only>` pada input email/password, `aria-label`/`aria-pressed` + target 32px pada tombol toggle sandi, hapus email default `admin@ven.test` (kebocoran identitas admin), `autocomplete` pada input, SVG kubus di-inline (nol request gambar), dan background gradien 4 lapis `blur-*` diganti satu aset WebP 17,5 KB (`public/assets/admin/ven-glow.webp`) — visual identik, biaya paint turun drastis di perangkat low-end. `layouts/app.blade.php`: font Google dihapus (font Plus Jakarta Sans kini di-*self-host* via Vite/bunny), JS 0-byte dihapus dari `@vite` (25 Nov: `resources/js/app.js` diaktifkan kembali khusus layout admin untuk progress bar; login tetap tanpa JS).
> - **`vite.config.js`, `resources/css/app.css`, `public/.htaccess`, aset build:** Vite hanya menghasilkan CSS + font lokal; cache header `immutable` untuk aset statis; `resources/js/app.js` (0 byte) dan `public/favicon.ico` (0 byte) dihapus, favicon asli ditaruh di root vhost Laragon (25 Nov: `app.js` dibuat ulang berisi progress bar khusus layout admin).
> - **Lingkungan Laragon (di luar repo):** Apache HTTP→HTTPS 301 + vhost TLS 443 memakai sertifikat ECDSA self-signed (`etc/ssl/ven.test`), `mod_deflate` (gzip: HTML 7,8 KB→2,2 KB, CSS 84 KB→15 KB) dan `mod_http2` diaktifkan; OPcache PHP aktif dengan `revalidate_freq=0` (menghindari stale compiled view). Akses lokal sekarang **https://VEN.test/...**; sertifikat self-signed perlu diklik "Advanced/Proceed" di browser biasa (Lighthouse CLI memakai `--ignore-certificate-errors`).
> - **Verifikasi:** `phpunit tests/Feature/AdminPanelAksesTest.php` **3/3 test lulus** (5 assertion); perbandingan screenshot Chrome headless sebelum/sesudah menunjukkan tampilan login identik. Belum ada commit/push otomatis sesuai aturan `no-git-push`.
>
> **Perubahan 24 November 2026 (pemisahan log aktivitas & keamanan web admin + pembersihan mockup data controller):**
> - **Pemisahan log di panel admin:** Halaman `/admin/activity-log` kini memuat layout header sejajar (`flex-col sm:flex-row gap-4` konsisten dengan halaman lain) yang menyatukan Search Bar (`flex-1`) dan tab switcher `[Log Aktivitas]` (`?tab=aktivitas`) / `[Log Keamanan]` (`?tab=keamanan`) dalam 1 baris. Tab aktivitas menampilkan data audit trail `LogAktivitas` (pesan aksi, entitas, status, pengguna, waktu), sedangkan tab keamanan menampilkan data `LogKeamanan` (event autentikasi, endpoint, method, badge status kode HTTP, IP address, user agent). Modal detail beradaptasi dinamis menampilkan atribut sesuai tipe log.
> - **Koreksi logging publikasi pameran:** `PameranController::publishFromEditor` yang sebelumnya salah mencatat publikasi pameran ke `LogSistem::keamanan` diperbaiki agar tercatat ke `LogSistem::aktivitas`.
> - **Pembersihan mockup/dummy data:** Menghapus seluruh blok `catch (\Throwable)` berisi ratusan baris data koleksi palsu (`$mockUsers`, `$mockPosts`, `$mockExhibitions`, `$mockLogs`) pada `AdminAccountController`, `AdminPostController`, `AdminExhibitionController`, dan `AdminLogController`. Seluruh controller web admin kini murni meng-query Eloquent database riil.
> - **Verifikasi:** Backend **111/111 test lulus** (517 assertion) lewat `php artisan test`, termasuk test baru pemisahan tab log pada `AdminListViewTest.php`.
>
> **Perubahan 23 November 2026 (polish UI minimalis halaman AI Assistant Android):**
> - **AI Assistant UI (`ui/screens/ai/`):** `ChatMessageAi.kt` diubah dari `TextAlign.Justify` ke `TextAlign.Start` dengan badge avatar AI minimalis (`ic_nav_ai_filled` beraksen ungu halus) dan tipografi `15sp/23sp`; `ChatBubbleUser.kt` memakai aksen `colors.primary` dengan teks putih kontras (`20dp` radius, tail `6dp`); `AiChatInput.kt` dirampingkan (`52dp`, multi-line hingga 4 baris, *hairline border* reaktif, dan tombol Send melingkar); `AiChatEmptyState.kt` dilengkapi emblem AI minimalis + 3 *quick prompt chips* yang dapat diketuk langsung; `AiChatTopBar.kt` & `AiChatScreen.kt` diberi divider tipis `0.5dp` dan spasi percakapan lebih proporsional.
> - **Verifikasi:** `:app:assembleDebug` **BUILD SUCCESSFUL** dan APK berhasil diinstal ke emulator (`emulator-5554`).
>
> **Lingkungan:** Windows + Laragon, PHP 8.3.33, Laravel 13.34.0, MySQL, Android Gradle 9.6

Dokumen ini merangkum hasil audit langsung terhadap repositori: modul apa yang sudah selesai, apakah benar-benar bisa dijalankan, bagian mana yang masih berupa mock, serta kendala yang ditemukan beserta rekomendasi perbaikannya.

> ⚠️ **Dokumen hidup:** setiap penambahan/perubahan fitur wajib memperbarui file ini pada commit yang sama. Lihat aturan di [`.agents/rules/status-proyek.md`](../.agents/rules/status-proyek.md).

---

## 1. Ringkasan Hasil Verifikasi

| Pemeriksaan | Perintah | Hasil |
|---|---|---|
| Konfigurasi backend | `php artisan about` | ✅ Laravel 13.34.0, PHP 8.3.33, DB MySQL, storage link aktif |
| Daftar route backend | `php artisan route:list` | ✅ 99 route terdaftar |
| Test backend | `php artisan test` / `phpunit --no-coverage` | ✅ **111 test — 111 lulus** (517 assertion), termasuk test baru pemisahan tab log aktivitas & keamanan |
| Fallback galat API | `php artisan test --filter=PenanganKesalahanApiTest` | ✅ **9 test lulus** — 401, 403, 404, 405, 422, 500 tanpa kebocoran detail internal + redirect browser & JSON untuk klien API |
| Halaman galat web | `GET /halaman-tidak-ada` | ✅ Blade 404 minimalis (tanpa kartu/dekorasi), mandiri (tanpa aset Vite), merek "Value Exhibition Network", respons `text/html`, tanpa tautan ke beranda |
| Akar situs | `GET /` | ✅ HTTP **404** — panel admin tidak terekspos, tidak redirect ke login/beranda |
| API dibuka dari browser | `GET /api/pameran/25` + `Accept: text/html` | ✅ `302` → `/404`, halaman 404 kustom ditampilkan; klien API (`application/json`, wildcard, tanpa Accept) tetap menerima JSON |
| Smoke test API live | `artisan serve` + login `fajri@ven.test` | ✅ Login, `/api/pameran`, `/api/karya/rekomendasi`, `/api/profil`, `/api/pencarian`, `/api/ai/tanya`, `/api/undangan` merespons normal |
| Admin Web Panel | `GET /admin/login` | ✅ HTTP 200 |
| Audit Lighthouse (desktop & mobile) | `npx lighthouse https://VEN.test/ven/backend-api/backend/public/admin/login --preset=desktop` / `--form-factor=mobile` | ✅ **100/100/100/100** (Performance, Accessibility, Best Practices, SEO) pada kedua profil; FCP/LCP 0,23–0,87 s, TBT 0–61 ms, CLS 0; verifikasi 24 November 2026 |
| Build Android | `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | ✅ **BUILD SUCCESSFUL** (build tanpa `--offline` karena dependensi LiveKit/core-splashscreen diunduh; `ndkVersion` app disamakan ke 27.2 karena NDK 28.2 lokal tidak lengkap) |
| Artefak APK | `app/build/outputs/apk/debug/app-debug.apk` | ✅ Ada, 135.169.223 byte (~135,2 MB; bertambah karena lib native WebRTC LiveKit + aset splash/intro) |
| Test Android | `.\gradlew.bat :app:testDebugUnitTest` | ✅ **82 test — 82 lulus** (0 failure, 0 error; termasuk 4 test `StartDestinationResolverTest` + 1 kasus `ScreenRouteTest` untuk `Screen.Intro`) |
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
| 6 | Diskusi & Undangan | `undangan` (index/store/show/status/tanggapi), voice token privat-only (`LayananTokenVoice`, guard 403 untuk ruangan umum), `ruangan/{id}/room-dto` (otorisasi peserta via `PenyusunRoomDto`), otomatisasi ruangan privat pameran, pencegahan self-invitation & duplicate active, eager load banner & pengguna | ✅ kode; ⚠️ voice live menunggu kredensial LiveKit |
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
| 8 | Viewer 3D Unity | `UnityLoadingScreen` + `UnityLauncher` + endpoint `room-dto`, fallback DTO anti black screen (room Template01 berjalan); editor 3D lewat `Screen.CreateExhibitEditor` → `UnityLoadingScreen` | ✅ kolom `objek.id_pameran` sudah ada di `ven`, `room-dto` live berjalan |
| 9 | AI Chat | `AiChatViewModel` → `ApiAiChat` + `AiChatHistoryPreferences`/`AiChatHistoryStore` + drawer riwayat chat persisten + hapus percakapan per item (menu ⋮, dialog konfirmasi), live Gemini | ✅ |
| 10 | Theme engine | `ThemePreferences`, Material 3 | ✅ |
| 11 | Multi-bahasa | `AppLanguage`, `VenApplication`, `locales_config.xml`, `AppCompatDelegate`, 321 keys (English default & Indonesian) | ✅ |
| 12 | Schedule (Jadwal Undangan) | `ScheduleViewModel` → `ApiUndangan` (4 tab: Approved, Waiting, Request, History; aksi Terima/Tolak & Join Room 3D Viewer) | ✅ |
| 13 | Request Meeting (UC-17) | `RequestMeetingViewModel` → `ApiUndangan.createUndangan`, validasi tanggal/jam lampau, input jam/menit/AM-PM, catatan | ✅ |
| 14 | Riwayat Undangan di Settings | `HistoryScreen` tersambung ke `ScheduleViewModel` (menampilkan riwayat undangan riil dari API) | ✅ |
| 15 | Penguatan keamanan mobile | `util/SecurePrefs` (AES-256-GCM AndroidKeyStore) + `SessionManager` terenkripsi; `ApiClient` logging BODY hanya debug & `setBaseUrl` menolak non-HTTPS di release; `network_security_config.xml` (release HTTPS-only), `backup_rules`/`data_extraction_rules` mengecualikan `ven_auth_pref` & `ven_ai_chat_pref`, activity Unity `exported=false`, `proguard-rules.pro` siap (minify off), cooldown OTP registrasi | ✅ |
| 16 | Polish UI & konsistensi design token | Tombol `VexButton.Outline` adaptif tema (perbaikan mode terang), `themes.xml` light/night (window background sesuai mode), `RequestMeetingScreen` memakai `VexTextArea` + loading state banner + spacing/typography token, token `accentWarning` baru, warna hardcode dihapus dari `CarouselExhibitions`/`CreateOptionsBottomSheet`; pill kategori `VexSkillChip` memakai latar tint transparan dan aksen warna kategori adaptif tema; pill kategori pada kartu feed (`PostCard`) dihapus; kartu feed ditata ulang bergaya Instagram (header, aksi bawah media, caption paragraf); halaman profil ditata ulang (top bar kanan, tab Posts/Exhibits, pill keahlian seragam & wrap, kontak interaktif + divider, tombol Edit/Share Profile); form edit profil diperbaiki padding & label-nya | ✅ |
| 17 | Buat Post / unggah karya | `CreatePostRoute.kt` (picker foto/video nyata, validasi MIME & ukuran 50 MB, cache `File`, pratinjau bitmap/thumbnail video), `CreatePostViewModel.kt` (state/eventUDF), `CreatePostScreen.kt` stateless 2 langkah, kategori dari `ApiProfil.kategori()`, unggah multipart via `ApiKarya.kt` ke `POST /api/karya`; deskripsi wajib. Tanpa kolom `tautan`/Product Link | ✅ |
| 18 | Perubahan UI login (Daffa) | Field Boxed + label di Login/Register, `VexButtonShape.Boxed` pada tombol login/register/lupa sandi (default `Pill` untuk layar lain) lewat `VexButton.kt` | ✅ |
| 19 | Voice chat ruangan privat (LiveKit) | `VoiceChatService` (foreground, proses utama, notifikasi CallStyle Bisu/Tutup), `VoiceChatViewModel` (UDF, `PenyediaTokenVoice` injectable), `KlienVoice`/`KlienVoiceLivekit` (audio-only), `ApiUndangan.voiceToken`, panel pre-join `VoiceChatPanel` di `EventDetailSheet`, izin RECORD_AUDIO/FOREGROUND_SERVICE_MICROPHONE/POST_NOTIFICATIONS, ikon `ic_voice_*`, strings `voice_*` EN+ID | ⚠️ kode + test selesai; smoke 2 perangkat menunggu kredensial LiveKit |
| 20 | Routing ruangan privat (Join Room → Unity) | `ScheduleEventItem.roomId` (dari `ruangan.idRuangan`), `Screen.ViewExhibit` arg opsional `?roomId=`, `UnityLauncher` extra `roomType`, `UnityBridge` memilih `/ruangan/{id}/room-dto`; Join Room memicu izin mic → token → voice service → viewer 3D privat | ⚠️ kode + test selesai; smoke menyusul setelah kredensial LiveKit |
| 21 | Splash native + Intro pra-login | `core-splashscreen` + `Theme.VEN.Splash` + `ic_splash_logo.xml` + `installSplashScreen()`; `SplashScreen.kt` (logo, tagline, progress); `ui/screens/onboarding/Intro*` (`IntroScreen`/`IntroRoute`/`IntroViewModel`/`IntroUiState`/`IntroSlide`, `HorizontalPager`, Skip/Next); `OnboardingPreferences` + `StartDestinationResolver`; aset `intro_3d_exhibit.png` & `intro_meet_connect.png` (1024px transparan); string EN+ID | ✅ kode + 82/82 unit test + `assembleDebug`; ⚠️ smoke visual belum (emulator tidak tersambung) |

### 2.3 Artefak Pendukung

- **Postman:** `postman/` berisi koleksi *VEN API - Complete Routes Test*, *VEN Audit Keamanan & Database* (7 folder), *VEN API - Pengguna & Auth Tests*, beserta environment.
- **Dokumentasi:** `backend-api/backend/docs/` (7 pilar + panduan test) dan `ven-mobile/docs/DESIGN.md`.
- **APK debug:** `ven-mobile/app/build/outputs/apk/debug/app-debug.apk`.
- **Unity:** modul `unityLibrary` sudah di-include di `settings.gradle.kts` dan activity Unity terdaftar di `AndroidManifest.xml`.

---

## 3. Bagian yang Masih Mock / Belum Tersambung ke Backend

| Fitur | Lokasi | Kondisi |
|---|---|---|
| Publish pameran dari editor Compose | `.../create/exhibit/editor/ExhibitEditorViewModel.kt` (`delay(800)`) | Simulasi; tidak menyimpan ke database. Jalur yang benar adalah Unity (`Screen.CreateExhibitEditor` → `UnityLoadingScreen`) |
| Privacy Policy / Help | `SettingsPlaceholderScreen.kt` | Masih placeholder |

> **Catatan 20 November 2026:** Create Post tidak lagi mock — sudah tersambung ke `POST /api/karya` lewat `ApiKarya.kt` (lihat modul 2.2 nomor 17). Editor pameran Compose dikembalikan sesuai keputusan pengguna; alur publish-nya tetap simulasi (sebelumnya memang demikian), sedangkan edit 3D produksi lewat Unity.

---

## 4. Kendala dan Rekomendasi Perbaikan

### Prioritas Tinggi

✅ **Tidak ada.** Kendala `objek.id_pameran` pada DB `ven` sudah terselesaikan (lihat "Kendala yang Sudah Diperbaiki", 20 Nov 2026).

### Prioritas Sedang

| No | Kendala | Dampak | Rekomendasi |
|:--:|---|---|---|
| 1 | Kredensial LiveKit kosong (`LIVEKIT_API_KEY`/`SECRET`; `LIVEKIT_URL` placeholder `wss://livekit.domain-anda.com`) | Endpoint `/api/ruangan/{id}/voice` mengembalikan 503; voice chat belum bisa diuji live | Daftar LiveKit Cloud (tier gratis) atau self-host Docker, lalu isi `.env`; smoke test 2 emulator: A buat pameran → B request meeting → A approve → keduanya Join Room (voice tersambung, mute lewat notifikasi) |
| 2 | Source Unity (`.cs`) tidak ada di repo, hanya binary `il2cpp` | Integrasi Unity tidak dapat diaudit dari source; verifikasi butuh perangkat/emulator. Token Bearer masih dikirim via intent extra ke proses `:unity` (`UnityLauncher` → `UnityBridge`) — risiko eksternal sudah ditekan (activity Unity kini `exported="false"`), tetapi token tetap tampil di intent dump. **Multiplayer realtime pameran utama (Fase berikutnya) membutuhkan source ini** | Simpan proyek Unity sebagai submodule/folder terpisah atau dokumentasikan prosedur build; jangka panjang: sampaikan token lewat mekanisme IPC terbatas |
| 3 | Perubahan binary Unity belum di-commit | `libil2cpp.so` (arm64) termodifikasi dan `compile-data.json` masih untracked | Tinjau ulang lalu commit secara terkontrol (hindari commit binary besar tanpa kebutuhan) |
| 4 | R8/obfuskasi (`isMinifyEnabled`) masih nonaktif | APK release mudah dibedah; `proguard-rules.pro` sudah disiapkan (keep rules Gson/Retrofit/UnityBridge) | Aktifkan `isMinifyEnabled = true` lalu smoke test build release di perangkat fisik (Unity + Gson + LiveKit/WebRTC) sebelum rilis |

### Prioritas Rendah / Lingkungan

| No | Kendala | Rekomendasi |
|:--:|---|---|
| 6 | `BASE_URL` mobile default `http://10.0.2.2:8000/api/` | Hanya untuk emulator; ubah `ven-mobile/.env` untuk perangkat fisik. Pastikan URL HTTPS dipakai saat rilis (release build menolak base URL non-HTTPS) |
| 7 | APK berukuran ~126 MB karena Unity binary | Pertimbangkan Android App Bundle atau ABI split |
| 8 | `php` tidak ada di PATH | Gunakan path Laragon (`C:\laragon\bin\php\php-8.3.33-Win32-vs16-x64\php.exe`) atau tambahkan ke environment variable |
| 9 | `GOOGLE_SERVER_CLIENT_ID` di `.env` berformat non-standar (`glowing-icon-510605-m1.apps.googleusercontent.com`) | Client ID Google OAuth umumnya berawalan angka; verifikasi nilai Web Client ID di Google Cloud Console agar login Google tidak gagal di produksi |

### Kendala yang Sudah Diperbaiki

| Tanggal | Kendala | Perbaikan | Bukti |
|---|---|---|---|
| 25 Nov 2026 | Navigasi panel admin web memakai full page reload; ikon sidebar berupa `<img>` tanpa cache header di `artisan serve` sehingga hilang dulu saat loading, font Google render-blocking, dan tidak ada transisi/indikator navigasi | Logo + ikon sidebar dijadikan inline SVG dari aset yang sama; View Transitions cross-document + progress bar navigasi; Plus Jakarta Sans di-self-host via `bunny()` + `Vite::fonts()` (Google Fonts dihapus); gambar daftar diberi `loading="lazy"`/`decoding="async"` | `npm run build` sukses; full backend suite **113/113 test lulus** (531 assertion), termasuk `AdminListViewTest` 3/3 (45 assertion) dan `AdminPanelAksesTest` 3/3 (5 assertion) |
| 24 Nov 2026 | Log aktivitas dan log keamanan belum terpisah di web admin (`/admin/activity-log`); controller admin masih memuat ratusan baris data dummy mockup (`catch \Throwable`) | Web admin `/admin/activity-log` dilengkapi tab switcher `[Log Aktivitas]` (`?tab=aktivitas`) & `[Log Keamanan]` (`?tab=keamanan`) dengan kolom list/card dan modal detail adaptif; `PameranController` publikasi dicatat ke `LogAktivitas`; seluruh blok data mockup palsu di `AdminAccountController`, `AdminPostController`, `AdminExhibitionController`, dan `AdminLogController` dihapus total | `tests/Feature/AdminListViewTest.php` 3 test lulus (45 assertion); full backend suite **111/111 test lulus** (517 assertion) |
| 23 Nov 2026 | Field judul pada alur Create Exhibit masih tampil **pill** sementara deskripsi boxed; penyebabnya default `VexTextField` = `Pill` dan tiga call site (`CreateExhibitInfoScreen`, `ExhibitDetailsBottomSheet`, `ExhibitDetailsView`) tidak mengirim `variant` | Default `VexTextField` dan `VexPasswordField` diubah ke `VexTextFieldVariant.Boxed`; form aplikasi konsisten boxed tanpa mengubah call site yang sudah eksplisit. Varian `Pill` kini tidak dipakai layar aplikasi | `:app:testDebugUnitTest` **82/82 lulus**, `:app:assembleDebug` BUILD SUCCESSFUL, APK 135.169.223 byte |
| 23 Nov 2026 | Setelah splash, pengguna yang sudah punya token tetap diarahkan ke Login karena `isLoggedIn` di `AppNavigation.kt` dihitung tetapi tidak dipakai; splash juga tidak punya penanda apakah intro pernah dilihat | `StartDestinationResolver` baru (debug showExplore → Home, sudah login → Home, belum login → Intro/Login) dipakai di `onSplashFinished`; intro ditandai per perangkat lewat `OnboardingPreferences` (`ven_onboarding_pref`, terpisah dari `onboardingSelesai` akun); route `Screen.Intro` + layar 3 slide; splash native `core-splashscreen` dengan ikon persegi dan latar `#1F1F1F` | `:app:testDebugUnitTest` **82/82 lulus** (4 test baru `StartDestinationResolverTest` + 1 kasus `ScreenRouteTest`), `:app:assembleDebug` BUILD SUCCESSFUL, APK 135.169.223 byte; smoke visual belum (tidak ada emulator tersambung) |
| 21 Nov 2026 | `voiceToken` justru mengizinkan voice di ruangan **umum** (tanpa cek apa pun), kontradiksi dengan desain publik/privat; logika token + JWT menumpuk di controller; ruangan privat tidak punya room-dto sendiri sehingga Join Room selalu memuat scene pameran utama | Guard baru: token voice hanya untuk `jenis === 'privat'` (`KesalahanVoice::hanyaPrivat`, 403). Ekstraksi `LayananTokenVoice` (service + JWT) dan `PenyusunRoomDto` (payload room 3D bersama). Endpoint baru `GET /api/ruangan/{id}/room-dto` berotorisasi peserta/pemilik/admin dengan `roomId => "ruangan-{id}"`, `isPublic => false`. Android: `ScheduleEventItem.roomId` → route `?roomId=` → extra `roomType` → `UnityBridge` memilih endpoint ruangan; voice LiveKit via foreground service + izin mikrofon | Backend **110/110 test** (506 assertion) termasuk `DiskusiVoiceTest` 7 test (200 peserta/penyelenggara, **403 ruangan umum**, 403 tanpa sesi, 403 di luar jendela, 503 tanpa kredensial, 404 yatim) & `DiskusiRuanganRoomDtoTest` 6 test; Android **77/77 test** (3 baru `VoiceChatViewModelTest`), `:app:compileDebugKotlin` & `:app:assembleDebug` BUILD SUCCESSFUL, APK 134.739.756 byte |
| 20 Nov 2026 | Create Post di mobile hanya simulasi (`delay(400)`), tidak menyimpan data ke server; field "Product Link"/kolom `tautan` ditambahkan di luar rencana | Dibuat `ApiKarya.kt` (multipart `POST /api/karya`), `CreatePostRoute.kt` (picker foto/video nyata, validasi MIME/ukuran 50 MB, cache `File`, pratinjau), `CreatePostScreen.kt` stateless, `CreatePostViewModel.kt` UDF; kategori dari `ApiProfil.kategori()`, deskripsi wajib. Kolom `tautan` di-rollback di `ven`/`ven_testing`, file migration dihapus, fillable/rules backend dikembalikan, field Product Link & string terkait dihapus | Backend 97/97 test (468 assertion) termasuk `KaryaUnggahTest` 4 test (poster, video, deskripsi wajib, 401); `ven.karya.tautan` = 0 kolom; Android `:app:compileDebugKotlin`, `:app:testDebugUnitTest` 66/66, `:app:assembleDebug` BUILD SUCCESSFUL |
| 20 Nov 2026 | Create Post di mobile hanya simulasi (`delay(400)`), tidak menyimpan data ke server; field "Product Link"/kolom `tautan` ditambahkan di luar rencana | Dibuat `ApiKarya.kt` (multipart `POST /api/karya`), `CreatePostRoute.kt` (picker foto/video nyata, validasi MIME/ukuran 50 MB, cache `File`, pratinjau), `CreatePostScreen.kt` stateless, `CreatePostViewModel.kt` UDF; kategori dari `ApiProfil.kategori()`, deskripsi wajib. Kolom `tautan` di-rollback di `ven`/`ven_testing`, file migration dihapus, fillable/rules backend dikembalikan, field Product Link & string terkait dihapus | Backend 97/97 test (468 assertion) termasuk `KaryaUnggahTest` 4 test (poster, video, deskripsi wajib, 401); `ven.karya.tautan` = 0 kolom; Android `:app:compileDebugKotlin`, `:app:testDebugUnitTest` 66/66, `:app:assembleDebug` BUILD SUCCESSFUL |
| 20 Nov 2026 | Proses backup/restore database bisa hang; audit memakai database sandbox yang tidak terisolasi | `BackupDatabase.php`/`RestoreDatabase.php` diperbaiki; `AuditBackupDanRestoreTest.php` memakai sandbox `ven_testing_restore_sandbox` | Audit Backup/Restore lolos di suite penuh (97/97 test) |
| 20 Nov 2026 | Kolom `objek.id_pameran` belum ada di DB aplikasi `ven`, sehingga `GET /api/pameran/{id}/room-dto` gagal (HTTP 500) | Migrasi `2026_11_11_000001_add_id_pameran_to_objek_table.php` diterapkan di `ven` | Verifikasi `information_schema`: `ven.objek.id_pameran` = 1 kolom; `room-dto` live berjalan |
| 20 Nov 2026 | Perubahan UI login dari Daffa (commit `85f2687`) pernah di-revert di `65cd82e`, sehingga tidak aktif | Diterapkan ulang secara adaptif: field Boxed + label di Login/Register, `VexButtonShape.Boxed` pada tombol login/register/lupa sandi; default `Pill` agar layar lain tidak berubah | `:app:compileDebugKotlin` & `:app:assembleDebug` BUILD SUCCESSFUL |
| 20 Nov 2026 | Layout header profil tidak sesuai desain yang diminta (tombol Edit/Share mendominasi, tab di atas skill, kontak tanpa kartu) | Header disusun ulang: avatar + nama/statistik berdampingan, bio tanpa label "Deskripsi", kartu kontak bergaris beraksen pink, skill chip, lalu tab; tombol Edit/Share dan callback navigasi mati dihapus | `gradlew --offline :app:testDebugUnitTest :app:assembleDebug` BUILD SUCCESSFUL; 73/73 test lulus (0 failure, 0 error); APK 124.985.156 byte |
| 20 Nov 2026 | Cleanup "rapikan aplikasi" sempat menghapus editor pameran Compose, 32 string orphan, 4 drawable, `VexIcons.Editor*`, serta `VexTagChip`, `onCategoryColor`, `Motion.kt`, `SettingsUiState` + `dummySettingsUiState` | Sesuai keputusan pengguna, seluruh penghapusan tersebut **di-restore dari `HEAD`**; hanya penghapusan kolom `tautan` yang dipertahankan | Android **73/73 test** (0 failure, 0 error), `:app:assembleDebug` BUILD SUCCESSFUL, APK 124.985.156 byte |
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

- **Backend:** 100% test lulus (**111 test, 517 assertion**). Seluruh modul inti (auth, profil, pameran, karya, undangan, **voice privat-only + room-dto ruangan privat**, pencarian, admin, audit keamanan, backup/restore, AI live Gemini) merespons normal.
- **Android:** alur utama selesai dan berhasil di-build (APK debug 134.739.756 byte, ~134,7 MB). Autentikasi, home, explore, profil, settings, create exhibit, Create Post, Schedule, Request Meeting (UC-17), AI Chat live, Multi-bahasa (EN/ID), viewer Unity, **voice chat ruangan privat (LiveKit)**, dan **routing ruangan privat** aktif tersambung API. Voice baru bisa live setelah kredensial LiveKit diisi.
- **Keamanan mobile:** token & data sesi terenkripsi AES-256-GCM (AndroidKeyStore), logging HTTP hanya di debug, release HTTPS-only dengan pengecualian emulator pada debug, prefs sensitif dikecualikan dari backup, activity Unity non-exported, service voice `exported=false` + `foregroundServiceType="microphone"`, `proguard-rules.pro` siap (minify menunggu smoke test perangkat).
- **Dummy data dihapus total** dari controller web admin (`AdminAccountController`, `AdminPostController`, `AdminExhibitionController`, `AdminLogController`) serta jalur production mobile — seluruh daftar menampilkan empty state asli saat data kosong.
- **Belum selesai:** kredensial LiveKit (voice belum bisa diuji live), smoke test 2 perangkat untuk voice + ruangan privat, publish pameran dari editor Compose (alur mock `delay(800)`, dikembalikan sesuai keputusan pengguna), dan halaman Privacy Policy / Help yang masih placeholder. **Multiplayer realtime pameran utama** menunggu source Unity `.cs` di repo — rencananya Laravel Reverb untuk presence + posisi avatar; kontrak `UnityBridge` yang ada siap jadi titik sambung.
- **Catatan integrasi Unity:** UnityPlayerGameActivity terbuka normal; `room-dto` live berjalan (fallback RoomDto tetap ada sebagai anti–black screen). Source `.cs` Unity tidak ada di repo (hanya binary `il2cpp`).
- **Test Android:** **77/77 lulus** (0 failure, 0 error), termasuk 3 test baru `VoiceChatViewModelTest` dan kasus route privat di `ScreenRouteTest`. APK debug 134.739.756 byte (~134,7 MB).

---

## 6. Cara Verifikasi Ulang

```powershell
# Backend
cd backend-api/backend
$php = "C:\laragon\bin\php\php-8.3.33-Win32-vs16-x64\php.exe"
& $php artisan about
& $php artisan route:list
& $php artisan test
& $php vendor\phpunit\phpunit\phpunit tests/Feature/DiskusiVoiceTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/DiskusiRuanganRoomDtoTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/DiskusiUndanganTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/PenanganKesalahanApiTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/Halaman404Test.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/AdminPanelAksesTest.php
& $php vendor\phpunit\phpunit\phpunit tests/Feature/AIChatGeminiTest.php

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
# Voice privat: GET http://127.0.0.1:8000/api/ruangan/{id_ruangan}/voice (Bearer token; 503 tanpa kredensial LiveKit)
# Room-dto privat: GET http://127.0.0.1:8000/api/ruangan/{id_ruangan}/room-dto (Bearer token)
# AI Tanya: POST http://127.0.0.1:8000/api/ai/tanya (Bearer token)
# Halaman 404 web: GET http://127.0.0.1:8000/halaman-tidak-ada
# Browser membuka API: GET http://127.0.0.1:8000/api/pameran/25 + Accept text/html → 302 /404
# Klien API: GET http://127.0.0.1:8000/api/pameran/25 (+ Accept application/json) → 401 JSON
```

**Database:** test wajib memakai `ven_testing`; database aplikasi adalah `ven`. Jangan arahkan test ke `ven`.
