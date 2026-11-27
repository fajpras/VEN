<?php

namespace App\Console\Commands;

use App\Models\Undangan;
use App\Services\Diskusi\JendelaSesi;
use Illuminate\Console\Command;

class KedaluwarsakanUndangan extends Command
{
    protected $signature = 'ven:kedaluwarsakan-undangan';

    protected $description = 'Tandai undangan diproses yang jadwalnya sudah lewat sebagai ditolak.';

    public function handle(): int
    {
        $batas = now()->subMinutes(JendelaSesi::MENIT_SESUDAH);
        $jumlah = 0;

        Undangan::query()
            ->where('status', 'diproses')
            ->where('waktu', '<', $batas)
            ->chunkById(100, function ($undangan) use (&$jumlah) {
                foreach ($undangan as $item) {
                    $item->forceFill([
                        'status' => 'ditolak',
                        'alasan' => 'Jadwal terlewat tanpa tanggapan.',
                    ])->save();
                    $jumlah++;
                }
            }, 'id_undangan');

        $this->info("{$jumlah} undangan kedaluwarsa ditandai.");

        return self::SUCCESS;
    }
}
