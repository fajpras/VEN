<?php

namespace Tests\Feature;

use App\Models\Karya;
use App\Models\Kategori;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class PencarianDiscoveryTest extends TestCase
{
    private Pengguna $pengguna;
    private Kategori $kategoriMinat;
    private Kategori $kategoriLuar;
    private Karya $karyaMinat;
    private Karya $karyaLuar;
    private Pameran $pameranMinat;
    private Pameran $pameranLuar;
    private string $keyword;

    protected function setUp(): void
    {
        parent::setUp();

        $this->assertSame(
            'ven_testing',
            DB::connection('mysql')->getDatabaseName(),
            'Test harus menggunakan database ven_testing.'
        );

        $this->keyword = 'Discovery'.uniqid();

        $this->pengguna = Pengguna::factory()->create([
            'email' => 'discovery-'.uniqid().'@ven.test',
            'nama_panggilan' => 'discovery_'.uniqid(),
        ]);

        $this->kategoriMinat = Kategori::firstOrCreate(['kategori' => 'Minat '.uniqid()]);
        $this->kategoriLuar = Kategori::firstOrCreate(['kategori' => 'Luar '.uniqid()]);

        $model = Model3D::firstOrCreate(
            ['jenis' => 'hall', 'nama' => 'Template Discovery Test'],
            ['file' => 'discovery.glb', 'gambar' => 'discovery.png', 'kapasitas' => 10]
        );

        $this->pameranMinat = Pameran::create([
            'id_pengguna' => $this->pengguna->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => "Pameran Minat {$this->keyword}",
            'tipe' => 'kecil',
            'warna' => '#1F1F1F',
            'banner' => 'pameran_minat.jpg',
        ]);

        $this->pameranLuar = Pameran::create([
            'id_pengguna' => $this->pengguna->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => "Pameran Luar {$this->keyword}",
            'tipe' => 'kecil',
            'warna' => '#1F1F1F',
            'banner' => 'pameran_luar.jpg',
        ]);

        $this->karyaMinat = Karya::create([
            'id_pengguna' => $this->pengguna->id_pengguna,
            'id_pameran' => $this->pameranMinat->id_pameran,
            'id_kategori' => $this->kategoriMinat->id_kategori,
            'judul' => "Karya Minat {$this->keyword}",
            'deskripsi' => "Deskripsi minat {$this->keyword}",
            'file' => 'karya_minat.jpg',
            'jenis' => 'poster',
        ]);

        $this->karyaLuar = Karya::create([
            'id_pengguna' => $this->pengguna->id_pengguna,
            'id_pameran' => $this->pameranLuar->id_pameran,
            'id_kategori' => $this->kategoriLuar->id_kategori,
            'judul' => "Karya Luar {$this->keyword}",
            'deskripsi' => "Deskripsi luar {$this->keyword}",
            'file' => 'karya_luar.jpg',
            'jenis' => 'poster',
        ]);
    }

    protected function tearDown(): void
    {
        $this->pengguna->tokens()->delete();
        $this->pengguna->kategori()->detach();

        Karya::whereIn('id_karya', [
            $this->karyaMinat->id_karya,
            $this->karyaLuar->id_karya,
        ])->forceDelete();

        Pameran::whereIn('id_pameran', [
            $this->pameranMinat->id_pameran,
            $this->pameranLuar->id_pameran,
        ])->forceDelete();

        $this->pengguna->forceDelete();

        Kategori::whereIn('id_kategori', [
            $this->kategoriMinat->id_kategori,
            $this->kategoriLuar->id_kategori,
        ])->delete();

        DB::setDefaultConnection('mysql');

        parent::tearDown();
    }

    public function test_discovery_karya_hanya_menampilkan_kategori_di_luar_minat(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=karya');

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaLuar->id_karya, $ids);
        $this->assertNotContains($this->karyaMinat->id_karya, $ids);
    }

    public function test_discovery_karya_fallback_ke_semua_kategori_jika_luar_minat_kosong(): void
    {
        $semuaKategori = Kategori::whereRaw('LOWER(kategori) <> ?', ['others'])
            ->pluck('id_kategori')
            ->all();

        $this->pasangMinat($semuaKategori);

        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=karya');

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaMinat->id_karya, $ids);
    }

    public function test_discovery_karya_tanpa_minat_menampilkan_semua_kategori(): void
    {
        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=karya');

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaMinat->id_karya, $ids);
        $this->assertContains($this->karyaLuar->id_karya, $ids);
    }

    public function test_discovery_karya_dengan_minat_others_menampilkan_semua_kategori(): void
    {
        $others = Kategori::firstOrCreate(['kategori' => 'Others']);

        $this->pasangMinat([
            $others->id_kategori,
            $this->kategoriMinat->id_kategori,
        ]);

        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=karya');

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaMinat->id_karya, $ids);
        $this->assertContains($this->karyaLuar->id_karya, $ids);
    }

    public function test_filter_kategori_eksplisit_mengalahkan_aturan_luar_minat(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'karya',
            'kategori' => [$this->kategoriMinat->kategori],
        ]);

        $response = $this->actingAs($this->pengguna)->getJson($url);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaMinat->id_karya, $ids);
    }

    public function test_filter_kategori_membatasi_hasil_discovery(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'karya',
            'kategori' => [$this->kategoriLuar->kategori],
        ]);

        $response = $this->actingAs($this->pengguna)->getJson($url);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaLuar->id_karya, $ids);
        $this->assertNotContains($this->karyaMinat->id_karya, $ids);
    }

    public function test_discovery_pameran_hanya_yang_punya_karya_luar_minat(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=pameran');

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_pameran');

        $this->assertContains($this->pameranLuar->id_pameran, $ids);
        $this->assertNotContains($this->pameranMinat->id_pameran, $ids);
    }

    public function test_filter_kategori_pameran_berdasarkan_karya(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'pameran',
            'kategori' => [$this->kategoriLuar->kategori],
        ]);

        $response = $this->actingAs($this->pengguna)->getJson($url);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_pameran');

        $this->assertContains($this->pameranLuar->id_pameran, $ids);
        $this->assertNotContains($this->pameranMinat->id_pameran, $ids);
    }

    public function test_pencarian_keyword_tetap_lintas_kategori(): void
    {
        $this->pasangMinat([$this->kategoriMinat->id_kategori]);

        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=karya&q='.$this->keyword);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaMinat->id_karya, $ids);
        $this->assertContains($this->karyaLuar->id_karya, $ids);
    }

    public function test_pencarian_keyword_dengan_filter_kategori_hanya_mengembalikan_kategori_itu(): void
    {
        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'karya',
            'q' => $this->keyword,
            'kategori' => [$this->kategoriLuar->kategori],
        ]);

        $response = $this->actingAs($this->pengguna)->getJson($url);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_karya');

        $this->assertContains($this->karyaLuar->id_karya, $ids);
        $this->assertNotContains($this->karyaMinat->id_karya, $ids);
    }

    public function test_pencarian_keyword_pameran_dengan_filter_kategori(): void
    {
        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'pameran',
            'q' => $this->keyword,
            'kategori' => [$this->kategoriLuar->kategori],
        ]);

        $response = $this->actingAs($this->pengguna)->getJson($url);

        $response->assertOk();

        $ids = array_column($response->json('data.data'), 'id_pameran');

        $this->assertContains($this->pameranLuar->id_pameran, $ids);
        $this->assertNotContains($this->pameranMinat->id_pameran, $ids);
    }

    public function test_pameran_mengembalikan_kategori_perwakilan_dari_karya(): void
    {
        $response = $this->actingAs($this->pengguna)
            ->getJson('/api/pencarian?tipe=pameran');

        $response->assertOk();

        $items = collect($response->json('data.data'));
        $pameranLuar = $items->firstWhere('id_pameran', $this->pameranLuar->id_pameran);

        $this->assertNotNull($pameranLuar);
        $this->assertSame($this->kategoriLuar->kategori, $pameranLuar['kategori']);
    }

    public function test_kategori_tidak_valid_menghasilkan_error_validasi(): void
    {
        $url = '/api/pencarian?'.http_build_query([
            'tipe' => 'karya',
            'kategori' => ['Kategori Tidak Ada'],
        ]);

        $this->actingAs($this->pengguna)
            ->getJson($url)
            ->assertUnprocessable()
            ->assertJsonValidationErrors('kategori.0');
    }

    private function pasangMinat(array $idKategori): void
    {
        $pivot = collect($idKategori)
            ->mapWithKeys(fn ($id) => [$id => ['bobot' => 1.0]])
            ->all();

        $this->pengguna->kategori()->sync($pivot);
    }
}
