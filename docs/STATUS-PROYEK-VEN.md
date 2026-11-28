# Status Proyek VEN — Daftar Modul, Verifikasi, dan Kendala

> **Tanggal verifikasi:** 27 November 2026
>
> **Branch aktif:** `main`
>
> **Commit terakhir:** commit ini (Discovery Explore di luar kategori minat + filter kategori aktif; melanjutkan perbaikan Penjadwalan 27 Nov); baseline sebelumnya `af32687`.
>
> **Perubahan 28 November 2026 (Discovery Explore di Luar Minat + Filter Kategori):**
> - **Backend (`backend-api/backend`):** `PencarianController` memvalidasi `kategori[]`, `exclude`, dan `refresh`; Discovery karya/pameran mendukung konten di luar kategori minat pengguna (fallback semua kategori bila kosong); tanpa filter keyword tetap lintas kategori, dengan filter hasil keyword dibatasi ke kategori terpilih; filter eksplisit mengalahkan aturan luar-minat; `tipe=pameran` mendukung filter kategori melalui karya. Respons pameran kini menyertakan `kategori` perwakilan dari karya pertama (bukan `tipe`).
> - **Android (`ven-mobile`):** `ExploreViewModel` memuat kategori dari API (fallback 14 kategori), mengirim filter saat sheet ditutup, memuat karya Discovery, menyimpan riwayat query nyata (`ExploreHistoryPreferences`, maks 10), menghapus fallback hasil kosong ke Discovery, serta menampilkan state loading/error/empty. Discovery kembali berupa grid karya 3 kolom tanpa seksi pameran; badge kategori di kartu dihapus sehingga kartu hanya menampilkan gambar/judul; `ExploreScreen` menambahkan submit keyboard SearchBar dan tombol filter tetap tampil saat pencarian aktif. `selectedCategories` kini dikirim sebagai `kategori[]` (key ber-bracket agar diparse Laravel sebagai array) pada `searchSemua`, `searchKarya`, dan `searchPameran`; menutup sheet saat hasil aktif mengulang pencarian dengan filter terbaru.
> - **Verifikasi:** PHPUnit `PencarianTest` + `PencarianDiscoveryTest` **19/19 lulus (83 assertions)**; suite penuh backend **149 test (139 lulus, 10 gagal non-Explore)**. Android `:app:testDebugUnitTest --offline` **115/115 lulus**; `:app:compileDebugKotlin` dan `:app:assembleDebug` sukses. Smoke emulator: Discovery/Home/Profil tanpa badge kategori (gambar/judul saja); filter `3D` + keyword `robot` berhasil menampilkan hasil tanpa error.
>
> **Perubahan 27 November 2026 (Perbaikan Fitur Penjadwalan, Undangan Diskusi, Jendela Sesi 15 Menit & Proteksi Ruangan Privat):**
> - **Backend (`backend-api/backend`):**
>   - Timezone & Database: Normalisasi penyimpanan `waktu` undangan ke UTC (`CarbonImmutable::parse()->utc()`) untuk mengatasi pergeseran jam lokal, penambahan kolom `alasan` via migrasi pada `undangan`, validasi `AjukanUndanganRequest` & `TanggapiUndanganRequest` dengan guard jadwal lampau (HTTP 409) dan penolakan bentrok sesi < 60 menit (HTTP 409).
>   - Jendela Sesi & Otorisasi: Implementasi `JendelaSesi` (-15m s.d. +15m) dan `PenjagaAksesRuangan` untuk `room-dto` dan Edgegap server (`/api/room-server/{roomKey}` & `/api/ruangan/{id}/server`); `LayananTokenVoice` mendukung query `?undangan=` untuk pemilihan sesi spesifik; `PameranController::storeRuangan` idempoten via `firstOrCreate`.
>   - Scheduled Command: `ven:kedaluwarsakan-undangan` (`KedaluwarsakanUndangan.php`) dijadwalkan tiap 5 menit untuk menandai undangan `diproses` yang terlewat > 15 menit menjadi `ditolak` dengan alasan terstruktur.
> - **Android (`ven-mobile`):**
>   - Formatting & Waktu: `DateFormatter` mendukung parsing offset ISO 8601 (`+07:00` / `Z`), utilitas `isPastWithGrace` dan `isWithinSessionWindow`; `RequestMeetingViewModel` mengirim waktu dengan `ISO_OFFSET_DATE_TIME` dan menginjeksi clock; kalender menonaktifkan (`isEnabled = false`) tanggal lampau.
>   - Schedule State & UI: Paginasi `loadMore()`, sorting berbasis waktu (approved/waiting urut naik, riwayat urut turun), toleransi 15 menit ke tab riwayat, auto-refresh 60 detik & tick 30 detik; Join Room dipagari `isJoinable` (hanya aktif dalam jendela sesi); `EventDetailSheet` menampilkan hint jendela sesi, alasan penolakan/kedaluwarsa, dan tombol salin kode sesi (tautan).
>   - Integrasi Unity & Voice: `UnityLauncher` me-reset `UnityBridge.currentRoomJson = null` dan menghapus pre-populasi mock; `UnityBridge` mengirim `OnRoomError` jika ruangan privat ditolak (mencegah bypass fallback publik); `VoiceChatViewModel` dan `ApiUndangan` meneruskan `idUndangan` ke endpoint voice token.
> - **Verifikasi:** PHPUnit backend (`DiskusiUndanganTest`, `DiskusiVoiceTest`, `DiskusiRuanganRoomDtoTest`, `EdgegapServerTest`, `KedaluwarsakanUndanganTest`, `PameranRuanganTest`) **36/36 lulus (104 assertions)**; Android unit tests (`:app:testDebugUnitTest --offline`) **99/99 lulus (0 failure, 0 error)**.
>
> **Perubahan 25 November 2026 (Seeder Dummy + Aset, Audit Trail & Komponen Admin, Video Autoplay & Dialog Suspend Android):**
> - **Fresh DB & Seeder Dummy:** `ven:initialize-db --fresh` pada DB lokal `ven`; `DummyKaryaPameranSeeder` + `GeneratesSeedAssets` + 2 klip MP4 ter-track (`database/seeders/assets/video/`) mengisi **8 kreator**, **10 pameran** (9 aktif, 1 arsip), **72 karya** lintas 14 kategori (**49 poster**, **23 video**, **4 arsip**) secara idempoten (`missing=0`, MIME `image/png` & `video/mp4` valid).
> - **Audit Trail & Admin Web:** `LogSistem` menstandarkan format `[SEVERITY | KONTROL]` berbasis prinsip Keamanan Basis Data untuk **Security Log** dan **Activity Log** lengkap dengan modal **Audit Trail** di kolom paling kanan; tab **Accounts** pada Archive kini khusus akun tersuspend (`ditangguhkan`/`nonaktifkan`, bukan terhapus); seluruh komponen UI berulang dipisah terkategori di `resources/views/admin/components/{toolbar,navigation,modals}/`; titik (`•`) aturan sandi di Pengaturan Admin dihilangkan.
> - **Android (`ven-mobile`):** `AutoPlayVideoPlayer` (`TextureView` + `MediaPlayer`, rasio `16:9`, mute/unmute, single-codec debounce `150ms`, `clipToBounds` + `clipToOutline`), optimasi `ImageCacheManager` (`RGB_565`, `MediaMetadataRetriever` thumbnail + semaphore 2, deduplikasi file, LRU disk cache 80 MB, OOM guard) & `HomeViewModel` (`distinctBy`, `takeLast(100)`), serta `AccountSuspendedDialog` bergaya Instagram (EN/ID) dari payload terstruktur `sanksi`.
> - **Verifikasi:** `vite build` sukses (492 ms); PHPUnit (`AdminListViewTest`, `AdminPanelAksesTest`, `PenanganKesalahanApiTest`) **lulus seluruhnya**; stress test API bulk data (200 OK, 7–49 ms, peak 30 MB); Android `:app:testDebugUnitTest --offline` **89/89 lulus** (termasuk `BulkFeedStabilityTest`).
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
| Test backend | `php artisan test` / `phpunit --no-coverage` | ⚠️ **149 test — 139 lulus, 10 gagal** (19/19 test Explore lulus; 10 kegagalan berada di suite Diskusi/Log/Profil/Exhibition yang tidak terkait perubahan ini) |
| Fallback galat API | `php artisan test --filter=PenanganKesalahanApiTest` | ✅ **9 test lulus** — 401, 403, 404, 405, 422, 500 tanpa kebocoran detail internal + redirect browser & JSON untuk klien API |
| Halaman galat web | `GET /halaman-tidak-ada` | ✅ Blade 404 minimalis (tanpa kartu/dekorasi), mandiri (tanpa aset Vite), merek "Value Exhibition Network", respons `text/html`, tanpa tautan ke beranda |
| Akar situs | `GET /` | ✅ HTTP **404** — panel admin tidak terekspos, tidak redirect ke login/beranda |
| API dibuka dari browser | `GET /api/pameran/25` + `Accept: text/html` | ✅ `302` → `/404`, halaman 404 kustom ditampilkan; klien API (`application/json`, wildcard, tanpa Accept) tetap menerima JSON |
| Smoke test API live | `artisan serve` + login `fajri@ven.test` | ✅ Login, `/api/pameran`, `/api/karya/rekomendasi`, `/api/profil`, `/api/pencarian`, `/api/ai/tanya`, `/api/undangan` merespons normal |
| Admin Web Panel | `GET /admin/login` | ✅ HTTP 200 |
| Audit Lighthouse (desktop & mobile) | `npx lighthouse https://VEN.test/ven/backend-api/backend/public/admin/login --preset=desktop` / `--form-factor=mobile` | ✅ **100/100/100/100** (Performance, Accessibility, Best Practices, SEO) pada kedua profil; FCP/LCP 0,23–0,87 s, TBT 0–61 ms, CLS 0; verifikasi 24 November 2026 |
| Build Android | `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | ✅ **BUILD SUCCESSFUL** (build tanpa `--offline` karena dependensi LiveKit/core-splashscreen diunduh; `ndkVersion` app disamakan ke 27.2 karena NDK 28.2 lokal tidak lengkap) |
| Artefak APK | `app/build/outputs/apk/debug/app-debug.apk` | ✅ Ada, 135.169.223 byte (~135,2 MB; bertambah karena lib native WebRTC LiveKit + aset splash/intro) |
| Test Android | `.\gradlew.bat :app:testDebugUnitTest` | ✅ **115 test — 115 lulus** (0 failure, 0 error; termasuk 16 test `ExploreViewModelTest` untuk Discovery luar-minat, filter, riwayat, loading/error, dan load more) |
| Audit & perapian koding bersih | `git diff --check` + build/test | ✅ Tidak ada `dd/dump`, `!!`, `TODO/FIXME`, import tak terpakai, atau logging debug di rilis; struktur `Route`/`Screen` dipisah sesuai konvensi |

**Catatan lingkungan:** perintah `php` tidak tersedia di PATH. Gunakan path Laragon:

```powershell
C:\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe artisan test
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
| 6 | Diskusi & Undangan | `undangan` (index/store/show/status/tanggapi), normalisasi penyimpanan waktu UTC, pembatasan jendela sesi 15 menit (`JendelaSesi`), otorisasi berbasis jendela (`PenjagaAksesRuangan` untuk `room-dto` dan Edgegap server), auto-expire via `ven:kedaluwarsakan-undangan` tiap 5 menit, pencegahan bentrok slot < 60 menit, kolom `alasan` terstruktur, voice token privat (`?undangan=`), otomatisasi ruangan privat pameran idempoten | ✅ kode; ⚠️ voice live menunggu kredensial LiveKit |
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
| 4 | Explore & pencarian | `ExploreViewModel` → `ApiPencarian`; Discovery hanya karya di luar minat (fallback semua), filter kategori, riwayat query persisten, grid 3 kolom | ✅ |
| 5 | Profil & User Profile | `ProfileUiState` → `ApiProfil` (profil, jumlah karya/pameran, karya milik) | ✅ |
| 6 | Settings akun | Ganti email, ganti kata sandi, status akun, tema gelap/terang | ✅ |
| 7 | Buat pameran (template & info) | `CreateExhibitViewModel`, upload banner via `ApiPameran` | ✅ |
| 8 | Viewer 3D Unity | `UnityLoadingScreen` + `UnityLauncher` + endpoint `room-dto`, fallback DTO anti black screen (room Template01 berjalan); editor 3D lewat `Screen.CreateExhibitEditor` → `UnityLoadingScreen` | ✅ kolom `objek.id_pameran` sudah ada di `ven`, `room-dto` live berjalan |
| 9 | AI Chat | `AiChatViewModel` → `ApiAiChat` + `AiChatHistoryPreferences`/`AiChatHistoryStore` + drawer riwayat chat persisten + hapus percakapan per item (menu ⋮, dialog konfirmasi), live Gemini | ✅ |
| 10 | Theme engine | `ThemePreferences`, Material 3 | ✅ |
| 11 | Multi-bahasa | `AppLanguage`, `VenApplication`, `locales_config.xml`, `AppCompatDelegate`, 321 keys (English default & Indonesian) | ✅ |
| 12 | Schedule (Jadwal Undangan) | `ScheduleViewModel` → `ApiUndangan` (4 tab: Approved, Waiting, Request, History; aksi Terima/Tolak dengan alasan, paginasi `loadMore()`, sorting berbasis jadwal, toleransi 15 menit ke riwayat, refresh otomatis 60s & tick 30s, gating `isJoinable`, salin kode sesi, dan Join Room 3D Viewer) | ✅ |
| 13 | Request Meeting (UC-17) | `RequestMeetingViewModel` → `ApiUndangan.createUndangan`, format ISO_OFFSET_DATE_TIME, pembatasan kalender hari lampau (`isEnabled = false`), validasi waktu lampau berbasis clock, input jam/menit/AM-PM, catatan | ✅ |
| 14 | Riwayat Undangan di Settings | `HistoryScreen` tersambung ke `ScheduleViewModel` (menampilkan riwayat undangan riil dari API) | ✅ |
| 15 | Penguatan keamanan mobile | `util/SecurePrefs` (AES-256-GCM AndroidKeyStore) + `SessionManager` terenkripsi; `ApiClient` logging BODY hanya debug & `setBaseUrl` menolak non-HTTPS di release; `network_security_config.xml` (release HTTPS-only), `backup_rules`/`data_extraction_rules` mengecualikan `ven_auth_pref` & `ven_ai_chat_pref`, activity Unity `exported=false`, `proguard-rules.pro` siap (minify off), cooldown OTP registrasi | ✅ |
| 16 | Polish UI & konsistensi design token | Tombol `VexButton.Outline` adaptif tema (perbaikan mode terang), `themes.xml` light/night (window background sesuai mode), `RequestMeetingScreen` memakai `VexTextArea` + loading state banner + spacing/typography token, token `accentWarning` baru, warna hardcode dihapus dari `CarouselExhibitions`/`CreateOptionsBottomSheet`; pill kategori `VexSkillChip` memakai latar tint transparan dan aksen warna kategori adaptif tema; pill kategori pada kartu feed (`PostCard`) dihapus; kartu feed ditata ulang bergaya Instagram (header, aksi bawah media, caption paragraf); halaman profil ditata ulang (top bar kanan, tab Posts/Exhibits, pill keahlian seragam & wrap, kontak interaktif + divider, tombol Edit/Share Profile); form edit profil diperbaiki padding & label-nya | ✅ |
| 17 | Buat Post / unggah karya | `CreatePostRoute.kt` (picker foto/video nyata, validasi MIME & ukuran 50 MB, cache `File`, pratinjau bitmap/thumbnail video), `CreatePostViewModel.kt` (state/eventUDF), `CreatePostScreen.kt` stateless 2 langkah, kategori dari `ApiProfil.kategori()`, unggah multipart via `ApiKarya.kt` ke `POST /api/karya`; deskripsi wajib. Tanpa kolom `tautan`/Product Link | ✅ |
| 18 | Perubahan UI login (Daffa) | Field Boxed + label di Login/Register, `VexButtonShape.Boxed` pada tombol login/register/lupa sandi (default `Pill` untuk layar lain) lewat `VexButton.kt` | ✅ |
| 19 | Voice chat ruangan privat (LiveKit) | `VoiceChatService` (foreground, proses utama, notifikasi CallStyle Bisu/Tutup), `VoiceChatViewModel` (UDF, `PenyediaTokenVoice` injectable), `KlienVoice`/`KlienVoiceLivekit` (audio-only), `ApiUndangan.voiceToken`, panel pre-join `VoiceChatPanel` di `EventDetailSheet`, izin RECORD_AUDIO/FOREGROUND_SERVICE_MICROPHONE/POST_NOTIFICATIONS, ikon `ic_voice_*`, strings `voice_*` EN+ID | ⚠️ kode + test selesai; smoke 2 perangkat menunggu kredensial LiveKit |
| 20 | Routing ruangan privat (Join Room → Unity) | `ScheduleEventItem.roomId` (dari `ruangan.idRuangan`), `Screen.ViewExhibit` arg opsional `?roomId=`, `UnityLauncher` extra `roomType` + reset `currentRoomJson = null`, `UnityBridge` memilih `/ruangan/{id}/room-dto` dan mengirim `OnRoomError` jika ditolak (anti bypass fallback publik); Join Room memicu gating waktu → izin mic → token spesifik `?undangan=` → voice service → viewer 3D privat | ⚠️ kode + test selesai; smoke menyusul setelah kredensial LiveKit |
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
| 8 | `php` tidak ada di PATH | Gunakan path Laragon (`C:\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe`) atau tambahkan ke environment variable |
| 9 | `GOOGLE_SERVER_CLIENT_ID` di `.env` berformat non-standar (`glowing-icon-510605-m1.apps.googleusercontent.com`) | Client ID Google OAuth umumnya berawalan angka; verifikasi nilai Web Client ID di Google Cloud Console agar login Google tidak gagal di produksi |

