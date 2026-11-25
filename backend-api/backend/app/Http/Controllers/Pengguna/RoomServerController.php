<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Models\Pameran;
use App\Models\Ruangan;
use App\Models\Undangan;
use App\Services\Edgegap\LayananEdgegapServer;
use Illuminate\Http\Request;
use RuntimeException;

class RoomServerController extends Controller
{
    public function byRoomKey(Request $request, string $roomKey, LayananEdgegapServer $edgegap)
    {
        if (str_starts_with($roomKey, 'ruangan-')) {
            $idRuangan = substr($roomKey, strlen('ruangan-'));
            if (! ctype_digit($idRuangan)) {
                return $this->notFound('Ruangan tidak ditemukan.');
            }

            $ruangan = Ruangan::with('pameran')->find((int) $idRuangan);
            if (! $ruangan || ! $ruangan->pameran) {
                return $this->notFound('Ruangan tidak ditemukan.');
            }

            $this->pastikanAksesRuangan($request, $ruangan);

            return $this->provision($request, 'ruangan-'.$ruangan->id_ruangan, $edgegap);
        }

        if (! ctype_digit($roomKey)) {
            return $this->notFound('Pameran tidak ditemukan.');
        }

        $pameran = Pameran::find((int) $roomKey);
        if (! $pameran) {
            return $this->notFound('Pameran tidak ditemukan.');
        }

        return $this->provision($request, (string) $pameran->id_pameran, $edgegap);
    }

    public function pameran(Request $request, Pameran $pameran, LayananEdgegapServer $edgegap)
    {
        return $this->provision($request, (string) $pameran->id_pameran, $edgegap);
    }

    public function ruangan(Request $request, Ruangan $ruangan, LayananEdgegapServer $edgegap)
    {
        $ruangan->loadMissing('pameran');
        abort_unless($ruangan->pameran, 404);

        $this->pastikanAksesRuangan($request, $ruangan);

        return $this->provision($request, 'ruangan-'.$ruangan->id_ruangan, $edgegap);
    }

    private function provision(Request $request, string $roomId, LayananEdgegapServer $edgegap)
    {
        if (! $edgegap->terkonfigurasi()) {
            return $this->error('Layanan server multiplayer belum dikonfigurasi.', 503);
        }

        try {
            $payload = $edgegap->sediakanServer($roomId, $request->ip());

            return $this->ok($payload, $payload['ready'] ? 'Server ruangan siap.' : 'Server ruangan sedang disiapkan.');
        } catch (RuntimeException $e) {
            return $this->error($e->getMessage(), 502);
        }
    }

    private function pastikanAksesRuangan(Request $request, Ruangan $ruangan): void
    {
        $user = $request->user();

        $peserta = Undangan::query()
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->where('id_pengguna', $user->id_pengguna)
            ->where('status', 'disetujui')
            ->exists();

        $boleh = $peserta
            || hash_equals((string) $ruangan->pameran->id_pengguna, (string) $user->id_pengguna)
            || $user->isAdmin();

        abort_unless($boleh, 403, 'Anda tidak berhak mengakses ruangan ini.');
    }
}
