<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Mail\UndanganDitanggapiMail;
use App\Mail\UndanganMasukMail;
use App\Mail\UndanganPengingatMail;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\PermintaanGabungRuangan;
use App\Models\Ruangan;
use App\Models\Undangan;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\URL;
use Tests\TestCase;

class DiskusiPrivatFlowTest extends TestCase
{
    use DatabaseTransactions;

    protected Pengguna $pengundang;
    protected Pengguna $penyelenggara;
    protected Pengguna $tamuKetok;
    protected Pameran $pameran;
    protected Ruangan $ruanganPrivat;
    protected Ruangan $ruanganUmum;

    protected function setUp(): void
    {
        parent::setUp();
        $this->withoutMiddleware(PilihKoneksi::class);
        Mail::fake();

        config([
            'services.livekit.url' => 'wss://livekit-uji.test',
            'services.livekit.key' => 'kunci-uji',
            'services.livekit.secret' => 'rahasia-uji',
        ]);

        $suffix = substr(uniqid(), -6);
        $this->pengundang = Pengguna::factory()->create([
            'nama_panggilan' => 'pengundang_'.$suffix,
            'role' => 'pengguna',
            'status' => 'aktif',
        ]);
        $this->penyelenggara = Pengguna::factory()->create([
            'nama_panggilan' => 'host_'.$suffix,
            'role' => 'pengguna',
            'status' => 'aktif',
        ]);
        $this->tamuKetok = Pengguna::factory()->create([
            'nama_panggilan' => 'tamu_'.$suffix,
            'role' => 'pengguna',
            'status' => 'aktif',
        ]);

        $model3d = Model3D::firstOrCreate(
            ['nama' => 'Template Test Hall'],
            ['jenis' => 'hall', 'file' => 'models/hall.glb', 'kapasitas' => '1', 'gambar' => 'models/hall.png']
        );

        $this->pameran = Pameran::create([
            'id_pengguna' => $this->penyelenggara->id_pengguna,
            'id_model' => $model3d->id_model,
            'judul' => 'Pameran Privat Uji',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        $this->ruanganPrivat = Ruangan::create([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);
        $this->ruanganUmum = Ruangan::create([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'umum',
        ]);
    }

    protected function tearDown(): void
    {
        Carbon::setTestNow();
        parent::tearDown();
    }

    public function test_pengajuan_undangan_valid_mengirim_email_ke_penyelenggara(): void
    {
        $waktu = now()->addDays(2)->setTime(14, 0)->format('Y-m-d H:i:s');

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktu,
            'durasi_menit' => 45,
            'catatan' => 'Diskusi kurasi karya instalasi untuk bulan depan',
        ]);

        $response->assertCreated();
        $response->assertJsonPath('data.durasi_menit', 45);
        $response->assertJsonPath('data.status', 'diproses');

        Mail::assertSent(UndanganMasukMail::class, function (UndanganMasukMail $mail) {
            return $mail->hasTo($this->penyelenggara->email)
                && str_contains($mail->urlSetujui, '/tanggapi/setujui')
                && str_contains($mail->urlTolak, '/tanggapi/tolak');
        });
    }

    public function test_validasi_durasi_dan_catatan_wajib(): void
    {
        $waktu = now()->addDays(2)->format('Y-m-d H:i:s');

        $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktu,
            'durasi_menit' => 99,
            'catatan' => 'Topik valid diskusi',
        ])->assertStatus(422);

