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

    public static function berlaku(CarbonInterface $waktu): bool
    {
        $kini = now();

        return $waktu->betweenIncluded(
            $kini->copy()->subMinutes(self::MENIT_SEBELUM),
            $kini->copy()->addMinutes(self::MENIT_SESUDAH),
        );
    }

    public static function bentrok(CarbonInterface $a, CarbonInterface $b): bool
    {
        return $a->lt($b->copy()->addMinutes(self::DURASI_SLOT_MENIT))
            && $b->lt($a->copy()->addMinutes(self::DURASI_SLOT_MENIT));
    }
}
