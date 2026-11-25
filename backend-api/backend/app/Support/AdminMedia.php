<?php

namespace App\Support;

class AdminMedia
{
    /**
     * Normalisasi path/URL media untuk panel admin agar tidak terjadi
     * double "/storage/", URL emulator "10.0.2.2" yang tidak bisa diakses browser,
     * maupun URL eksternal (Google OAuth) yang dibungkus "asset('storage/...')".
     */
    public static function imageUrl(?string $path): ?string
    {
        $path = trim((string) $path);
        if ($path === '') {
            return null;
        }

        if (preg_match('#^https?://#i', $path) === 1) {
            $parsed = parse_url($path);
            $host = strtolower((string) ($parsed['host'] ?? ''));
            $urlPath = (string) ($parsed['path'] ?? '');

            $isLocalHost = in_array($host, ['10.0.2.2', '127.0.0.1', 'localhost', 'ven.test'], true);
            if ($isLocalHost && str_contains($urlPath, '/storage/')) {
                $relative = ltrim(substr($urlPath, strpos($urlPath, '/storage/') + strlen('/storage/')), '/');

                return $relative !== '' ? asset('storage/'.$relative) : null;
            }

            return $path;
        }

        $clean = ltrim($path, '/');
        if (str_starts_with($clean, 'storage/')) {
            $clean = substr($clean, strlen('storage/'));
        }

        return $clean !== '' ? asset('storage/'.$clean) : null;
    }

    /**
     * Avatar SVG inline berbasis inisial (tidak bergantung layanan eksternal).
     */
    public static function avatarFallback(?string $name): string
    {
        $clean = trim((string) $name);
        $parts = preg_split('/\s+/', $clean, -1, PREG_SPLIT_NO_EMPTY) ?: [];

        if (count($parts) >= 2) {
            $initials = mb_strtoupper(mb_substr($parts[0], 0, 1).mb_substr($parts[1], 0, 1));
        } elseif ($clean !== '') {
            $initials = mb_strtoupper(mb_substr($clean, 0, 2));
        } else {
            $initials = 'VN';
        }

        $safe = htmlspecialchars($initials, ENT_XML1 | ENT_QUOTES, 'UTF-8');
        $svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80">'
            .'<rect width="80" height="80" rx="40" fill="#BA18F5"/>'
            .'<text x="50%" y="53%" dominant-baseline="middle" text-anchor="middle" '
            .'fill="#FFFFFF" font-family="Plus Jakarta Sans, system-ui, sans-serif" font-size="28" font-weight="700">'
            .$safe.'</text></svg>';

        return 'data:image/svg+xml,'.rawurlencode($svg);
    }

    /**
     * Placeholder banner pameran / thumbnail karya (SVG inline, bekerja offline).
     */
    public static function bannerFallback(): string
    {
        $svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 160">'
            .'<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">'
            .'<stop offset="0%" stop-color="#F3E8FF"/><stop offset="100%" stop-color="#E5E7EB"/>'
            .'</linearGradient></defs>'
            .'<rect width="320" height="160" fill="url(#g)"/>'
            .'<g fill="none" stroke="#BA18F5" stroke-opacity="0.45" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">'
            .'<rect x="134" y="54" width="52" height="40" rx="6"/>'
            .'<circle cx="150" cy="68" r="4"/>'
            .'<path d="M138 88l12-12 9 9 8-8 15 15"/>'
            .'</g></svg>';

        return 'data:image/svg+xml,'.rawurlencode($svg);
    }

    /**
     * Ubah string User-Agent mentah menjadi ringkasan klien yang mudah dibaca.
     */
    public static function parseUserAgent(?string $ua): string
    {
        $ua = trim((string) $ua);
        if ($ua === '') {
            return 'Tidak tercatat';
        }

        if (stripos($ua, 'okhttp') !== false) {
            return 'Android App (OkHttp)';
        }
        if (stripos($ua, 'CLI/Internal') !== false) {
            return 'Sistem / CLI';
        }
        if (stripos($ua, 'PHPUnit') !== false || stripos($ua, 'Symfony') !== false) {
            return 'Automated Test';
        }
        if (stripos($ua, 'Postman') !== false) {
            return 'Postman';
        }
        if (stripos($ua, 'curl') !== false) {
            return 'cURL';
        }

        $browser = match (true) {
            str_contains($ua, 'Edg/') => 'Edge',
            str_contains($ua, 'OPR/') || str_contains($ua, 'Opera') => 'Opera',
            str_contains($ua, 'Chrome/') => 'Chrome',
            str_contains($ua, 'Firefox/') => 'Firefox',
            str_contains($ua, 'Safari/') => 'Safari',
            default => null,
        };

        $os = match (true) {
            stripos($ua, 'Windows') !== false => 'Windows',
            stripos($ua, 'Android') !== false => 'Android',
            stripos($ua, 'iPhone') !== false || stripos($ua, 'iPad') !== false => 'iOS',
            stripos($ua, 'Macintosh') !== false || stripos($ua, 'Mac OS') !== false => 'macOS',
            stripos($ua, 'Linux') !== false => 'Linux',
            default => null,
        };

        if ($browser && $os) {
            return "{$browser} · {$os}";
        }

        return $browser ?? $os ?? mb_strimwidth($ua, 0, 28, '…');
    }
}
