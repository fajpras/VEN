<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Tests\TestCase;

class DiskusiVoiceTest extends TestCase
{
    use DatabaseTransactions;

    protected Pengguna $pengundang;
    protected Pengguna $penyelenggara;
    protected Pengguna $orangLain;
    protected Pameran $pameran;
    protected Ruangan $ruanganPrivat;
    protected Ruangan $ruanganUmum;

    protected function setUp(): void
    {
        parent::setUp();
        $this->withoutMiddleware(PilihKoneksi::class);

        config([
            'services.livekit.url' => 'wss://livekit-uji.test',
            'services.livekit.key' => 'kunci-uji',
            'services.livekit.secret' => 'rahasia-uji',
        ]);

        $this->pengundang = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $this->penyelenggara = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $this->orangLain = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);

        $model3d = Model3D::firstOrCreate(
            ['nama' => 'Template Test Hall'],
            ['jenis' => 'hall', 'file' => 'models/hall.glb', 'kapasitas' => '1', 'gambar' => 'models/hall.png']
        );

        $this->pameran = Pameran::create([
            'id_pengguna' => $this->penyelenggara->id_pengguna,
            'id_model' => $model3d->id_model,
            'judul' => 'Pameran Virtual Uji',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        $this->ruanganPrivat = Ruangan::create(['id_pameran' => $this->pameran->id_pameran, 'jenis' => 'privat']);
        $this->ruanganUmum = Ruangan::create(['id_pameran' => $this->pameran->id_pameran, 'jenis' => 'umum']);
    }

    private function buatUndanganDisetujui(string $waktu = null): Undangan
    {
        return Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $waktu ?? now()->addMinutes(10),
            'catatan' => 'Diskusi karya',
            'status' => 'disetujui',
            'tautan' => 'tautan-uji-'.uniqid(),
        ]);
    }

    private function dekodeKlaim(string $jwt): array
    {
        [$head, $body, $tandaTangan] = explode('.', $jwt);
        $klaim = json_decode(base64_decode(strtr($body, '-_', '+/')), true);

        $tandaHarapan = rtrim(strtr(base64_encode(
            hash_hmac('sha256', "$head.$body", 'rahasia-uji', true)
        ), '+/', '-_'), '=');

        $this->assertSame($tandaHarapan, $tandaTangan, 'Tanda tangan JWT tidak cocok dengan rahasia konfigurasi.');

        return $klaim;
    }

    public function test_peserta_disetujui_menerima_token_voice_ruangan_privat(): void
    {
        $undangan = $this->buatUndanganDisetujui();

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice");

        $response->assertOk();
        $response->assertJsonPath('data.url', 'wss://livekit-uji.test');
        $response->assertJsonPath('data.room', 'diskusi-'.$this->ruanganPrivat->id_ruangan.'-'.$undangan->id_undangan);

        $klaim = $this->dekodeKlaim($response->json('data.token'));
        $this->assertSame('kunci-uji', $klaim['iss']);
        $this->assertSame($this->pengundang->id_pengguna, $klaim['sub']);
        $this->assertSame('diskusi-'.$this->ruanganPrivat->id_ruangan.'-'.$undangan->id_undangan, $klaim['video']['room']);
        $this->assertTrue($klaim['video']['roomJoin']);
        $this->assertTrue($klaim['video']['canPublish']);
        $this->assertTrue($klaim['video']['canSubscribe']);
    }

    public function test_penyelenggara_menerima_token_voice_tanpa_undangan_sendiri(): void
    {
        $undangan = $this->buatUndanganDisetujui();

        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice");

        $response->assertOk();
        $this->assertSame(
            'diskusi-'.$this->ruanganPrivat->id_ruangan.'-'.$undangan->id_undangan,
            $response->json('data.room')
        );
    }

    public function test_voice_ditolak_untuk_ruangan_umum(): void
    {
        $this->buatUndanganDisetujui();

        // Bahkan penyelenggara pun tidak boleh memakai voice di ruangan umum.
        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganUmum->id_ruangan}/voice");

        $response->assertStatus(403);
        $response->assertJsonPath('message', 'Voice chat hanya tersedia di ruangan privat.');
    }

    public function test_voice_ditolak_tanpa_undangan_disetujui(): void
    {
        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice");

        $response->assertStatus(403);
        $response->assertJsonPath('message', 'Anda tidak memiliki sesi diskusi aktif di ruangan ini.');
    }

    public function test_voice_ditolak_untuk_undangan_di_luar_jendela_sesi(): void
    {
        $this->buatUndanganDisetujui(now()->addDays(2));

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice");

        $response->assertStatus(403);
    }

    public function test_voice_diterima_10_menit_setelah_jadwal(): void
    {
        $this->buatUndanganDisetujui(now()->subMinutes(10));

        $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice")
            ->assertOk();
    }

    public function test_voice_ditolak_20_menit_setelah_jadwal(): void
    {
        $this->buatUndanganDisetujui(now()->subMinutes(20));

        $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice")
            ->assertStatus(403);
    }

    public function test_penyelenggara_memilih_undangan_sesuai_parameter(): void
    {
        $this->buatUndanganDisetujui();

        $undanganKedua = Undangan::forceCreate([
            'id_pengguna' => $this->orangLain->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => now()->addMinutes(5),
            'catatan' => 'Diskusi lain',
            'status' => 'disetujui',
            'tautan' => 'tautan-uji-'.uniqid(),
        ]);

        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice?undangan={$undanganKedua->id_undangan}");

        $response->assertOk();
        $this->assertSame(
            'diskusi-'.$this->ruanganPrivat->id_ruangan.'-'.$undanganKedua->id_undangan,
            $response->json('data.room')
        );
    }

    public function test_voice_mengembalikan_503_tanpa_konfigurasi_livekit(): void
    {
        $this->buatUndanganDisetujui();
        config(['services.livekit.key' => null, 'services.livekit.secret' => null]);

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/voice");

        $response->assertStatus(503);
        $response->assertJsonPath('message', 'Layanan voice chat belum dikonfigurasi.');
    }

    public function test_voice_mengembalikan_404_untuk_ruangan_tanpa_pameran(): void
    {
        $ruanganYatim = Ruangan::create(['id_pameran' => null, 'jenis' => 'privat']);

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$ruanganYatim->id_ruangan}/voice");

        $response->assertStatus(404);
    }
}
