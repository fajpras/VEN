# Docker VEN dan pengukuran VPS

Konfigurasi ini menjalankan Laravel + PHP-FPM, Nginx, dan MySQL 8.4 pada satu
host. Konfigurasi awal dituning untuk menguji VPS **2 vCPU / 2 GB RAM**;
limit Docker bukan jaminan kebutuhan VPS, tetapi membuat hasil pengukuran lebih
mudah dibandingkan.

## Persiapan pertama

```powershell
cd backend-api/backend
Copy-Item .env.docker.example .env.docker
```

Edit `.env.docker` dan isi minimal `APP_KEY`, `VEN_CUSTOM_KEY`,
`MYSQL_ROOT_PASSWORD`, serta password empat akun `DB_*`. Generate dua key
aplikasi di PowerShell (buat nilai berbeda untuk masing-masing):

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Isi `APP_KEY=base64:<hasil>` dan gunakan string acak lain minimal 32 karakter
untuk `VEN_CUSTOM_KEY`. Jangan memakai password Laragon atau commit `.env.docker`.

## Build dan jalankan

```powershell
docker compose build --no-cache
docker compose up -d db app web
docker compose ps
```

Entrypoint aplikasi menjalankan `ven:setup-db`, migrasi melalui `mysql_dba`,
menjalankan setup DB sekali lagi agar grant granular diterapkan setelah tabel
terbentuk, lalu `artisan optimize`. `VEN_DB_CLIENT_HOST=%` membuat akun DB dapat dipakai
dari jaringan Docker; ini menghindari error grant ke `user@localhost` ketika
koneksi sebenarnya berasal dari container.

Buka `http://localhost:8080`. Untuk log:

```powershell
docker compose logs -f app
docker compose logs -f db
```

## Mengukur RAM dan CPU

```powershell
docker stats --no-stream
docker stats
```

Catat `MEM USAGE / LIMIT`, `MEM %`, `CPU %`, `NET I/O`, dan `BLOCK I/O`.
Gunakan angka **peak**, bukan idle. Ukur login, daftar pameran/karya, upload,
dan room DTO selama 5–10 menit per skenario.

## Load test bertahap

Compose menyediakan profile k6. Endpoint default `GET /api/exhibition_templates`
merupakan endpoint API yang ada (tanpa token akan memberi 401 yang diharapkan);
isi `LOAD_TOKEN` dengan token testing sementara bila ingin menguji request terautentikasi.

```powershell
$env:VUS=5;  $env:DURATION='2m'; docker compose --profile loadtest run --rm loadtest
$env:VUS=15; $env:DURATION='5m'; docker compose --profile loadtest run --rm loadtest
$env:VUS=30; $env:DURATION='5m'; docker compose --profile loadtest run --rm loadtest
```

Di terminal lain jalankan `docker stats`. Skenario k6 berada di
`docker/loadtest/smoke.js`; tambahkan endpoint terautentikasi memakai token
testing khusus, bukan token pengguna nyata.

## Menentukan VPS

Sisakan 20–25% RAM untuk OS, Docker, dan lonjakan:

| Peak saat test | Keputusan awal |
|---|---|
| RAM total < 1.3 GB, CPU < 60% | 2C2G kemungkinan cukup untuk trafik kecil |
| RAM 1.3–1.7 GB atau CPU 60–80% | 2C4G lebih aman |
| RAM > 1.7 GB, OOM, atau CPU > 80% | Jangan pilih 2C2G |

Upload file besar, worker queue, relay multiplayer, dan LiveKit belum termasuk
dalam compose inti; ukur komponen tersebut terpisah sebelum produksi.

## Backup dan data

Volume `ven_mysql` menyimpan database dan `ven_storage` menyimpan upload.

```powershell
docker compose exec app php artisan ven:backup-db --path=storage/app/backups
docker compose cp app:/var/www/html/storage/app/backups ./backups
```

Jangan menjalankan `docker compose down -v` jika data ingin dipertahankan.
