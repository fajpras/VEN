<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

return new class extends Migration
{
    public function up(): void
    {
        $conn = $this->getConnection();
        $db = DB::connection($conn);

        // 1. Lepas semua foreign key yang mengarah ke pameran.id_pameran
        foreach (['karya', 'objek', 'ruangan'] as $table) {
            $fkName = "{$table}_id_pameran_foreign";
            $exists = $db->selectOne(
                'SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = "FOREIGN KEY"',
                [$table, $fkName]
            );
            if ($exists) {
                $db->statement("ALTER TABLE `{$table}` DROP FOREIGN KEY `{$fkName}`");
            }
        }

        // 2. Ubah tipe kolom PK & FK menjadi CHAR(36)
        $db->statement('ALTER TABLE `pameran` MODIFY `id_pameran` CHAR(36) NOT NULL');
        $db->statement('ALTER TABLE `karya` MODIFY `id_pameran` CHAR(36) NULL');
        $db->statement('ALTER TABLE `objek` MODIFY `id_pameran` CHAR(36) NULL');
        $db->statement('ALTER TABLE `ruangan` MODIFY `id_pameran` CHAR(36) NULL');

        // 3. Konversi ID numerik lama ke UUID v7 (identik dengan Pengguna & Karya) secara konsisten di semua tabel terkait
        $rows = $db->table('pameran')->select('id_pameran')->get();
        foreach ($rows as $row) {
            $oldId = (string) $row->id_pameran;
            if (Str::isUuid($oldId)) {
                continue;
            }
            $newUuid = (string) Str::uuid7();
            $db->table('pameran')->where('id_pameran', $oldId)->update(['id_pameran' => $newUuid]);
            $db->table('karya')->where('id_pameran', $oldId)->update(['id_pameran' => $newUuid]);
            $db->table('objek')->where('id_pameran', $oldId)->update(['id_pameran' => $newUuid]);
            $db->table('ruangan')->where('id_pameran', $oldId)->update(['id_pameran' => $newUuid]);
        }

        // 4. Pasang kembali foreign key sesuai aturan referensial aslinya
        $db->statement('ALTER TABLE `karya` ADD CONSTRAINT `karya_id_pameran_foreign` FOREIGN KEY (`id_pameran`) REFERENCES `pameran` (`id_pameran`) ON DELETE SET NULL');
        $db->statement('ALTER TABLE `objek` ADD CONSTRAINT `objek_id_pameran_foreign` FOREIGN KEY (`id_pameran`) REFERENCES `pameran` (`id_pameran`) ON DELETE CASCADE');
        $db->statement('ALTER TABLE `ruangan` ADD CONSTRAINT `ruangan_id_pameran_foreign` FOREIGN KEY (`id_pameran`) REFERENCES `pameran` (`id_pameran`) ON DELETE CASCADE');
    }

    public function down(): void
    {
        // Konversi UUID kembali ke auto-increment integer tidak bersifat lossless.
    }
};
