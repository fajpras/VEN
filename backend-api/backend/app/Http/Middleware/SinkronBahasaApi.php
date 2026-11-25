<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Menyelaraskan locale Laravel dengan header Accept-Language dari klien (Android/web).
 * Default tetap 'id' bila header tidak dikirim agar kompatibel penuh dengan klien lama.
 */
class SinkronBahasaApi
{
    public function handle(Request $request, Closure $next): Response
    {
        self::terapkanDariRequest($request);

        return $next($request);
    }

    public static function terapkanDariRequest(Request $request): void
    {
        $eksplisit = strtolower(trim((string) $request->header('X-App-Language', '')));
        $header = $eksplisit !== ''
            ? $eksplisit
            : strtolower(trim((string) $request->header('Accept-Language', '')));

        // Abaikan header bawaan sintetis Symfony Request::create() saat tidak ada header bahasa eksplisit
        if ($header === '' || ($eksplisit === '' && $header === 'en-us,en;q=0.5')) {
            app()->setLocale('id');

            return;
        }

        // Ambil bahasa prioritas pertama (mis. "en-US,en;q=0.9" -> "en")
        $utama = explode(',', $header)[0] ?? '';
        $utama = trim(explode(';', $utama)[0] ?? '');

        app()->setLocale(str_starts_with($utama, 'en') ? 'en' : 'id');
    }
}
