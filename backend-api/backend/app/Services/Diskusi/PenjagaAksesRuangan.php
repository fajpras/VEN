<?php

namespace App\Services\Diskusi;

use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;

final class PenjagaAksesRuangan
{
    public function pastikan(Ruangan $ruangan, Pengguna $pengguna): void
    {
        if ($pengguna->isAdmin()) {
            return;
        }

        $ruangan->loadMissing('pameran');

        $pemilik = $ruangan->pameran?->id_pengguna;
        $adalahPemilik = $pemilik && hash_equals((string) $pemilik, (string) $pengguna->id_pengguna);

        $kueri = Undangan::query()
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->where('status', 'disetujui')
            ->whereBetween('waktu', [JendelaSesi::mulai(), JendelaSesi::selesai()]);

        if (! $adalahPemilik) {
            $kueri->where('id_pengguna', $pengguna->id_pengguna);
        }

        abort_unless($kueri->exists(), 403, 'Anda tidak berhak mengakses ruangan ini.');
    }
}
