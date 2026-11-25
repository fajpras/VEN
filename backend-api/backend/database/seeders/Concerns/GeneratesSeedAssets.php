<?php

namespace Database\Seeders\Concerns;

use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use RuntimeException;

trait GeneratesSeedAssets
{
    /** @var array<string, string> */
    private array $categoryPalette = [
        'Cybersecurity' => '#0F4C81',
        'Software' => '#1D4ED8',
        'UI/UX' => '#7C3AED',
        'Videography' => '#BE185D',
        'Photography' => '#EA580C',
        '3D' => '#9333EA',
        'Animation' => '#DB2777',
        'Internet Of Things' => '#047857',
        'Automation System' => '#0F766E',
        'Game Dev' => '#4338CA',
        'Fabrication' => '#B45309',
        'Manufacturing' => '#334155',
        '2D' => '#0891B2',
        'Others' => '#6D28D9',
    ];

    private int $generatedAssetCount = 0;

    private int $reusedAssetCount = 0;

    protected function resetAssetCounters(): void
    {
        $this->generatedAssetCount = 0;
        $this->reusedAssetCount = 0;
    }

    /**
     * @return array{generated: int, reused: int}
     */
    protected function getAssetCounters(): array
    {
        return [
            'generated' => $this->generatedAssetCount,
            'reused' => $this->reusedAssetCount,
        ];
    }

    protected function ensureBannerPng(string $judul, string $warnaHex, string $tipe): string
    {
        $filename = $this->stableAssetName($judul, 'png');
        $relativePath = 'pameran/banner/'.$filename;
        $disk = Storage::disk('public');

        if ($disk->exists($relativePath) && $this->detectMime($disk->path($relativePath)) === 'image/png') {
            $this->reusedAssetCount++;

            return $relativePath;
        }

        $pngBinary = $this->renderArtworkPng(
            width: 1280,
            height: 720,
            title: $judul,
            badge: 'PAMERAN • '.strtoupper($tipe),
            footer: 'VEN VIRTUAL EXHIBITION',
            hexColor: $warnaHex,
            seedKey: 'banner:'.$judul,
        );

        $disk->put($relativePath, $pngBinary);
        $this->assertValidMime($disk->path($relativePath), 'image/png');
        $this->generatedAssetCount++;

        return $relativePath;
    }

    protected function ensurePosterPng(string $judul, string $kategori, ?string $warnaHex = null): string
    {
        $filename = $this->stableAssetName($judul, 'png');
        $relativePath = 'karya/'.$filename;
        $disk = Storage::disk('local');

        if ($disk->exists($relativePath) && $this->detectMime($disk->path($relativePath)) === 'image/png') {
            $this->reusedAssetCount++;

            return $relativePath;
        }

        $color = $warnaHex ?? ($this->categoryPalette[$kategori] ?? '#6D28D9');
        $pngBinary = $this->renderArtworkPng(
            width: 800,
            height: 1000,
            title: $judul,
            badge: strtoupper($kategori),
            footer: 'VEN DIGITAL ARCHIVE',
            hexColor: $color,
            seedKey: 'poster:'.$judul,
        );

        $disk->put($relativePath, $pngBinary);
        $this->assertValidMime($disk->path($relativePath), 'image/png');
        $this->generatedAssetCount++;

        return $relativePath;
    }

    protected function ensureVideoMp4(string $judul, int $variantIndex = 0): string
    {
        $sourceFiles = $this->discoverSourceMp4Files();
        if (empty($sourceFiles)) {
            throw new RuntimeException(
                'Tidak ditemukan aset MP4 valid di database/seeders/assets/video/. '
                .'Tambahkan minimal satu file .mp4 valid sebelum menjalankan seeder.'
            );
        }

        $sourcePath = $sourceFiles[$variantIndex % count($sourceFiles)];
        $this->assertValidMime($sourcePath, 'video/mp4');

        $filename = $this->stableAssetName($judul, 'mp4');
        $relativePath = 'karya/'.$filename;
        $disk = Storage::disk('local');

        if ($disk->exists($relativePath) && $this->detectMime($disk->path($relativePath)) === 'video/mp4') {
            $this->reusedAssetCount++;

            return $relativePath;
        }

        $bytes = file_get_contents($sourcePath);
        if ($bytes === false || $bytes === '') {
            throw new RuntimeException("Gagal membaca file sumber MP4: {$sourcePath}");
        }

        $disk->put($relativePath, $bytes);
        $this->assertValidMime($disk->path($relativePath), 'video/mp4');
        $this->generatedAssetCount++;

        return $relativePath;
    }

    /**
     * @return list<string>
     */
    private function discoverSourceMp4Files(): array
    {
        $videoDir = database_path('seeders/assets/video');
        if (! is_dir($videoDir)) {
            return [];
        }

        $files = glob($videoDir.DIRECTORY_SEPARATOR.'*.mp4') ?: [];
        sort($files);

        return array_values(array_filter(
            $files,
            fn (string $path): bool => is_file($path) && $this->detectMime($path) === 'video/mp4'
        ));
    }

