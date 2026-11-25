<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class EdgegapServerTest extends TestCase
{
    use DatabaseTransactions;

    protected function setUp(): void
    {
        parent::setUp();
        $this->withoutMiddleware(PilihKoneksi::class);
        Cache::flush();

        config([
            'services.edgegap.url' => 'https://api.edgegap.com',
            'services.edgegap.token' => 'token test-edgegap-key',
            'services.edgegap.app_name' => 'ven',
            'services.edgegap.app_version' => '26.11.24-23.01.49-UTC',
        ]);
    }

    private function buatPameran(Pengguna $pemilik): Pameran
    {
        $model = Model3D::firstOrCreate(
            ['nama' => 'Hall Template Kecil Edgegap'],
            ['jenis' => 'hall', 'file' => 'models/template01.glb', 'kapasitas' => '9', 'gambar' => 'models/preview.png']
        );

        return Pameran::create([
            'id_pengguna' => $pemilik->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => 'Pameran Multiplayer',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);
    }

    public function test_pengunjung_dapat_meminta_server_pameran_dan_memakai_ulang_deployment_sama(): void
    {
        $pemilik = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $pengunjung1 = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $pengunjung2 = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $pameran = $this->buatPameran($pemilik);

        $deployCalls = 0;

        Http::fake([
            'https://api.edgegap.com/v1/deployments' => Http::response(['data' => []], 200),
            'https://api.edgegap.com/v1/deploy' => function () use (&$deployCalls) {
                $deployCalls++;

                return Http::response(['request_id' => 'req_ven_001'], 200);
            },
            'https://api.edgegap.com/v1/status/req_ven_001' => Http::response([
                'request_id' => 'req_ven_001',
                'current_status' => 'Status.READY',
                'ready' => true,
                'fqdn' => 'req_ven_001.pr.edgegap.net',
                'public_ip' => '172.237.68.185',
                'ports' => [
                    'gameport' => [
                        'external' => 31763,
                        'internal' => 7777,
                        'protocol' => 'UDP',
                    ],
                ],
            ], 200),
        ]);

        $res1 = $this->actingAs($pengunjung1, 'sanctum')
            ->postJson('/api/room-server/'.$pameran->id_pameran);

        $res1->assertOk()
            ->assertJsonPath('data.request_id', 'req_ven_001')
            ->assertJsonPath('data.ready', true)
            ->assertJsonPath('data.public_ip', '172.237.68.185')
            ->assertJsonPath('data.port', 31763);

        $res2 = $this->actingAs($pengunjung2, 'sanctum')
            ->postJson('/api/room-server/'.$pameran->id_pameran);

        $res2->assertOk()
            ->assertJsonPath('data.request_id', 'req_ven_001')
            ->assertJsonPath('data.port', 31763);

        $this->assertSame(1, $deployCalls, 'Pengunjung kedua harus memakai ulang deployment ruangan yang sama.');
    }

    public function test_ruangan_privat_menolak_pengguna_bukan_peserta_dan_mengizinkan_peserta(): void
    {
        $pemilik = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $peserta = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $orangAsing = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $pameran = $this->buatPameran($pemilik);

        $ruangan = Ruangan::create([
            'id_pameran' => $pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        Undangan::forceCreate([
            'id_pengguna' => $peserta->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addHour(),
            'status' => 'disetujui',
        ]);

        Http::fake([
            'https://api.edgegap.com/v1/deployments' => Http::response(['data' => []], 200),
            'https://api.edgegap.com/v1/deploy' => Http::response(['request_id' => 'req_priv_99'], 200),
            'https://api.edgegap.com/v1/status/req_priv_99' => Http::response([
                'request_id' => 'req_priv_99',
                'current_status' => 'Status.INITIALIZING',
                'ready' => false,
            ], 200),
        ]);

        $this->actingAs($orangAsing, 'sanctum')
            ->postJson('/api/room-server/ruangan-'.$ruangan->id_pameran.'999')
            ->assertNotFound();

        $this->actingAs($orangAsing, 'sanctum')
            ->postJson('/api/room-server/ruangan-'.$ruangan->id_ruangan)
            ->assertForbidden();

        $this->actingAs($peserta, 'sanctum')
            ->postJson('/api/room-server/ruangan-'.$ruangan->id_ruangan)
            ->assertOk()
            ->assertJsonPath('data.request_id', 'req_priv_99')
            ->assertJsonPath('data.ready', false);
    }
}
