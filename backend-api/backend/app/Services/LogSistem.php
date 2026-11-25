<?php

namespace App\Services;

use App\Models\LogAktivitas;
use App\Models\LogKeamanan;
use App\Models\Pengguna;
use Throwable;

class LogSistem
{
    private static function koneksi(): string
    {
        return config('database.log_connection', 'mysql');
    }

    public static function aktivitas(
        ?string $entitas = null,
        ?string $jenis = null,
        string $pesan = '',
        string $status = 'berhasil',
        ?string $idPengguna = null,
        ?string $aksi = null
    ): void {
        try {
            $idAktor = $idPengguna ?? self::aktorId();
            $namaAktor = $entitas ?: self::namaAktor($idAktor);
            $keterangan = self::formatPesanAktivitas($jenis, $aksi, $pesan, $status);

            LogAktivitas::on(self::koneksi())->create([
                'id_pengguna' => $idAktor,
                'entitas'     => mb_substr($namaAktor, 0, 100),
                'jenis'       => $jenis ? mb_substr($jenis, 0, 50) : null,
                'aksi'        => $aksi ? mb_substr($aksi, 0, 20) : null,
                'status'      => $status,
                'pesan'       => mb_substr($keterangan, 0, 255),
                'ip_address'  => request()?->ip() ?? '127.0.0.1',
            ]);
        } catch (Throwable $e) {
            report($e);
        }
    }

    /**
     * Memformat pesan log aktivitas sesuai prinsip Audit Trail & Integritas Data.
     */
    private static function formatPesanAktivitas(?string $jenis, ?string $aksi, string $pesan, string $status): string
    {
        $teks = trim($pesan);
        if ($teks !== '' && str_starts_with($teks, '[')) {
            return $teks;
        }

        $aksiLower = strtolower(trim((string) $aksi));
        $jenisLower = strtolower(trim((string) $jenis));
        $isFailed = strtolower(trim($status)) !== 'berhasil';

        [$severity, $control] = $jenisLower === 'sanksi'
            ? ['HIGH', 'ACCESS_CONTROL']
            : match ($aksiLower) {
                'hapus'      => ['HIGH', 'DATA_MODERATION'],
                'pulih'      => ['MEDIUM', 'DATA_RECOVERY'],
                'sanksi', 'blokir', 'tangguh' => ['HIGH', 'ACCESS_CONTROL'],
                'registrasi' => ['INFO', 'IDENTITY_PROVISIONING'],
                'tambah'     => ['INFO', 'DATA_INTEGRITY'],
                'ubah'       => [$jenisLower === 'profil' || $jenisLower === 'pengguna' ? 'MEDIUM' : 'INFO', 'CHANGE_AUDIT'],
                'lihat'      => ['INFO', 'READ_AUDIT'],
                default      => ['INFO', 'ACTIVITY_AUDIT'],
            };

        if ($isFailed && $severity === 'INFO') {
            $severity = 'WARN';
        }

        $isi = $teks !== ''
            ? $teks
            : "Aktivitas {$aksiLower} pada domain ".($jenisLower ?: 'sistem')." tercatat ({$status}).";

        return "[{$severity} | {$control}] {$isi}";
    }

    public static function keamanan(
        string $event,
        ?string $pesan = null,
        ?int $statusKode = null,
        ?string $idPengguna = null
    ): void {
        try {
            $req = request();
            $status = $statusKode ?? 200;
            $keterangan = self::formatPesanKeamanan($event, $pesan, $status);

            LogKeamanan::on(self::koneksi())->create([
                'id_pengguna' => $idPengguna ?? self::aktorId(),
                'event'       => $event,
                'endpoint'    => mb_substr('/'.ltrim((string) $req?->path(), '/'), 0, 100),
                'method'      => $req?->method() ?? 'CLI',
                'status_kode' => $status,
                'ip_address'  => $req?->ip() ?? '127.0.0.1',
                'user_agent'  => mb_substr((string) ($req?->userAgent() ?: 'CLI/Internal'), 0, 255),
                'pesan'       => mb_substr($keterangan, 0, 255),
            ]);
        } catch (Throwable $e) {
            report($e);
        }
    }

    public static function lihat(string $jenis, string $namaAtauId, ?string $detail = null): void
    {
        self::aktivitas(
            entitas: null,
            jenis: $jenis,
            pesan: "Melihat {$jenis} \"{$namaAtauId}\"".($detail ? " ({$detail})" : ''),
            status: 'berhasil',
            aksi: 'lihat',
        );
    }

