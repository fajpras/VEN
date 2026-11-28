#Requires -Version 5.1
<#
.SYNOPSIS
    Starter 1-klik untuk menyalakan Laravel API + Scheduler + Cloudflare Quick Tunnel (Opsi A).
    Otomatis memperbarui APP_URL di backend/.env dan BASE_URL di ven-mobile/.env.
#>

$ErrorActionPreference = "Stop"

$RepoRoot    = $PSScriptRoot
$BackendDir  = Join-Path $RepoRoot "backend-api\backend"
$MobileDir   = Join-Path $RepoRoot "ven-mobile"
$BackendEnv  = Join-Path $BackendDir ".env"
$MobileEnv   = Join-Path $MobileDir ".env"
$PhpExe      = "C:\laragon\bin\php\php-8.3.33-Win32-vs16-x64\php.exe"
$Cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

if (-not (Test-Path $PhpExe)) {
    $PhpExe = (Get-Command php -ErrorAction Stop).Source
}
if (-not (Test-Path $Cloudflared)) {
    $Cloudflared = (Get-Command cloudflared -ErrorAction Stop).Source
}

function Set-EnvKey {
    param([string]$FilePath, [string]$Key, [string]$Value)
    if (-not (Test-Path $FilePath)) {
        Set-Content -Path $FilePath -Value "$Key=$Value" -Encoding UTF8
        return
    }
    $lines = Get-Content -Path $FilePath
    $found = $false
    $updated = foreach ($line in $lines) {
        if ($line -match "^$([regex]::Escape($Key))=") {
            $found = $true
            "$Key=$Value"
        } else {
            $line
        }
    }
    if (-not $found) {
        $updated += "$Key=$Value"
    }
    Set-Content -Path $FilePath -Value $updated -Encoding UTF8
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  VEN - Laravel + Scheduler + Cloudflare Quick Tunnel" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Cek MySQL Laragon (port 3306)
$mysqlUp = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
if (-not $mysqlUp) {
    Write-Host "[PERINGATAN] MySQL di port 3306 belum berjalan!" -ForegroundColor Yellow
    Write-Host "             Silakan buka Laragon lalu klik 'Start All' terlebih dahulu." -ForegroundColor Yellow
} else {
    Write-Host "[OK] MySQL Laragon aktif di port 3306." -ForegroundColor Green
}

$procs = @()
$tunnelLog = Join-Path $env:TEMP "ven_cloudflared.log"
if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force -ErrorAction SilentlyContinue }

try {
    # 2. Jalankan Laravel API di 127.0.0.1:8000 (jika belum jalan)
    $port8000 = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
    if (-not $port8000) {
        Write-Host "[1/3] Menyalakan Laravel API di http://127.0.0.1:8000 ..." -ForegroundColor Cyan
        $pServe = Start-Process -FilePath $PhpExe `
            -ArgumentList "artisan", "serve", "--host=127.0.0.1", "--port=8000" `
            -WorkingDirectory $BackendDir `
            -WindowStyle Hidden -PassThru
        $procs += $pServe
        Start-Sleep -Seconds 2
    } else {
        Write-Host "[1/3] Port 8000 sudah aktif, memakai server yang sedang berjalan." -ForegroundColor Green
    }

    # 3. Jalankan Laravel Scheduler (auto-expire & pengingat undangan)
    Write-Host "[2/3] Menyalakan Laravel Scheduler (schedule:work) ..." -ForegroundColor Cyan
    $pSched = Start-Process -FilePath $PhpExe `
        -ArgumentList "artisan", "schedule:work" `
        -WorkingDirectory $BackendDir `
        -WindowStyle Hidden -PassThru
    $procs += $pSched

    # 4. Jalankan Cloudflare Quick Tunnel (pakai QUIC + IPv4 agar tidak diputus NAT/ISP tiap 60 detik seperti HTTP/2)
    $publicUrl = $null
    $pTunnel = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force -ErrorAction SilentlyContinue }
        Write-Host "[3/3] Membuka Cloudflare Quick Tunnel -> http://127.0.0.1:8000 (percobaan $attempt/3) ..." -ForegroundColor Cyan
        $pTunnel = Start-Process -FilePath $Cloudflared `
            -ArgumentList "tunnel", "--url", "http://127.0.0.1:8000", "--protocol", "quic", "--edge-ip-version", "4", "--logfile", $tunnelLog `
            -WindowStyle Hidden -PassThru
        $procs += $pTunnel

        # Tunggu URL https://*.trycloudflare.com muncul (baca dengan FileShare.ReadWrite agar tidak mengunci log)
        for ($i = 0; $i -lt 50; $i++) {
            Start-Sleep -Milliseconds 500
            if (Test-Path $tunnelLog) {
                try {
                    $fs = [System.IO.File]::Open($tunnelLog, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
                    $sr = New-Object System.IO.StreamReader($fs)
                    $content = $sr.ReadToEnd()
                    $sr.Close()
                    $fs.Close()
                    if ($content -match "https://[a-zA-Z0-9-]+\.trycloudflare\.com") {
                        $publicUrl = $Matches[0]
                        break
                    }
                } catch {}
            }
            if ($pTunnel.HasExited) {
                Write-Host "    cloudflared berhenti (exit code $($pTunnel.ExitCode)), mencoba ulang..." -ForegroundColor Yellow
                Start-Sleep -Seconds 2
                break
            }
        }
        if ($publicUrl) { break }
    }

    if (-not $publicUrl) {
        throw "Gagal mendapatkan URL trycloudflare.com setelah 3 percobaan. Cek log: $tunnelLog"
    }

    # 5. Otomatis perbarui APP_URL (backend) & BASE_URL (Android)
    Set-EnvKey -FilePath $BackendEnv -Key "APP_URL" -Value $publicUrl
    Set-EnvKey -FilePath $MobileEnv  -Key "BASE_URL" -Value ($publicUrl.TrimEnd('/') + "/api/")
    & $PhpExe (Join-Path $BackendDir "artisan") config:clear | Out-Null

    Write-Host ""
    Write-Host "=====================================================" -ForegroundColor Green
    Write-Host "  TUNNEL AKTIF!" -ForegroundColor Green
    Write-Host "  Public URL  : $publicUrl" -ForegroundColor White
    Write-Host "  API URL     : $publicUrl/api/" -ForegroundColor White
    Write-Host "=====================================================" -ForegroundColor Green
    Write-Host "  - backend-api/backend/.env (APP_URL) sudah diperbarui." -ForegroundColor Gray
    Write-Host "  - ven-mobile/.env (BASE_URL) sudah diperbarui." -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Untuk pasang APK dengan URL baru ini, jalankan di terminal lain:" -ForegroundColor Yellow
    Write-Host "    cd `"$MobileDir`"; .\gradlew.bat :app:assembleDebug --offline; adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Tekan Ctrl+C di jendela ini untuk menghentikan server & tunnel." -ForegroundColor Magenta

    while (-not $pTunnel.HasExited) {
        Start-Sleep -Seconds 2
    }
}
finally {
    Write-Host "`nMenghentikan proses background (Laravel, Scheduler, Cloudflared)..." -ForegroundColor Yellow
    foreach ($p in $procs) {
        if ($p -and -not $p.HasExited) {
            Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Write-Host "Selesai." -ForegroundColor Green
}
