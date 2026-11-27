<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Tests\TestCase;

class PameranRuanganTest extends TestCase
{
    use DatabaseTransactions;

    public function test_store_ruangan_idempoten_untuk_jenis_yang_sama(): void
    {
        $this->withoutMiddleware(PilihKoneksi::class);

        $pemilik = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);

        $model = Model3D::firstOrCreate(
            ['nama' => 'Template Test Hall'],
            ['jenis' => 'hall', 'file' => 'models/hall.glb', 'kapasitas' => '1', 'gambar' => 'models/hall.png']
        );

        $pameran = Pameran::create([
            'id_pengguna' => $pemilik->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => 'Pameran Ruangan Idempoten',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        $this->actingAs($pemilik, 'sanctum')
            ->postJson("/api/pameran/{$pameran->id_pameran}/ruangan", ['jenis' => 'privat'])
            ->assertCreated();

        $this->actingAs($pemilik, 'sanctum')
            ->postJson("/api/pameran/{$pameran->id_pameran}/ruangan", ['jenis' => 'privat'])
            ->assertOk();

        $this->assertSame(
            1,
            Ruangan::where('id_pameran', $pameran->id_pameran)->where('jenis', 'privat')->count()
        );
    }
}