    /**
     * Memformat pesan audit keamanan sesuai prinsip Keamanan Basis Data
     * (Severity + Kontrol Keamanan: Access Control, Authentication, Least Privilege,
     * Credential Integrity, Availability, Non-Repudiation).
     */
    private static function formatPesanKeamanan(string $event, ?string $pesan, int $status): string
    {
        $teks = trim((string) $pesan);
        if ($teks !== '' && str_starts_with($teks, '[')) {
            return $teks;
        }

        [$severity, $control] = self::klasifikasiAuditKeamanan($event, $status);
        $isi = $teks !== '' ? $teks : self::defaultPesanKeamanan($event, $status);

        return "[{$severity} | {$control}] {$isi}";
    }

    /**
     * @return array{0: string, 1: string}
     */
    private static function klasifikasiAuditKeamanan(string $event, int $status): array
    {
        return match ($event) {
            'login_berhasil'            => ['INFO', 'AUTHENTICATION'],
            'login_gagal'               => ['WARN', 'AUTHENTICATION'],
            'login_ditolak'             => ['HIGH', 'ACCESS_CONTROL'],
            'logout'                    => ['INFO', 'SESSION_AUDIT'],
            'reset_kata_sandi'          => ['MEDIUM', 'CREDENTIAL_INTEGRITY'],
            'google_token_tidak_valid'  => ['WARN', 'FEDERATED_AUTH'],
            'tidak_terautentikasi'      => ['WARN', 'ACCESS_CONTROL'],
            'akses_ditolak'             => ['HIGH', 'LEAST_PRIVILEGE'],
            'terlalu_banyak_permintaan' => ['HIGH', 'AVAILABILITY_THROTTLE'],
            'otp_terbit'                => ['INFO', 'MFA_OTP'],
            'otp_terverifikasi'         => ['INFO', 'MFA_OTP'],
            'otp_gagal'                 => ['WARN', 'MFA_OTP'],
            default                     => [$status >= 400 ? 'WARN' : 'INFO', 'AUDIT_TRAIL'],
        };
    }

    private static function defaultPesanKeamanan(string $event, int $status): string
    {
        return match ($event) {
            'login_berhasil'            => 'Sesi otentikasi berhasil diverifikasi sesuai prinsip kontrol akses berbasis peran.',
            'login_gagal'               => 'Verifikasi kredensial gagal karena identitas atau kata sandi tidak cocok.',
            'login_ditolak'             => 'Otentikasi ditolak oleh kontrol akses akun karena status sanksi aktif.',
            'logout'                    => 'Sesi aktif diakhiri secara sah dan token otentikasi dicabut.',
            'reset_kata_sandi'          => 'Pembaruan kredensial kata sandi akun berhasil diverifikasi.',
            'google_token_tidak_valid'  => 'Verifikasi token OAuth Google gagal; integritas identitas eksternal tidak valid.',
            'tidak_terautentikasi'      => 'Akses ke sumber daya terproteksi ditolak: token otentikasi tidak valid atau kedaluwarsa.',
            'akses_ditolak'             => 'Pelanggaran otorisasi (RBAC): aktor tidak memiliki hak akses ke sumber daya ini.',
            'terlalu_banyak_permintaan' => 'Pembatasan laju (rate limit) diaktifkan untuk menjaga ketersediaan layanan & basis data.',
            'otp_terbit'                => 'Kode verifikasi OTP sekali pakai berhasil diterbitkan.',
            'otp_terverifikasi'         => 'Kode verifikasi OTP berhasil divalidasi dengan benar.',
            'otp_gagal'                 => 'Validasi kode OTP gagal atau telah melampaui batas percobaan.',
            default                     => "Peristiwa audit keamanan {$event} tercatat (HTTP {$status}).",
        };
    }

    private static function aktorId(): ?string
    {
        try {
            return request()?->user('sanctum')?->id_pengguna ?? auth()->id();
        } catch (Throwable) {
            return null;
        }
    }

    private static function namaAktor(?string $idAktor): string
    {
        try {
            if ($user = (request()?->user('sanctum') ?? auth()->user())) {
                return (string) $user->nama;
            }

            if ($idAktor && ($pengguna = Pengguna::find($idAktor))) {
                return (string) $pengguna->nama;
            }
        } catch (Throwable) {
        }

        return request() ? 'Tamu' : 'Sistem';
    }
}
