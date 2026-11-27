<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Models\Pameran;
use App\Models\Ruangan;
use App\Services\Diskusi\PenjagaAksesRuangan;
use App\Services\Edgegap\LayananEdgegapServer;
use Illuminate\Http\Request;
use RuntimeException;

class RoomServerController extends Controller
{
    public function byRoomKey(Request $request, string $roomKey, LayananEdgegapServer $edgegap, PenjagaAksesRuangan $penjaga)
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

            $penjaga->pastikan($ruangan, $request->user());

            return $this->provision($request, 'ruangan-'.$ruangan->id_ruangan, $edgegap);
        }

        if (! \Illuminate\Support\Str::isUuid($roomKey)) {
            return $this->notFound('Pameran tidak ditemukan.');
        }

        $pameran = Pameran::find($roomKey);
        if (! $pameran) {
            return $this->notFound('Pameran tidak ditemukan.');
        }

        return $this->provision($request, (string) $pameran->id_pameran, $edgegap);
    }

    public function pameran(Request $request, Pameran $pameran, LayananEdgegapServer $edgegap)
    {
        return $this->provision($request, (string) $pameran->id_pameran, $edgegap);
    }

    public function ruangan(Request $request, Ruangan $ruangan, LayananEdgegapServer $edgegap, PenjagaAksesRuangan $penjaga)
    {
        $ruangan->loadMissing('pameran');
        abort_unless($ruangan->pameran, 404);

        $penjaga->pastikan($ruangan, $request->user());

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
}
