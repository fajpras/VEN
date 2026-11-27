<?php

namespace App\Console\Commands;

use App\Mail\UndanganPengingatMail;
use App\Models\Undangan;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;

/**
 * Mengirim email pengingat H-1 untuk seluruh undangan diskusi privat yang disetujui
 * dan dijadwalkan berlangsung besok.
 */
class KirimPengingatUndanganCommand extends Command
{
    protected $signature = 'undangan:kirim-pengingat';
    protected $description = 'Kirim pengingat H-1 melalui email untuk jadwal diskusi privat besok.';

    public function handle(): int
    {
        $awalBesok = now()->addDay()->startOfDay();
        $akhirBesok = now()->addDay()->endOfDay();

        $daftar = Undangan::with(['ruangan.pameran.pengguna', 'pengguna'])
            ->where('status', 'disetujui')
            ->whereNull('pengingat_dikirim_pada')
            ->whereBetween('waktu', [$awalBesok, $akhirBesok])
            ->get();

        $terkirim = 0;

        foreach ($daftar as $undangan) {
            $deepLink = $undangan->tautan ? 'ven://diskusi/'.$undangan->tautan : null;
            $penerima = array_filter([
                $undangan->pengguna?->email => $undangan->pengguna?->nama ?? 'Pengguna',
                $undangan->ruangan?->pameran?->pengguna?->email => $undangan->ruangan?->pameran?->pengguna?->nama ?? 'Pemilik Pameran',
            ]);

            foreach ($penerima as $email => $nama) {
                try {
                    Mail::to($email)->send(new UndanganPengingatMail($undangan, $nama, $deepLink));
                } catch (\Throwable $e) {
                    Log::warning("Gagal kirim pengingat H-1 ke {$email}: ".$e->getMessage());
                }
            }

            $undangan->forceFill(['pengingat_dikirim_pada' => now()])->save();
            $terkirim++;
        }

        $this->info("Pengingat H-1 dikirim untuk {$terkirim} undangan.");

        return self::SUCCESS;
    }
}
