<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasColumn('undangan', 'alasan')) {
            Schema::table('undangan', function (Blueprint $table) {
                $table->string('alasan', 255)->nullable()->after('catatan');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasColumn('undangan', 'alasan')) {
            Schema::table('undangan', function (Blueprint $table) {
                $table->dropColumn('alasan');
            });
        }
    }
};