    private function stableAssetName(string $judul, string $extension): string
    {
        $slug = Str::slug(Str::limit($judul, 48, ''));
        $slug = $slug !== '' ? $slug : 'asset';
        $hash = substr(sha1($judul), 0, 8);

        return "{$slug}-{$hash}.{$extension}";
    }

    private function renderArtworkPng(
        int $width,
        int $height,
        string $title,
        string $badge,
        string $footer,
        string $hexColor,
        string $seedKey,
    ): string {
        if (! extension_loaded('gd')) {
            throw new RuntimeException('Ekstensi PHP GD wajib aktif untuk menghasilkan aset PNG seeder.');
        }

        $image = imagecreatetruecolor($width, $height);
        if ($image === false) {
            throw new RuntimeException('Gagal membuat kanvas GD.');
        }

        imagealphablending($image, true);
        imagesavealpha($image, true);

        [$baseR, $baseG, $baseB] = $this->hexToRgb($hexColor);
        $darkR = (int) max(12, $baseR * 0.24);
        $darkG = (int) max(12, $baseG * 0.24);
        $darkB = (int) max(18, $baseB * 0.30);

        $accentR = (int) min(255, $baseR + 55);
        $accentG = (int) min(255, $baseG + 35);
        $accentB = (int) min(255, $baseB + 65);

        // Gradasi vertikal halus
        for ($y = 0; $y < $height; $y++) {
            $ratio = $y / max(1, $height - 1);
            $r = (int) round($baseR * (1 - $ratio) + $darkR * $ratio);
            $g = (int) round($baseG * (1 - $ratio) + $darkG * $ratio);
            $b = (int) round($baseB * (1 - $ratio) + $darkB * $ratio);
            $lineColor = imagecolorallocate($image, $r, $g, $b);
            imageline($image, 0, $y, $width, $y, $lineColor);
        }

        // Pola grid tipis
        $gridColor = imagecolorallocatealpha($image, 255, 255, 255, 118);
        $step = (int) max(48, round($width / 16));
        for ($x = $step; $x < $width; $x += $step) {
            imageline($image, $x, 0, $x, $height, $gridColor);
        }
        for ($y = $step; $y < $height; $y += $step) {
            imageline($image, 0, $y, $width, $y, $gridColor);
        }

        // Dekorasi geometris deterministik dari hash
        $hashInt = crc32($seedKey);
        $orb1Color = imagecolorallocatealpha($image, $accentR, $accentG, $accentB, 92);
        $orb2Color = imagecolorallocatealpha($image, 255, 255, 255, 108);
        $ringColor = imagecolorallocatealpha($image, 255, 255, 255, 96);

        $cx1 = (int) ($width * (0.68 + (($hashInt % 20) / 100)));
        $cy1 = (int) ($height * (0.20 + ((($hashInt >> 4) % 20) / 100)));
        $rad1 = (int) (min($width, $height) * 0.52);
        imagefilledellipse($image, $cx1, $cy1, $rad1, $rad1, $orb1Color);

        $cx2 = (int) ($width * (0.14 + ((($hashInt >> 8) % 15) / 100)));
        $cy2 = (int) ($height * (0.78 + ((($hashInt >> 12) % 12) / 100)));
        $rad2 = (int) (min($width, $height) * 0.36);
        imagefilledellipse($image, $cx2, $cy2, $rad2, $rad2, $orb2Color);

        imagesetthickness($image, 2);
        imageellipse($image, $cx1, $cy1, (int) ($rad1 * 1.18), (int) ($rad1 * 1.18), $ringColor);
        imagesetthickness($image, 1);

        // Panel kartu transparan di bawah/tengah untuk keterbacaan teks
        $padX = (int) round($width * 0.08);
        $padY = (int) round($height * 0.10);
        $cardFill = imagecolorallocatealpha($image, 10, 12, 22, 68);
        $cardBorder = imagecolorallocatealpha($image, 255, 255, 255, 95);
        imagefilledrectangle($image, $padX, $padY, $width - $padX, $height - $padY, $cardFill);
        imagerectangle($image, $padX, $padY, $width - $padX, $height - $padY, $cardBorder);

        $white = imagecolorallocate($image, 255, 255, 255);
        $softWhite = imagecolorallocate($image, 226, 232, 240);
        $badgeBg = imagecolorallocatealpha($image, 255, 255, 255, 98);

        $innerLeft = $padX + (int) round($width * 0.05);
        $innerRight = $width - $padX - (int) round($width * 0.05);
        $maxTextWidth = max(200, $innerRight - $innerLeft);

        $fontBold = $this->resolveFontPath(bold: true);
        $fontRegular = $this->resolveFontPath(bold: false);

        if ($fontBold !== null) {
            $badgeSize = (int) round($width * 0.018);
            $badgeBox = imagettfbbox($badgeSize, 0, $fontBold, $badge);
            $badgeW = $badgeBox ? abs($badgeBox[2] - $badgeBox[0]) : 160;
            $badgeH = $badgeBox ? abs($badgeBox[7] - $badgeBox[1]) : 22;

            $badgeTop = $padY + (int) round($height * 0.07);
            imagefilledrectangle(
                $image,
                $innerLeft,
                $badgeTop,
                $innerLeft + $badgeW + 32,
                $badgeTop + $badgeH + 20,
                $badgeBg
            );
            imagettftext($image, $badgeSize, 0, $innerLeft + 16, $badgeTop + $badgeH + 9, $white, $fontBold, $badge);

            $titleSize = $width >= 1000 ? 44 : 38;
            $lines = $this->wrapTtfText($title, $fontBold, $titleSize, $maxTextWidth);
            $lineHeight = (int) round($titleSize * 1.42);
            $startY = (int) round($height * 0.44 - ((count($lines) - 1) * $lineHeight * 0.35));

            foreach ($lines as $idx => $line) {
                imagettftext($image, $titleSize, 0, $innerLeft, $startY + ($idx * $lineHeight), $white, $fontBold, $line);
            }

            $footerSize = (int) round($width * 0.015);
            $footerFont = $fontRegular ?? $fontBold;
            $footerY = $height - $padY - (int) round($height * 0.06);
            imagettftext($image, $footerSize, 0, $innerLeft, $footerY, $softWhite, $footerFont, $footer);
        } else {
            // Fallback aman bila font TrueType tidak tersedia di OS
            imagestring($image, 5, $innerLeft, $padY + 36, $badge, $softWhite);
            $wrapped = explode("\n", wordwrap($title, 28, "\n", true));
            foreach ($wrapped as $idx => $line) {
                imagestring($image, 5, $innerLeft, (int) ($height * 0.40) + ($idx * 24), $line, $white);
            }
            imagestring($image, 3, $innerLeft, $height - $padY - 40, $footer, $softWhite);
        }

        ob_start();
        imagepng($image, null, 6);
        $binary = ob_get_clean();
        imagedestroy($image);

        if (! is_string($binary) || $binary === '') {
            throw new RuntimeException('Gagal mengodekan gambar PNG.');
        }

        return $binary;
    }

