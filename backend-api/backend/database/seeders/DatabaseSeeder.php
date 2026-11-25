<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        // Seeder adalah tugas pemeliharaan/inisialisasi database, gunakan koneksi DBA jika ada
        $original = DB::getDefaultConnection();
        if (config()->has('database.connections.mysql_dba')) {
            DB::setDefaultConnection('mysql_dba');
        }

        try {
            $this->call([
                KategoriSeeder::class,
                PenggunaSeeder::class,
                ModelSeeder::class,
                // Menggunakan DummyKaryaPameranSeeder (termasuk generator aset PNG & salinan MP4 valid).
                // KaryaDanPameranSeeder & PameranSeeder lama dipertahankan tetapi tidak dipanggil bersamaan.
                DummyKaryaPameranSeeder::class,
            ]);
        } finally {
            DB::setDefaultConnection($original);
        }
    }
}

