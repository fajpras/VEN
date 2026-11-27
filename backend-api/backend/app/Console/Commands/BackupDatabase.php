<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;

class BackupDatabase extends Command
{
    protected $signature = 'ven:backup-db {--path= : Directory to store the backup file}';

    protected $description = 'Backup MySQL database using mysqldump. Stores the dump in the specified directory.';

    public function handle(): int
    {
        $host = env('DB_HOST', '127.0.0.1');
        $port = env('DB_PORT', '3306');
        $db = env('DB_DATABASE');
        $user = env('DB_DBA_USERNAME', 'dba');

        if (! $db) {
            $this->error('Variabel DB_DATABASE tidak di-set di .env');
            return self::FAILURE;
        }

        $dir = $this->option('path') ?: config('backup.path') ?: storage_path('app/backups');
        $dir = rtrim((string) $dir, '/\\');

        if ($dir === '') {
            $this->error('Path backup tidak valid. Periksa BACKUP_PATH di .env.');
            return self::FAILURE;
        }

        if (! is_dir($dir) && ! mkdir($dir, 0755, true) && ! is_dir($dir)) {
            $this->error("Tidak dapat membuat direktori backup: {$dir}");
            return self::FAILURE;
        }

        $file = rtrim($dir, DIRECTORY_SEPARATOR)
            .DIRECTORY_SEPARATOR.'backup_'.$db.'_'.now()->format('Ymd_His').'.sql';

        $binary = env('MYSQL_DUMP_PATH') ?: $this->cariBinerMysql('mysqldump');
        $kredensial = $this->berkasKredensial($user, (string) env('DB_DBA_PASSWORD', ''));

        // --single-transaction + --skip-lock-tables: dump konsisten tanpa
        // LOCK TABLES, sehingga backup tidak saling memblokir dengan transaksi
        // lain (termasuk transaksi PHPUnit saat seluruh suite berjalan).
        $command = escapeshellarg($binary)
            .' --defaults-extra-file='.escapeshellarg($kredensial)
            .' --single-transaction --skip-lock-tables --no-tablespaces'
            .' -h '.escapeshellarg((string) $host)
            .' -P '.escapeshellarg((string) $port)
            .' '.escapeshellarg((string) $db)
            .' > '.escapeshellarg($file);

        $this->info('Menjalankan backup...');
        exec($command, $output, $returnVar);
        @unlink($kredensial);

        if ($returnVar !== 0) {
            @unlink($file);
            $this->error('Backup gagal. Periksa apakah mysqldump terinstal dan kredensial benar.');
            return self::FAILURE;
        }

        $this->info("Backup berhasil: {$file}");
        return self::SUCCESS;
    }

    /**
     * Mencari biner MySQL bawaan Laragon, lalu fallback ke nama biner di PATH.
     */
    private function cariBinerMysql(string $nama): string
    {
        $kandidat = glob('C:/laragon/bin/mysql/*/bin/'.$nama.'.exe');

        return ! empty($kandidat) ? (string) end($kandidat) : $nama;
    }

    /**
     * Berkas opsi sementara agar kata sandi tidak muncul pada argumen proses
     * (argumen `-pSANDI` terlihat oleh pengguna lain lewat daftar proses).
     */
    private function berkasKredensial(string $user, string $password): string
    {
        $path = tempnam(sys_get_temp_dir(), 'ven-dump-');

        if ($path === false) {
            throw new \RuntimeException('Tidak dapat membuat berkas kredensial untuk mysqldump.');
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
