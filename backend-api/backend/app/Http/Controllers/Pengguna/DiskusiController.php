<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Mail\UndanganDitanggapiMail;
use App\Mail\UndanganMasukMail;
use App\Models\Karya;
use App\Models\Pameran;
use App\Models\PermintaanGabungRuangan;
use App\Models\Ruangan;
use App\Models\Undangan;
use App\Services\Livekit\LayananTokenVoice;
use App\Services\Pameran\PenyusunRoomDto;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\URL;
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
            'ruangan.pameran.pengguna:id_pengguna,nama,nama_panggilan,foto_profil',
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
            'id_ruangan'   => ['nullable', 'integer', Rule::exists('ruangan', 'id_ruangan')->where('jenis', 'privat')],
            'id_pameran'   => ['nullable', 'string', 'exists:pameran,id_pameran'],
            'id_karya'     => ['nullable', 'string', 'exists:karya,id_karya'],
            'waktu'        => ['required', 'date', 'after:now'],
            'durasi_menit' => ['nullable', 'integer', Rule::in([30, 45, 60])],
            'catatan'      => ['required', 'string', 'min:5', 'max:255'],
        ]);

        if (empty($validated['id_ruangan']) && empty($validated['id_pameran']) && empty($validated['id_karya'])) {
            return $this->error('Target pameran atau ruangan harus disertakan.', 422);
        }

        $user = $request->user();

        if (! empty($validated['id_ruangan'])) {
            $ruangan = Ruangan::with('pameran.pengguna')->findOrFail($validated['id_ruangan']);
        } elseif (! empty($validated['id_pameran'])) {
            $pameran = Pameran::with('pengguna')->findOrFail($validated['id_pameran']);
            $ruangan = Ruangan::firstOrCreate(
                ['id_pameran' => $pameran->id_pameran, 'jenis' => 'privat']
            );
            $ruangan->setRelation('pameran', $pameran);
        } else {
            $karya = Karya::with('pameran.pengguna')->findOrFail($validated['id_karya']);

            if (hash_equals((string) $karya->id_pengguna, (string) $user->id_pengguna)) {
                return $this->error('Anda tidak perlu mengundang diri sendiri pada karya Anda.', 422);
            }

            // Jika karya ikut pameran yang kebetulan dimiliki pengaju sendiri, arahkan ke pameran milik pemilik karya.
            $pameranKarya = $karya->pameran;
            if ($pameranKarya && hash_equals((string) $pameranKarya->id_pengguna, (string) $user->id_pengguna)) {
                $pameranKarya = null;
            }

            $pameran = $pameranKarya
                ?: Pameran::with('pengguna')->where('id_pengguna', $karya->id_pengguna)->latest('id_pameran')->first();

            if (! $pameran) {
                return $this->notFound('Pemilik karya belum memiliki ruang pameran untuk diskusi privat.');
            }

            $ruangan = Ruangan::firstOrCreate(
                ['id_pameran' => $pameran->id_pameran, 'jenis' => 'privat']
            );
            $ruangan->setRelation('pameran', $pameran);
        }

        if (! $ruangan->pameran) {
            return $this->notFound('Pameran tidak tersedia.');
        }

        if (hash_equals((string) $ruangan->pameran->id_pengguna, (string) $user->id_pengguna)) {
            return $this->error('Anda tidak perlu mengundang diri sendiri ke pameran Anda.', 422);
        }

        // Hanya blokir jika masih ada undangan aktif yang belum kadaluarsa
        $sudahAda = Undangan::where('id_pengguna', $user->id_pengguna)
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->whereIn('status', ['diproses', 'disetujui'])
            ->where(function ($q) {
                $q->whereNull('kadaluarsa_pada')
                    ->orWhere('kadaluarsa_pada', '>', now());
            })
            ->exists();

        if ($sudahAda) {
            return $this->error('Anda sudah memiliki undangan aktif untuk ruangan ini.', 409);
        }

        $undangan = Undangan::forceCreate([
            'id_pengguna'  => $user->id_pengguna,
            'id_ruangan'   => $ruangan->id_ruangan,
            'waktu'        => $validated['waktu'],
            'durasi_menit' => (int) ($validated['durasi_menit'] ?? 30),
            'catatan'      => trim($validated['catatan']),
            'status'       => 'diproses',
        ]);

        $undangan->load([
            'ruangan.pameran:id_pameran,judul,id_pengguna,banner',
            'ruangan.pameran.pengguna:id_pengguna,nama,nama_panggilan,email,foto_profil',
            'pengguna:id_pengguna,nama,nama_panggilan,email,foto_profil',
        ]);

        $this->kirimEmailUndanganMasuk($undangan);

        return $this->created($undangan, 'Undangan diskusi diajukan.');
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
            'id_undangan'      => $undangan->id_undangan,
            'status'           => $undangan->status,
            'waktu'            => $undangan->waktu,
            'durasi_menit'     => $undangan->durasi_menit ?? 30,
            'tautan'           => $undangan->status === 'disetujui' ? $undangan->tautan : null,
            'deep_link'        => $undangan->status === 'disetujui' && $undangan->tautan ? 'ven://diskusi/'.$undangan->tautan : null,
            'kadaluarsa_pada'  => $undangan->batasKadaluarsa(),
            'sudah_kadaluarsa' => $undangan->sudahKadaluarsa(),
        ]);
    }

    public function tanggapi(Request $request, Undangan $undangan)
    {
        $this->pastikanPenyelenggara($request, $undangan);

        $validated = $request->validate([
            'status' => ['required', Rule::in(['disetujui', 'ditolak'])],
            'alasan' => ['nullable', 'string', 'max:255'],
            'catatan' => ['nullable', 'string', 'max:255'],
        ]);

        if ($undangan->status !== 'diproses') {
            return $this->error('Undangan ini sudah ditanggapi.', 409);
        }

        if ($undangan->waktu && $undangan->waktu->lte(now())) {
            return $this->error('Jadwal undangan sudah lewat.', 409);
        }

        if ($validated['status'] === 'disetujui') {
            $undangan->forceFill([
                'status'          => 'disetujui',
                'tautan'          => Str::random(32),
                'kadaluarsa_pada' => $undangan->waktu?->copy()->endOfDay() ?? now()->endOfDay(),
            ])->save();

            $undangan->load(['ruangan.pameran', 'pengguna']);
            $this->kirimEmailHasilTanggapan($undangan);

            return $this->ok($undangan->fresh(), 'Undangan disetujui.');
        }

        $undangan->forceFill([
            'status' => 'ditolak',
            'alasan' => $validated['alasan'] ?? $validated['catatan'] ?? null,
        ])->save();

        $undangan->load(['ruangan.pameran', 'pengguna']);
        $this->kirimEmailHasilTanggapan($undangan);

        return $this->ok($undangan->fresh(), 'Undangan ditolak.');
    }

    /**
     * Akses ruang privat menggunakan token tautan khusus (`ven://diskusi/{token}`).
     * - Host & pengaju asli yang di-ACC: langsung masuk (`status_akses = "langsung"`).
     * - Pengguna lain yang memiliki tautan: masuk ruang tunggu / knocking (`status_akses = "menunggu"`).
     * - Lewat pukul 00:00 akhir hari jadwal: 410 Gone.
     */
    public function aksesTautan(Request $request, string $token)
    {
        $undangan = Undangan::with(['ruangan.pameran', 'pengguna'])
            ->where('tautan', $token)
            ->where('status', 'disetujui')
            ->first();

        if (! $undangan || ! $undangan->ruangan || ! $undangan->ruangan->pameran) {
            return $this->notFound('Tautan diskusi privat tidak ditemukan.');
        }

        if ($undangan->sudahKadaluarsa()) {
            return $this->error('Tautan sesi diskusi telah berakhir pada pukul 00:00.', 410);
        }

        $user = $request->user();
        $pameran = $undangan->ruangan->pameran;
        $isHost = hash_equals((string) $pameran->id_pengguna, (string) $user->id_pengguna);
        $isPengajuAsli = hash_equals((string) $undangan->id_pengguna, (string) $user->id_pengguna);

        $payloadDasar = [
            'id_undangan'     => $undangan->id_undangan,
            'id_ruangan'      => $undangan->id_ruangan,
            'id_pameran'      => $pameran->id_pameran,
            'judul_pameran'   => $pameran->judul,
            'waktu'           => $undangan->waktu,
            'durasi_menit'    => $undangan->durasi_menit ?? 30,
            'tautan'          => $undangan->tautan,
            'deep_link'       => 'ven://diskusi/'.$undangan->tautan,
            'kadaluarsa_pada' => $undangan->batasKadaluarsa(),
            'is_host'         => $isHost,
        ];

        if ($isHost || $isPengajuAsli || $user->isAdmin()) {
            return $this->ok(array_merge($payloadDasar, ['status_akses' => 'langsung']));
        }

        $permintaan = PermintaanGabungRuangan::firstOrCreate(
            [
                'id_undangan' => $undangan->id_undangan,
                'id_pengguna' => $user->id_pengguna,
            ],
            [
                'id_ruangan' => $undangan->id_ruangan,
                'status'     => 'menunggu',
            ],
        );

        if ($permintaan->status === 'disetujui') {
            return $this->ok(array_merge($payloadDasar, [
                'status_akses'  => 'langsung',
                'id_permintaan' => $permintaan->id_permintaan,
            ]));
        }

        if ($permintaan->status === 'ditolak') {
            return $this->error('Pemilik pameran menolak permintaan bergabung Anda.', 403);
        }

        return response()->json([
            'message' => 'Menunggu izin pemilik pameran untuk bergabung.',
            'data'    => array_merge($payloadDasar, [
                'status_akses'  => 'menunggu',
                'id_permintaan' => $permintaan->id_permintaan,
            ]),
        ], 202);
    }

    /**
     * Daftar antrean Waiting Room (knocking) & peserta sesi privat untuk Host
     * (dipanggil oleh Bottom Sheet Jadwal di dalam Unity PlayScene & Tab Jadwal).
     */
    public function antrean(Request $request, Ruangan $ruangan)
    {
        $ruangan->loadMissing('pameran');
        abort_unless($ruangan->pameran && $ruangan->jenis === 'privat', 404);

        $user = $request->user();
        $isHost = hash_equals((string) $ruangan->pameran->id_pengguna, (string) $user->id_pengguna);
        abort_unless($isHost || $user->isAdmin(), 403, 'Hanya pemilik pameran yang dapat mengelola antrean ruang privat.');

        $undanganAktif = $this->undanganAktifUntukRuangan($ruangan);

        $antrean = PermintaanGabungRuangan::with('pengguna:id_pengguna,nama,nama_panggilan,foto_profil')
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->when($undanganAktif, fn ($q) => $q->where('id_undangan', $undanganAktif->id_undangan))
            ->latest('id_permintaan')
            ->get();

        return $this->ok([
            'id_ruangan'      => $ruangan->id_ruangan,
            'id_pameran'      => $ruangan->pameran->id_pameran,
            'judul_pameran'   => $ruangan->pameran->judul,
            'tautan'          => $undanganAktif?->tautan,
            'deep_link'       => $undanganAktif?->tautan ? 'ven://diskusi/'.$undanganAktif->tautan : null,
            'kadaluarsa_pada' => $undanganAktif?->batasKadaluarsa(),
            'pengaju_utama'   => $undanganAktif?->pengguna,
            'menunggu'        => $antrean->where('status', 'menunggu')->values(),
            'disetujui'       => $antrean->where('status', 'disetujui')->values(),
        ]);
    }

    /**
     * Host menyetujui / menolak tamu dari Waiting Room (mirip Google Meet).
     */
    public function tanggapiAntrean(Request $request, Ruangan $ruangan, PermintaanGabungRuangan $permintaan)
    {
        $ruangan->loadMissing('pameran');
        abort_unless($ruangan->pameran && $ruangan->jenis === 'privat', 404);
        abort_unless((int) $permintaan->id_ruangan === (int) $ruangan->id_ruangan, 404);

        $user = $request->user();
        $isHost = hash_equals((string) $ruangan->pameran->id_pengguna, (string) $user->id_pengguna);
        abort_unless($isHost || $user->isAdmin(), 403, 'Hanya pemilik pameran yang dapat menanggapi permintaan bergabung.');

        $validated = $request->validate([
            'status' => ['required', Rule::in(['disetujui', 'ditolak'])],
        ]);

        $permintaan->forceFill(['status' => $validated['status']])->save();

        return $this->ok(
            $permintaan->fresh('pengguna:id_pengguna,nama,nama_panggilan,foto_profil'),
            $validated['status'] === 'disetujui' ? 'Peserta diizinkan masuk ruang privat.' : 'Permintaan bergabung ditolak.'
        );
    }

    /**
     * Token Voice Chat — EKSKLUSIF hanya untuk ruang privat aktif & peserta yang diizinkan.
     */
    public function voiceToken(Request $request, Ruangan $ruangan, LayananTokenVoice $voice)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        if ($ruangan->jenis !== 'privat') {
            return $this->error('Voice chat hanya tersedia di ruangan privat.', 403);
        }

        $cek = $this->verifikasiAksesRuanganPrivat($request, $ruangan, 'Anda tidak memiliki sesi diskusi aktif di ruangan ini.');
        if ($cek !== true) {
            return $cek;
        }

        if (! $voice->terkonfigurasi()) {
            return $this->error('Layanan voice chat belum dikonfigurasi.', 503);
        }

        $sesi = $voice->sesiDiskusi($ruangan, $request->user());
        if (! $sesi) {
            return $this->error('Anda tidak memiliki sesi diskusi aktif di ruangan ini.', 403);
        }

        return $this->ok($sesi);
    }

    public function roomDto(Request $request, Ruangan $ruangan, PenyusunRoomDto $penyusun)
    {
        $ruangan->loadMissing('pameran');

        abort_unless($ruangan->pameran, 404);

        $cek = $this->verifikasiAksesRuanganPrivat($request, $ruangan, 'Anda tidak berhak mengakses ruangan ini.');
        if ($cek !== true) {
            return $cek;
        }

        $dto = $penyusun->susun(
            $ruangan->pameran,
            $request->user(),
            isPublic: false,
            roomId: 'ruangan-'.$ruangan->id_ruangan,
        );

        return response()->json($dto);
    }

    private function undanganAktifUntukRuangan(Ruangan $ruangan): ?Undangan
    {
        return Undangan::with('pengguna:id_pengguna,nama,nama_panggilan,foto_profil')
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->where('status', 'disetujui')
            ->where(function ($q) {
                $q->whereNull('kadaluarsa_pada')
                    ->orWhere('kadaluarsa_pada', '>', now());
            })
            ->latest('id_undangan')
            ->first();
    }

    /**
     * Memastikan user berhak mengakses ruang privat (baik RoomDto maupun Voice Chat)
     * dan sesi belum melewati batas kadaluarsa 00:00.
     *
     * @return true|\Illuminate\Http\JsonResponse
     */
    private function verifikasiAksesRuanganPrivat(
        Request $request,
        Ruangan $ruangan,
        string $pesanDitolak = 'Anda tidak berhak mengakses ruangan ini.'
    ): mixed {
        $user = $request->user();
        $isHost = hash_equals((string) $ruangan->pameran->id_pengguna, (string) $user->id_pengguna);

        $undanganDisetujui = Undangan::query()
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->where('status', 'disetujui')
            ->latest('id_undangan')
            ->get();

        $adaYangAktif = $undanganDisetujui->contains(fn (Undangan $u) => ! $u->sudahKadaluarsa());

        if ($undanganDisetujui->isNotEmpty() && ! $adaYangAktif && ! $user->isAdmin()) {
            return $this->error('Sesi ruang privat telah berakhir pada pukul 00:00.', 410);
        }

        if ($isHost || $user->isAdmin()) {
            return true;
        }

        $pesertaAsli = $undanganDisetujui->contains(
            fn (Undangan $u) => ! $u->sudahKadaluarsa() && hash_equals((string) $u->id_pengguna, (string) $user->id_pengguna)
        );

        if ($pesertaAsli) {
            return true;
        }

        $idUndanganAktif = $undanganDisetujui
            ->filter(fn (Undangan $u) => ! $u->sudahKadaluarsa())
            ->pluck('id_undangan');

        $tamuDisetujui = $idUndanganAktif->isNotEmpty() && PermintaanGabungRuangan::query()
            ->where('id_ruangan', $ruangan->id_ruangan)
            ->whereIn('id_undangan', $idUndanganAktif)
            ->where('id_pengguna', $user->id_pengguna)
            ->where('status', 'disetujui')
            ->exists();

        if ($tamuDisetujui) {
            return true;
        }

        return $this->error($pesanDitolak, 403);
    }

    private function kirimEmailUndanganMasuk(Undangan $undangan): void
    {
        $emailPemilik = $undangan->ruangan?->pameran?->pengguna?->email;
        if (! $emailPemilik) {
            return;
        }

        $batasLink = $undangan->waktu?->copy()->endOfDay() ?? now()->addDays(7);
        $urlSetujui = URL::temporarySignedRoute('undangan.web.tanggapi', $batasLink, [
            'undangan' => $undangan->id_undangan,
            'aksi'     => 'setujui',
        ]);
        $urlTolak = URL::temporarySignedRoute('undangan.web.tanggapi', $batasLink, [
            'undangan' => $undangan->id_undangan,
            'aksi'     => 'tolak',
        ]);

        try {
            Mail::to($emailPemilik)->send(new UndanganMasukMail($undangan, $urlSetujui, $urlTolak));
        } catch (\Throwable $e) {
            Log::warning('Gagal mengirim email UndanganMasukMail: '.$e->getMessage());
        }
    }

    private function kirimEmailHasilTanggapan(Undangan $undangan): void
    {
        $emailPengaju = $undangan->pengguna?->email;
        if (! $emailPengaju) {
            return;
        }

        $deepLink = $undangan->status === 'disetujui' && $undangan->tautan
            ? 'ven://diskusi/'.$undangan->tautan
            : null;

        try {
            Mail::to($emailPengaju)->send(new UndanganDitanggapiMail($undangan, $deepLink));
        } catch (\Throwable $e) {
            Log::warning('Gagal mengirim email UndanganDitanggapiMail: '.$e->getMessage());
        }
    }

    private function pastikanPenyelenggara(Request $request, Undangan $undangan): void
    {
        $pemilik = $undangan->ruangan?->pameran?->id_pengguna;

        abort_unless(
            $pemilik && hash_equals((string) $pemilik, (string) $request->user()->id_pengguna),
            403,
            'Hanya pemilik pameran yang dapat menanggapi undangan ini.'
        );
    }

    private function pastikanPeserta(Request $request, Undangan $undangan): void
    {
        $id = (string) $request->user()->id_pengguna;
        $pemilik = (string) ($undangan->ruangan?->pameran?->id_pengguna ?? '');

        abort_unless(
            hash_equals((string) $undangan->id_pengguna, $id) || ($pemilik !== '' && hash_equals($pemilik, $id)),
            403,
            'Anda tidak berhak melihat undangan ini.'
        );
    }
}
