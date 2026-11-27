<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Http\Requests\Pengguna\AjukanUndanganRequest;
use App\Http\Requests\Pengguna\TanggapiUndanganRequest;
use App\Models\Karya;
use App\Models\Pameran;
use App\Models\Ruangan;
use App\Models\Undangan;
use App\Services\Diskusi\JendelaSesi;
use App\Services\Diskusi\PenjagaAksesRuangan;
use App\Services\Pameran\PenyusunRoomDto;
use App\Services\Livekit\LayananTokenVoice;
use Carbon\CarbonImmutable;
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
            'per_page' => ['nullable', 'integer', 'min:1', 'max:100'],
        ]);

        $id = $request->user()->id_pengguna;
        $perPage = (int) $request->query('per_page', 24);

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
            ->orderBy('waktu')
            ->paginate($perPage);

        return $this->ok($undangan);
    }

    public function store(AjukanUndanganRequest $request)
    {
        $validated = $request->validated();

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
            ->where(function ($q) {
                $q->where('status', 'diproses')
                    ->orWhere(function ($q) {
                        $q->where('status', 'disetujui')
                            ->where('waktu', '>=', now()->subMinutes(JendelaSesi::MENIT_SESUDAH));
                    });
            })
            ->exists();

        if ($sudahAda) {
            return $this->error('Anda sudah memiliki undangan aktif untuk ruangan ini.', 409);
        }

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $user->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => CarbonImmutable::parse($validated['waktu'])->utc(),
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

    public function tanggapi(TanggapiUndanganRequest $request, Undangan $undangan)
    {
        $this->pastikanPenyelenggara($request, $undangan);

        $validated = $request->validated();

        if ($undangan->status !== 'diproses') {
            return $this->error('Undangan ini sudah ditanggapi.', 409);
        }

        if ($undangan->waktu->lte(now())) {
            return $this->error('Jadwal undangan sudah lewat.', 409);
        }

        if ($validated['status'] === 'disetujui') {
            $bentrok = Undangan::query()
                ->where('id_ruangan', $undangan->id_ruangan)
                ->where('id_undangan', '!=', $undangan->id_undangan)
                ->where('status', 'disetujui')
                ->whereBetween('waktu', [
                    $undangan->waktu->copy()->subMinutes(JendelaSesi::DURASI_SLOT_MENIT),
                    $undangan->waktu->copy()->addMinutes(JendelaSesi::DURASI_SLOT_MENIT),
                ])
                ->exists();

            if ($bentrok) {
                return $this->error('Slot jadwal pada ruangan ini sudah terisi.', 409);
            }

            $undangan->forceFill([
                'status' => 'disetujui',
                'tautan' => Str::random(32),
                'alasan' => null,
            ])->save();

            return $this->ok(
                $undangan->fresh()->load(['ruangan.pameran:id_pameran,judul,id_pengguna,banner', 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil']),
                'Undangan disetujui.'
            );
        }

        $undangan->forceFill([
            'status' => 'ditolak',
            'alasan' => $validated['alasan'] ?? $validated['catatan'] ?? null,
        ])->save();

        return $this->ok(
            $undangan->fresh()->load(['ruangan.pameran:id_pameran,judul,id_pengguna,banner', 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil']),
            'Undangan ditolak.'
        );
    }

    public function voiceToken(Request $request, Ruangan $ruangan, LayananTokenVoice $voice)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        $validated = $request->validate([
            'undangan' => ['nullable', 'integer', 'exists:undangan,id_undangan'],
        ]);

        if (! $voice->terkonfigurasi()) {
            return $this->error('Layanan voice chat belum dikonfigurasi.', 503);
        }

        return $this->ok($voice->sesiDiskusi($ruangan, $request->user(), $validated['undangan'] ?? null));
    }

    public function roomDto(Request $request, Ruangan $ruangan, PenyusunRoomDto $penyusun, PenjagaAksesRuangan $penjaga)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        $penjaga->pastikan($ruangan, $request->user());

        $dto = $penyusun->susun(
            $ruangan->pameran,
            $request->user(),
            isPublic: false,
            roomId: 'ruangan-'.$ruangan->id_ruangan,
        );

        return response()->json($dto);
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
