<?php

namespace App\Support;

use Illuminate\Http\Request;

/**
 * Opsi tampilan daftar pada panel admin (mode Card/List dan jumlah item per halaman).
 *
 * Nilai yang diterima dibatasi oleh allowlist agar permintaan per_page tidak
 * bisa diset sembarangan (mis. 999999 yang membebani database).
 */
final class AdminListOptions
{
    /** @var array<int, int> */
    public const PER_PAGE_OPTIONS = [12, 25, 50, 100, 200];

    /** @var array<int, string> */
    public const VIEW_MODES = ['card', 'list'];

    public static function perPage(Request $request, int $default = 12): int
    {
        $value = (int) $request->query('per_page', $default);

        return in_array($value, self::PER_PAGE_OPTIONS, true) ? $value : $default;
    }

    public static function viewMode(Request $request, string $default = 'list'): string
    {
        $value = (string) $request->query('view', $default);

        return in_array($value, self::VIEW_MODES, true) ? $value : $default;
    }
}
