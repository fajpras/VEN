<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Models\Karya;
use App\Models\Pameran;
use App\Models\Ruangan;
use App\Models\Undangan;
use App\Services\Pameran\PenyusunRoomDto;
use App\Services\Livekit\LayananTokenVoice;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\Rule;

class DiskusiController extends Controller
{
    public function index(Request $request)
    {
        $request->validate([
            'status' => ['nullable', Rule::in(['diproses', 'disetujui', 'ditolak'])],
            'sebagai' => ['nullable', Rule::in(['pengundang', 'penyelenggara'])],
        ]);

        $id = $request->user()->id_pengguna;

        $undangan = Undangan::with([
            'ruangan.pameran:id_pameran,judul,id_pengguna,banner',
            'pengguna:id_pengguna,nama,nama_panggilan,foto_profil',
        ])
            ->when(
                $request->query('sebagai') === 'penyelenggara',
                fn ($q) => $q->whereHas('ruangan.pameran', fn ($p) => $p->where('id_pengguna', $id)),
                fn ($q) => $q->where('id_pengguna', $id),
            )
            ->when($request->filled('status'), fn ($q) => $q->where('status', $request->query('status')))
            ->latest('id_undangan')
            ->paginate(24);

        return $this->ok($undangan);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'id_ruangan' => ['nullable', 'integer', Rule::exists('ruangan', 'id_ruangan')->where('jenis', 'privat')],
            'id_pameran' => ['nullable', 'integer', 'exists:pameran,id_pameran'],
            'id_karya'   => ['nullable', 'string', 'exists:karya,id_karya'],
            'waktu'      => ['required', 'date', 'after:now'],
            'catatan'    => ['nullable', 'string', 'max:255'],
        ]);

        if (empty($validated['id_ruangan']) && empty($validated['id_pameran']) && empty($validated['id_karya'])) {
            return $this->error('Target pameran atau ruangan harus disertakan.', 422);
        }

        if (!empty($validated['id_ruangan'])) {
            $ruangan = Ruangan::with('pameran')->findOrFail($validated['id_ruangan']);
        } elseif (!empty($validated['id_pameran'])) {
            $pameran = Pameran::findOrFail($validated['id_pameran']);
            $ruangan = Ruangan::firstOrCreate(
                ['id_pameran' => $pameran->id_pameran, 'jenis' => 'privat']
            );
            $ruangan->setRelation('pameran', $pameran);
        } else {
            $karya = Karya::with('pameran')->findOrFail($validated['id_karya']);
            $pameran = $karya->pameran ?: Pameran::where('id_pengguna', $karya->id_pengguna)->latest('id_pameran')->first();
            if (! $pameran) {
                return $this->notFound('Pameran terkait karya tidak tersedia.');
            }
            $ruangan = Ruangan::firstOrCreate(
                ['id_pameran' => $pameran->id_pameran, 'jenis' => 'privat']
            );
            $ruangan->setRelation('pameran', $pameran);
        }

        $user = $request->user();

        if (! $ruangan->pameran) {
            return $this->notFound('Pameran tidak tersedia.');
        }

        if (hash_equals($ruangan->pameran->id_pengguna, $user->id_pengguna)) {
            return $this->error('Anda tidak perlu mengundang diri sendiri ke pameran Anda.', 422);
        }

        $sudahAda = Undangan::where('id_pengguna', $user->id_pengguna)
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->whereIn('status', ['diproses', 'disetujui'])
            ->exists();

        if ($sudahAda) {
            return $this->error('Anda sudah memiliki undangan aktif untuk ruangan ini.', 409);
        }

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $user->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => $validated['waktu'],
            'catatan' => $validated['catatan'] ?? null,
            'status' => 'diproses',
        ]);

        return $this->created(
            $undangan->load(['ruangan.pameran:id_pameran,judul,id_pengguna,banner', 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil']),
            'Undangan diskusi diajukan.'
        );
    }

    public function show(Request $request, Undangan $undangan)
    {
        $this->pastikanPeserta($request, $undangan);

        return $this->ok($undangan->load(['ruangan.pameran:id_pameran,judul,id_pengguna', 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil']));
    }

    public function status(Request $request, Undangan $undangan)
    {
        $this->pastikanPeserta($request, $undangan);

        return $this->ok([
            'id_undangan' => $undangan->id_undangan,
            'status' => $undangan->status,
            'waktu' => $undangan->waktu,
            'tautan' => $undangan->status === 'disetujui' ? $undangan->tautan : null,
        ]);
    }

    public function tanggapi(Request $request, Undangan $undangan)
    {
        $this->pastikanPenyelenggara($request, $undangan);

        $validated = $request->validate([
            'status' => ['required', Rule::in(['disetujui', 'ditolak'])],
            'catatan' => ['nullable', 'string', 'max:255'],
        ]);

        if ($undangan->status !== 'diproses') {
            return $this->error('Undangan ini sudah ditanggapi.', 409);
        }

        if ($validated['status'] === 'disetujui') {
            $undangan->forceFill(['status' => 'disetujui', 'tautan' => Str::random(32)])->save();

            return $this->ok($undangan->fresh(), 'Undangan disetujui.');
        }

        $undangan->forceFill([
            'status' => 'ditolak',
            'catatan' => $validated['catatan'] ?? $undangan->catatan,
        ])->save();

        return $this->ok($undangan->fresh(), 'Undangan ditolak.');
    }

    public function voiceToken(Request $request, Ruangan $ruangan, LayananTokenVoice $voice)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        if (! $voice->terkonfigurasi()) {
            return $this->error('Layanan voice chat belum dikonfigurasi.', 503);
        }

        return $this->ok($voice->sesiDiskusi($ruangan, $request->user()));
    }

    public function roomDto(Request $request, Ruangan $ruangan, PenyusunRoomDto $penyusun)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        $this->pastikanAksesRuangan($request, $ruangan);

        $dto = $penyusun->susun(
            $ruangan->pameran,
            $request->user(),
            isPublic: false,
            roomId: 'ruangan-'.$ruangan->id_ruangan,
        );

        return response()->json($dto);
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

    private function pemilikPameran(Undangan $undangan): ?string
    {
        return $undangan->ruangan?->pameran?->id_pengguna;
    }

    private function pastikanPeserta(Request $request, Undangan $undangan): void
    {
        $user = $request->user();
        $pemilik = $this->pemilikPameran($undangan);

        $boleh = hash_equals($undangan->id_pengguna, $user->id_pengguna)
            || ($pemilik && hash_equals($pemilik, $user->id_pengguna))
            || $user->isAdmin();

        abort_unless($boleh, 403, 'Anda tidak berhak mengakses undangan ini.');
    }

    private function pastikanPenyelenggara(Request $request, Undangan $undangan): void
    {
        $this->pastikanPemilik($request, $this->pemilikPameran($undangan), true, 'Anda tidak berhak menanggapi undangan ini.');
    }
}
