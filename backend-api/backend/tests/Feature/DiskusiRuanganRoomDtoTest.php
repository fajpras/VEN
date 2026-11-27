<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class DiskusiRuanganRoomDtoTest extends TestCase
{
    use DatabaseTransactions;

    protected Pengguna $pengundang;
    protected Pengguna $penyelenggara;
    protected Pengguna $orangLain;
    protected Pengguna $admin;
    protected Pameran $pameran;
    protected Ruangan $ruanganPrivat;

    protected function setUp(): void
    {
        parent::setUp();
        $this->withoutMiddleware(PilihKoneksi::class);

        $this->pengundang = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $this->penyelenggara = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $this->orangLain = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $this->admin = Pengguna::factory()->create(['role' => 'admin', 'status' => 'aktif']);

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
    }

    private function buatUndangan(string $status, ?Carbon $waktu = null): Undangan
    {
        return Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $this->ruanganPrivat->id_ruangan,
            'waktu' => $waktu ?? now()->addMinutes(10),
            'catatan' => 'Diskusi karya',
            'status' => $status,
            'tautan' => $status === 'disetujui' ? 'tautan-uji-'.uniqid() : null,
        ]);
    }

    public function test_penyelenggara_dapat_membuka_room_dto_ruangan_privat(): void
    {
        $this->buatUndangan('disetujui');

        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertOk();
        $response->assertJsonPath('roomId', 'ruangan-'.$this->ruanganPrivat->id_ruangan);
        $response->assertJsonPath('isPublic', false);
        $response->assertJsonPath('templateId', 'template01');
        $response->assertJsonPath('title', 'Pameran Virtual Uji');
        $response->assertJsonPath('wallColor', '#1565C0');
        $response->assertJsonPath('ownerId', (string) $this->penyelenggara->id_pengguna);
        $response->assertJsonPath('viewerId', (string) $this->penyelenggara->id_pengguna);
        $this->assertNotEmpty($response->json('items'));
    }

    public function test_peserta_disetujui_dapat_membuka_room_dto_ruangan_privat(): void
    {
        $this->buatUndangan('disetujui');

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertOk();
        $response->assertJsonPath('isPublic', false);
        $response->assertJsonPath('viewerId', (string) $this->pengundang->id_pengguna);
    }

    public function test_peserta_diproses_ditolak_membuka_room_dto(): void
    {
        $this->buatUndangan('diproses');

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertStatus(403);
        $response->assertJsonPath('message', 'Anda tidak berhak mengakses ruangan ini.');
    }

    public function test_pengguna_lain_ditolak_membuka_room_dto(): void
    {
        $this->buatUndangan('disetujui');

        $response = $this->actingAs($this->orangLain)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertStatus(403);
    }

    public function test_admin_dapat_membuka_room_dto_ruangan_privat(): void
    {
        $response = $this->actingAs($this->admin)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertOk();
        $response->assertJsonPath('isPublic', false);
    }

    public function test_penyelenggara_tanpa_undangan_di_jendela_ditolak(): void
    {
        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertStatus(403);
    }

    public function test_peserta_disetujui_di_luar_jendela_ditolak(): void
    {
        $this->buatUndangan('disetujui', now()->addDays(2));

        $response = $this->actingAs($this->pengundang)
            ->getJson("/api/ruangan/{$this->ruanganPrivat->id_ruangan}/room-dto");

        $response->assertStatus(403);
    }

    public function test_room_dto_mengembalikan_404_untuk_ruangan_tanpa_pameran(): void
    {
        $ruanganYatim = Ruangan::create(['id_pameran' => null, 'jenis' => 'privat']);

        $response = $this->actingAs($this->penyelenggara)
            ->getJson("/api/ruangan/{$ruanganYatim->id_ruangan}/room-dto");

        $response->assertStatus(404);
    }
}