        $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktu,
            'durasi_menit' => 30,
            'catatan' => 'Hi',
        ])->assertStatus(422);
    }

    public function test_acc_via_signed_url_email_menghasilkan_tautan_dan_kadaluarsa_tengah_malam(): void
    {
        $waktu = now()->addDays(2)->setTime(15, 30);
        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $waktu,
            'durasi_menit' => 60,
            'catatan' => 'Diskusi pameran privat',
            'status' => 'diproses',
        ]);

        $signedUrl = URL::temporarySignedRoute(
            'undangan.web.tanggapi',
            now()->addDays(7),
            ['undangan' => $undangan->id_undangan, 'aksi' => 'setujui']
        );

        $response = $this->get($signedUrl);
        $response->assertOk();

        $undangan->refresh();
        $this->assertSame('disetujui', $undangan->status);
        $this->assertNotNull($undangan->tautan);
        $this->assertSame(
            $waktu->copy()->endOfDay()->format('Y-m-d H:i:s'),
            $undangan->kadaluarsa_pada->format('Y-m-d H:i:s')
        );

        Mail::assertSent(UndanganDitanggapiMail::class, function (UndanganDitanggapiMail $mail) {
            return $mail->hasTo($this->pengundang->email)
                && str_starts_with($mail->deepLink, 'ven://diskusi/');
        });
    }

    public function test_signed_url_tanpa_signature_valid_ditolak_403(): void
    {
        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => now()->addDays(2),
            'durasi_menit' => 30,
            'catatan' => 'Diskusi',
            'status' => 'diproses',
        ]);

        $this->get("/undangan/{$undangan->id_undangan}/tanggapi/setujui")
            ->assertStatus(403);
    }

    public function test_tautan_dan_voice_kadaluarsa_setelah_00_00_mengembalikan_410(): void
    {
        $hariDiskusi = Carbon::create(2026, 12, 1, 14, 0, 0);
        Carbon::setTestNow(Carbon::create(2026, 12, 1, 13, 55, 0));

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $hariDiskusi,
            'durasi_menit' => 30,
            'catatan' => 'Diskusi hari ini',
            'status' => 'disetujui',
            'tautan' => 'token-kadaluarsa-test',
            'kadaluarsa_pada' => $hariDiskusi->copy()->endOfDay(),
        ]);

        // Sebelum 00:00 → dapat akses langsung & voice token
        $this->actingAs($this->pengundang)
            ->getJson('/api/undangan/tautan/token-kadaluarsa-test')
            ->assertOk()
            ->assertJsonPath('data.status_akses', 'langsung');

        // Setelah lewat 00:00 (00:01 keesokan harinya) → 410 Gone
        Carbon::setTestNow(Carbon::create(2026, 12, 2, 0, 1, 0));

        $this->actingAs($this->pengundang)
            ->getJson('/api/undangan/tautan/token-kadaluarsa-test')
            ->assertStatus(410);

        $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice")
            ->assertStatus(410);
    }

    public function test_waiting_room_knocking_sampai_diacc_host_dan_dapat_voice(): void
    {
        $waktu = now()->addMinutes(5);
        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $waktu,
            'durasi_menit' => 30,
            'catatan' => 'Diskusi bersama rekan',
            'status' => 'disetujui',
            'tautan' => 'token-waiting-room-test',
            'kadaluarsa_pada' => $waktu->copy()->endOfDay(),
        ]);

        // 1. Tamu lain klik link → masuk waiting room (202 menunggu)
        $knockRes = $this->actingAs($this->tamuKetok)
            ->getJson('/api/undangan/tautan/token-waiting-room-test');
        $knockRes->assertStatus(202);
        $knockRes->assertJsonPath('data.status_akses', 'menunggu');

        // Sebelum di-ACC, tamu tidak boleh dapat voice token
        $this->actingAs($this->tamuKetok)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice")
            ->assertStatus(403);

        // 2. Host lihat daftar antrean
        $antreanRes = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/antrean");
        $antreanRes->assertOk();
        $this->assertCount(1, $antreanRes->json('data.menunggu'));
        $permintaanId = $antreanRes->json('data.menunggu.0.id_permintaan');

        // Bukan host ditolak melihat antrean
        $this->actingAs($this->tamuKetok)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/antrean")
            ->assertStatus(403);

        // 3. Host ACC tamu dari antrean
        $this->actingAs($this->penyelenggara)
            ->patchJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/antrean/{$permintaanId}", [
                'status' => 'disetujui',
            ])
            ->assertOk()
            ->assertJsonPath('data.status', 'disetujui');

        // 4. Tamu sekarang akses tautan → 200 langsung, dan bisa dapat voice token!
        $this->actingAs($this->tamuKetok)
            ->getJson('/api/undangan/tautan/token-waiting-room-test')
            ->assertOk()
            ->assertJsonPath('data.status_akses', 'langsung');

        $this->actingAs($this->tamuKetok)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice")
            ->assertOk()
            ->assertJsonPath('data.url', 'wss://livekit-uji.test');
    }

    public function test_pengingat_h_minus_1_mengirim_email_sekali_dan_idempoten(): void
    {
        $besok = now()->addDay()->setTime(10, 0);
        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $besok,
            'durasi_menit' => 45,
            'catatan' => 'Diskusi besok pagi',
            'status' => 'disetujui',
            'tautan' => 'token-pengingat-h1',
            'kadaluarsa_pada' => $besok->copy()->endOfDay(),
        ]);

        // Run pertama → kirim ke pengundang & penyelenggara
        $this->artisan('undangan:kirim-pengingat')->assertSuccessful();

        Mail::assertSent(UndanganPengingatMail::class, 2);
        $undangan->refresh();
        $this->assertNotNull($undangan->pengingat_dikirim_pada);

        // Run kedua → tidak kirim duplikat
        Mail::fake();
        $this->artisan('undangan:kirim-pengingat')->assertSuccessful();
        Mail::assertNothingSent();
    }

    public function test_mengundang_karya_orang_lain_yang_ada_di_pameran_sendiri_mengarah_ke_pameran_pemilik_karya(): void
    {
        $model3d = \App\Models\Model3D::firstOrFail();
        $kategori = \App\Models\Kategori::firstOrCreate(['kategori' => '3D']);

        // Pameran milik pengundang (akun yang login)
        $pameranMilikPengundang = Pameran::create([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_model' => $model3d->id_model,
            'judul' => 'Pameran Kolaborasi Milik Pengundang',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        // Karya milik penyelenggara (akun BERBEDA), tapi dipasang di pameran milik pengundang
        $karyaOrangLain = \App\Models\Karya::create([
            'id_pengguna' => $this->penyelenggara->id_pengguna,
            'id_pameran' => $pameranMilikPengundang->id_pameran,
            'id_kategori' => $kategori->id_kategori,
            'judul' => 'Karya Kolaborasi Orang Lain',
            'jenis' => 'poster',
            'deskripsi' => 'Deskripsi karya',
            'file' => 'karya/test.jpg',
        ]);

        // Pengundang mengajukan undangan ke karya orang lain -> HARUS berhasil & masuk ke pameran milik pemilik karya!
        $res = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_karya' => $karyaOrangLain->id_karya,
            'waktu' => now()->addDays(2)->format('Y-m-d H:i:s'),
            'durasi_menit' => 60,
            'catatan' => 'Ingin diskusi terkait karya kolaborasi ini',
        ]);

        $res->assertCreated();
        $res->assertJsonPath('data.ruangan.id_pameran', $this->pameran->id_pameran);

        Mail::assertSent(UndanganMasukMail::class, fn (UndanganMasukMail $m) => $m->hasTo($this->penyelenggara->email));
    }
}
