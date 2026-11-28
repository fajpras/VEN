<?php

use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Schedule;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');

// -----------------------------------------------------------------
// Pengingat H-1 otomatis untuk undangan diskusi privat (cek tiap jam)
// -----------------------------------------------------------------
Schedule::command('undangan:kirim-pengingat')
    ->hourly()
    ->withoutOverlapping();

Schedule::command('ven:kedaluwarsakan-undangan')
    ->everyFiveMinutes()
    ->withoutOverlapping();

// -----------------------------------------------------------------
// Automatic daily backup at 00:00 (Midnight)
// -----------------------------------------------------------------
Schedule::command('ven:backup-db')
    ->cron('0 0 * * *')
    ->withoutOverlapping()
    ->runInBackground();

// -----------------------------------------------------------------
// Retention: clean old backup files (older than 30 days) at 03:00 AM
// -----------------------------------------------------------------
Schedule::call(function () {
    $dir = config('backup.path', storage_path('app/backups'));
    $files = glob($dir . '/backup_*.sql');
    $threshold = time() - (30 * 86400); // 30 hari
    foreach ($files as $file) {
        if (is_file($file) && filemtime($file) < $threshold) {
            @unlink($file);
        }
    }
})->dailyAt('03:00');