    /**
     * @return list<string>
     */
    private function wrapTtfText(string $text, string $fontPath, int $fontSize, int $maxWidth): array
    {
        $words = preg_split('/\s+/', trim($text)) ?: [];
        $lines = [];
        $current = '';

        foreach ($words as $word) {
            $candidate = $current === '' ? $word : $current.' '.$word;
            $box = imagettfbbox($fontSize, 0, $fontPath, $candidate);
            $width = $box ? abs($box[2] - $box[0]) : 0;

            if ($width <= $maxWidth || $current === '') {
                $current = $candidate;
            } else {
                $lines[] = $current;
                $current = $word;
            }
        }

        if ($current !== '') {
            $lines[] = $current;
        }

        return array_slice($lines, 0, 5);
    }

    private function resolveFontPath(bool $bold): ?string
    {
        $candidates = $bold
            ? [
                'C:\Windows\Fonts\segoeuib.ttf',
                'C:\Windows\Fonts\arialbd.ttf',
                '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
                '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf',
            ]
            : [
                'C:\Windows\Fonts\segoeui.ttf',
                'C:\Windows\Fonts\arial.ttf',
                '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',
                '/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf',
            ];

        foreach ($candidates as $path) {
            if (is_file($path)) {
                return $path;
            }
        }

        return null;
    }

    /**
     * @return array{0: int, 1: int, 2: int}
     */
    private function hexToRgb(string $hex): array
    {
        $clean = ltrim(trim($hex), '#');
        if (strlen($clean) !== 6 || ! ctype_xdigit($clean)) {
            return [109, 40, 217];
        }

        return [
            hexdec(substr($clean, 0, 2)),
            hexdec(substr($clean, 2, 2)),
            hexdec(substr($clean, 4, 2)),
        ];
    }

    private function detectMime(string $absolutePath): string
    {
        if (! is_file($absolutePath)) {
            return '';
        }

        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        if ($finfo === false) {
            return '';
        }

        $mime = (string) finfo_file($finfo, $absolutePath);
        finfo_close($finfo);

        return $mime;
    }

    private function assertValidMime(string $absolutePath, string $expectedMime): void
    {
        $actual = $this->detectMime($absolutePath);
        if ($actual !== $expectedMime) {
            throw new RuntimeException(
                "MIME file {$absolutePath} tidak sesuai: diharapkan {$expectedMime}, terbaca '{$actual}'."
            );
        }
    }
}