---

## 5. Kesimpulan

- **Backend:** Modul inti (auth, profil, pameran, karya, undangan dengan jendela sesi 15 menit & auto-expire, **voice privat-only + room-dto ruangan privat**, pencarian dengan Discovery luar-minat, admin, audit keamanan, backup/restore, AI live Gemini) merespons normal. Suite penuh **149 test (139 lulus, 10 gagal pada Diskusi/Log/Profil/Exhibition yang tidak terkait perubahan Explore)**; seluruh **19/19 test Explore lulus (83 assertions)**.
- **Android:** alur utama selesai dan berhasil di-build. Autentikasi, home, explore, profil, settings, create exhibit, Create Post, Schedule (dengan paginasi, sorting, gating jendela sesi, dan salin kode sesi), Request Meeting (UC-17, ISO-8601 offset & pembatasan hari lampau), AI Chat live, Multi-bahasa (EN/ID), viewer Unity (anti bypass ruang privat), **voice chat ruangan privat (LiveKit)**, dan **routing ruangan privat** aktif tersambung API. Voice baru bisa live setelah kredensial LiveKit diisi.
- **Keamanan mobile:** token & data sesi terenkripsi AES-256-GCM (AndroidKeyStore), logging HTTP hanya di debug, release HTTPS-only dengan pengecualian emulator pada debug, prefs sensitif dikecualikan dari backup, activity Unity non-exported, service voice `exported=false` + `foregroundServiceType="microphone"`, `proguard-rules.pro` siap (minify menunggu smoke test perangkat).
- **Dummy data dihapus total** dari controller web admin (`AdminAccountController`, `AdminPostController`, `AdminExhibitionController`, `AdminLogController`) serta jalur production mobile — seluruh daftar menampilkan empty state asli saat data kosong.
- **Belum selesai:** kredensial LiveKit (voice belum bisa diuji live), smoke test 2 perangkat untuk voice + ruangan privat, publish pameran dari editor Compose (alur mock `delay(800)`, dikembalikan sesuai keputusan pengguna), dan halaman Privacy Policy / Help yang masih placeholder. **Multiplayer realtime pameran utama** menunggu source Unity `.cs` di repo — rencananya Laravel Reverb untuk presence + posisi avatar; kontrak `UnityBridge` yang ada siap jadi titik sambung.
- **Catatan integrasi Unity:** UnityPlayerGameActivity terbuka normal; `room-dto` live berjalan (fallback RoomDto hanya untuk pameran publik jika backend bermasalah, ruangan privat mengirim `OnRoomError`). Source `.cs` Unity tidak ada di repo (hanya binary `il2cpp`).
- **Test Android:** **115/115 lulus** (0 failure, 0 error), termasuk 16 test baru `ExploreViewModelTest` (Discovery luar-minat, filter kategori, riwayat, loading/error, load more).

---

## 6. Cara Verifikasi Ulang

```powershell
# Backend
cd backend-api/backend
$php = "C:\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe"
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
