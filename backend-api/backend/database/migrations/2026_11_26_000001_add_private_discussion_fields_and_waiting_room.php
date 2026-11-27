<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('undangan', function (Blueprint $table) {
            if (! Schema::hasColumn('undangan', 'durasi_menit')) {
                $table->unsignedSmallInteger('durasi_menit')->default(30)->after('waktu');
            }
            if (! Schema::hasColumn('undangan', 'kadaluarsa_pada')) {
                $table->timestamp('kadaluarsa_pada')->nullable()->after('tautan');
            }
            if (! Schema::hasColumn('undangan', 'pengingat_dikirim_pada')) {
                $table->timestamp('pengingat_dikirim_pada')->nullable()->after('kadaluarsa_pada');
            }
        });

        if (! Schema::hasTable('permintaan_gabung_ruangan')) {
            Schema::create('permintaan_gabung_ruangan', function (Blueprint $table) {
                $table->bigIncrements('id_permintaan');
                $table->unsignedInteger('id_undangan');
                $table->unsignedInteger('id_ruangan');
                $table->char('id_pengguna', 36);
                $table->string('status', 20)->default('menunggu');
                $table->timestamps();

                $table->unique(['id_undangan', 'id_pengguna'], 'unik_undangan_pengguna_gabung');
                $table->index(['id_ruangan', 'status'], 'idx_ruangan_status_gabung');
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('permintaan_gabung_ruangan');

        Schema::table('undangan', function (Blueprint $table) {
            $cols = array_values(array_filter(
                ['durasi_menit', 'kadaluarsa_pada', 'pengingat_dikirim_pada'],
                fn (string $col) => Schema::hasColumn('undangan', $col),
            ));
            if ($cols !== []) {
                $table->dropColumn($cols);
            }
        });
    }
};
