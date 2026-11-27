<?php

namespace App\Services\Diskusi;

use Carbon\CarbonInterface;

final class JendelaSesi
{
    public const MENIT_SEBELUM = 15;
    public const MENIT_SESUDAH = 15;
    public const DURASI_SLOT_MENIT = 60;

    public static function mulai(): CarbonInterface
    {
        return now()->subMinutes(self::MENIT_SEBELUM);
    }

    public static function selesai(): CarbonInterface
    {
        return now()->addMinutes(self::MENIT_SESUDAH);
    }

    public static function berlaku(CarbonInterface $waktu, ?int $durasiMenit = null): bool
    {
        $kini = now();
        $menitSesudah = max(self::MENIT_SESUDAH, (int) ($durasiMenit ?? 0));

        return $kini->betweenIncluded(
            $waktu->copy()->subMinutes(self::MENIT_SEBELUM),
            $waktu->copy()->addMinutes($menitSesudah),
        );
    }

    public static function bentrok(
        CarbonInterface $a,
        CarbonInterface $b,
        int $durasiA = self::DURASI_SLOT_MENIT,
        int $durasiB = self::DURASI_SLOT_MENIT,
    ): bool {
        return $a->lt($b->copy()->addMinutes(max(15, $durasiB)))
            && $b->lt($a->copy()->addMinutes(max(15, $durasiA)));
    }
}
