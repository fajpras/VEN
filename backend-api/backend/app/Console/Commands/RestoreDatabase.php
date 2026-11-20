<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use PDO;
use Throwable;

class RestoreDatabase extends Command
{
    protected $signature = 'ven:restore-db
        {--path= : Full path to the .sql dump file to restore}
        {--db= : Target database name (default: DB_DATABASE)}
        {--fresh : Drop lalu buat ulang database target sebelum restore (khusus sandbox verifikasi)}';

    protected $description = 'Restore MySQL database from a .sql dump file.';

    public function handle(): int
    {
        $host = (string) env('DB_HOST', '127.0.0.1');
        $port = (string) env('DB_PORT', '3306');
        $db = $this->option('db') ?: env('DB_DATABASE', 'ven');
        $user = (string) env('DB_DBA_USERNAME', 'dba');
        $password = (string) env('DB_DBA_PASSWORD', '');

        if (! $db || ! preg_match('/^[A-Za-z0-9_]+$/', $db)) {
            $this->error('Nama database target kosong atau berisi karakter tidak valid.');
            return self::FAILURE;
        }

        $path = (string) $this->option('path');
        if ($path === '' || ! file_exists($path)) {
            $this->error('Opsi --path wajib diisi dengan path lengkap ke file .sql yang ada.');
            return self::FAILURE;
        }

        if ($this->option('fresh')) {
            if (! $this->siapkanDatabaseSandbox($host, $port, $db, $user, $password)) {
                return self::FAILURE;
            }
        }

        $binary = env('MYSQL_BINARY_PATH') ?: $this->cariBinerMysql();
        $kredensial = $this->berkasKredensial($user, $password);

        $this->info('Menjalankan restore...');
        $command = escapeshellarg($binary)
            .' --defaults-extra-file='.escapeshellarg($kredensial)
            .' -h '.escapeshellarg($host)
            .' -P '.escapeshellarg($port)
            .' '.escapeshellarg((string) $db)
            .' < '.escapeshellarg($path);

        exec($command, $output, $returnVar);
        @unlink($kredensial);

        if ($returnVar !== 0) {
            $this->error('Restore gagal. Periksa kredensial dan pastikan file dump valid.');
            return self::FAILURE;
        }

        $this->info('Restore berhasil.');
        return self::SUCCESS;
    }

    /**
     * Membuat database sandbox kosong untuk verifikasi disaster recovery.
     * Tanpa --fresh, database target harus sudah ada (perilaku restore biasa).
     */
    private function siapkanDatabaseSandbox(string $host, string $port, string $db, string $user, string $password): bool
    {
        try {
            $pdo = new PDO(
                "mysql:host={$host};port={$port};charset=utf8mb4",
                $user,
                $password,
                [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION],
            );
        } catch (Throwable $e) {
            $this->error('Gagal terhubung ke MySQL untuk menyiapkan sandbox: '.$e->getMessage());
            return false;
        }

        $pdo->exec("DROP DATABASE IF EXISTS `{$db}`");
        $pdo->exec("CREATE DATABASE `{$db}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        return true;
    }

    /**
     * Mencari biner mysql bawaan Laragon, lalu fallback ke nama biner di PATH.
     */
    private function cariBinerMysql(): string
    {
        $kandidat = glob('C:/laragon/bin/mysql/*/bin/mysql.exe');

        return ! empty($kandidat) ? (string) end($kandidat) : 'mysql';
    }

    /**
     * Berkas opsi sementara agar kata sandi tidak muncul pada argumen proses.
     */
    private function berkasKredensial(string $user, string $password): string
    {
        $path = tempnam(sys_get_temp_dir(), 'ven-restore-');

        if ($path === false) {
            throw new \RuntimeException('Tidak dapat membuat berkas kredensial untuk mysql client.');
        }

        $isi = "[client]\nuser={$user}\n";
        if ($password !== '') {
            $isi .= 'password="'.str_replace('"', '\\"', $password)."\"\n";
        }

        file_put_contents($path, $isi);
        @chmod($path, 0600);

        return $path;
    }
}
