<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Audit & Test Kepatuhan:
 * 1. Backup Database (Manual & Otomatis)
 * 2. Restore Database Verification
 *
 * Catatan isolasi: restore diverifikasi ke database sandbox
 * (`ven_testing_restore_sandbox`) yang dibuat ulang dengan `--fresh`, bukan
 * ke `ven_testing`. Memulihkan ke database yang sama saat suite berjalan akan
 * menimpa skema di tengah transaksi test lain dan memicu metadata lock
 * (`DROP TABLE` menunggu transaksi aktif) sehingga suite menggantung.
 */
class AuditBackupDanRestoreTest extends TestCase
{
    private const DIREKTORI_BACKUP = 'framework/testing/audit_backups';

    private const DATABASE_SANDBOX = 'ven_testing_restore_sandbox';

    /**
     * Test alur backup database menggunakan Artisan command ven:backup-db.
     */
    public function test_artisan_backup_database_berhasil(): void
    {
        $direktori = storage_path(self::DIREKTORI_BACKUP);
        if (! is_dir($direktori)) {
            mkdir($direktori, 0755, true);
        }

        $exitCode = Artisan::call('ven:backup-db', [
            '--path' => $direktori,
        ]);

        $this->assertSame(0, $exitCode, 'Command ven:backup-db harus mengembalikan exit code 0.');

        $files = glob($direktori.'/backup_*.sql');
        $this->assertNotEmpty($files, 'File backup SQL harus terbentuk di direktori target.');

        $latestBackup = end($files);
        $content = (string) file_get_contents($latestBackup);
        $this->assertStringContainsString('MySQL dump', $content, 'File harus berisi header MySQL dump yang valid.');
        $this->assertStringContainsString('CREATE TABLE', $content, 'File dump harus memuat struktur tabel.');

        @unlink($latestBackup);
    }

    /**
     * Test alur verifikasi restore database ke sandbox terisolasi.
     */
    public function test_artisan_restore_database_berhasil(): void
    {
        $direktori = storage_path(self::DIREKTORI_BACKUP);
        if (! is_dir($direktori)) {
            mkdir($direktori, 0755, true);
        }

        Artisan::call('ven:backup-db', ['--path' => $direktori]);
        $files = glob($direktori.'/backup_*.sql');
        $this->assertNotEmpty($files);
        $backupFile = end($files);

        $exitCode = Artisan::call('ven:restore-db', [
            '--path' => $backupFile,
            '--db' => self::DATABASE_SANDBOX,
            '--fresh' => true,
        ]);

        $this->assertSame(0, $exitCode, 'Command ven:restore-db harus selesai dengan sukses (exit code 0).');

        @unlink($backupFile);
    }

    protected function tearDown(): void
    {
        $direktori = storage_path(self::DIREKTORI_BACKUP);
        if (is_dir($direktori)) {
            foreach (glob($direktori.'/*.sql') ?: [] as $file) {
                @unlink($file);
            }
        }

        $this->hapusDatabaseSandbox();

        parent::tearDown();
    }

    private function hapusDatabaseSandbox(): void
    {
        try {
            $pdo = new \PDO(
                'mysql:host='.env('DB_HOST', '127.0.0.1').';port='.env('DB_PORT', '3306').';charset=utf8mb4',
                (string) env('DB_DBA_USERNAME', 'root'),
                (string) env('DB_DBA_PASSWORD', ''),
                [\PDO::ATTR_ERRMODE => \PDO::ERRMODE_EXCEPTION],
            );
            $pdo->exec('DROP DATABASE IF EXISTS `'.self::DATABASE_SANDBOX.'`');
        } catch (\Throwable) {
            // Sandbox opsional; kegagalan membersihkan tidak boleh menggagalkan test.
        }
    }
}
