<?php

namespace App\Services\Livekit;

use App\Exceptions\KesalahanVoice;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;

/**
 * Penyedia sesi voice chat LiveKit untuk ruangan privat.
 *
 * Voice chat hanya tersedia di ruangan privat: peserta wajib memiliki
 * undangan diskusi yang disetujui dan berada di dalam jendela sesi
 * (mulai 3 jam sebelum jadwal hingga 15 menit setelah jadwal).
 */
final class LayananTokenVoice
{
    private const MASA_BERLAKU_TOKEN_DETIK = 3600;
    private const JENDELA_SESI_MULAI_JAM = 3;
    private const JENDELA_SESI_LEBIH_MENIT = 15;

    public function __construct(
        private readonly ?string $url,
        private readonly ?string $kunci,
        private readonly ?string $rahasia,
    ) {}

    /** True bila kredensial LiveKit server sudah diisi di konfigurasi. */
    public function terkonfigurasi(): bool
    {
        return filled($this->kunci) && filled($this->rahasia);
    }

    /**
     * Susun sesi diskusi privat (nama room LiveKit + token akses) untuk
     * seorang pengguna pada sebuah ruangan.
     *
     * @return array{url: ?string, room: string, token: string}
     */
    public function sesiDiskusi(Ruangan $ruangan, Pengguna $pengguna): array
    {
        if ($ruangan->jenis !== 'privat') {
            throw KesalahanVoice::hanyaPrivat();
        }

        $undangan = $this->undanganAktif($ruangan, $pengguna);
        $namaRoom = sprintf('diskusi-%d-%d', $ruangan->id_ruangan, $undangan->id_undangan);

        return [
            'url' => $this->url,
            'room' => $namaRoom,
            'token' => $this->token($namaRoom, $pengguna),
        ];
    }

    private function undanganAktif(Ruangan $ruangan, Pengguna $pengguna): Undangan
    {
        $ruangan->loadMissing('pameran');

        $penyelenggara = hash_equals((string) $ruangan->pameran?->id_pengguna, (string) $pengguna->id_pengguna);

        $kueri = Undangan::query()
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->where('status', 'disetujui')
            ->whereBetween('waktu', [
                now()->subHours(self::JENDELA_SESI_MULAI_JAM),
                now()->addMinutes(self::JENDELA_SESI_LEBIH_MENIT),
            ])
            ->latest('waktu');

        // Penyelenggara boleh bergabung pada sesi mana pun di ruangannya;
        // pengundang hanya pada sesi undangannya sendiri.
        if (! $penyelenggara) {
            $kueri->where('id_pengguna', $pengguna->id_pengguna);
        }

        $undangan = $kueri->first();

        if (! $undangan) {
            throw KesalahanVoice::tanpaSesiAktif();
        }

        return $undangan;
    }

    private function token(string $namaRoom, Pengguna $pengguna): string
    {
        $kini = time();

        return $this->jwt([
            'iss' => $this->kunci,
            'sub' => $pengguna->id_pengguna,
            'name' => $pengguna->nama_panggilan ?: $pengguna->nama,
            'nbf' => $kini,
            'exp' => $kini + self::MASA_BERLAKU_TOKEN_DETIK,
            'video' => [
                'room' => $namaRoom,
                'roomJoin' => true,
                'canPublish' => true,
                'canSubscribe' => true,
                'canPublishData' => false,
            ],
        ], $this->rahasia);
    }

    /** JWT HS256 dengan format klaim akses LiveKit, ditandatangani API secret. */
    private function jwt(array $claims, string $rahasia): string
    {
        $b64 = fn (string $s) => rtrim(strtr(base64_encode($s), '+/', '-_'), '=');
        $head = $b64(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $body = $b64(json_encode($claims));

        return "$head.$body.".$b64(hash_hmac('sha256', "$head.$body", $rahasia, true));
    }
}
