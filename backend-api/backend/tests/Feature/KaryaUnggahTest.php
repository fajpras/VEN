<?php

namespace Tests\Feature;

use App\Models\Karya;
use App\Models\Kategori;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class KaryaUnggahTest extends TestCase
{
    private Pengguna $pengguna;

    private Kategori $kategori;

    protected function setUp(): void
    {
        parent::setUp();

        $this->assertSame(
            'ven_testing',
            DB::connection('mysql')->getDatabaseName(),
            'Test harus menggunakan database ven_testing.'
        );

        Storage::fake('local');

        $this->kategori = Kategori::firstOrCreate(['kategori' => 'Uji Unggah '.uniqid()]);

        $this->pengguna = Pengguna::factory()->create();

        Sanctum::actingAs($this->pengguna);
    }

    protected function tearDown(): void
    {
        DB::table('karya')->where('id_pengguna', $this->pengguna->id_pengguna)->delete();
        DB::table('kategori')->where('id_kategori', $this->kategori->id_kategori)->delete();

        parent::tearDown();
    }

    public function test_poster_berhasil_diunggah(): void
    {
        $response = $this->post('/api/karya', [
            'id_kategori' => $this->kategori->id_kategori,
            'judul' => 'Poster Uji',
            'jenis' => 'poster',
            'deskripsi' => 'Deskripsi poster uji.',
            'file' => UploadedFile::fake()->image('poster_uji.jpg', 800, 1200),
        ]);

        $response->assertCreated()
            ->assertJsonPath('data.judul', 'Poster Uji')
            ->assertJsonPath('data.jenis', 'poster');

        $karya = Karya::where('id_pengguna', $this->pengguna->id_pengguna)
            ->where('judul', 'Poster Uji')
            ->firstOrFail();

        Storage::disk('local')->assertExists($karya->file);
    }

    public function test_video_berhasil_diunggah(): void
    {
        $response = $this->post('/api/karya', [
            'id_kategori' => $this->kategori->id_kategori,
            'judul' => 'Video Uji',
            'jenis' => 'video',
            'deskripsi' => 'Deskripsi video uji.',
            'file' => UploadedFile::fake()->create('video_uji.mp4', 512, 'video/mp4'),
        ]);

        $response->assertCreated()
            ->assertJsonPath('data.jenis', 'video');
    }

    public function test_deskripsi_wajib_diisi(): void
    {
        $response = $this->postJson('/api/karya', [
            'id_kategori' => $this->kategori->id_kategori,
            'judul' => 'Poster Uji',
            'jenis' => 'poster',
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors('deskripsi');
    }

    public function test_pengguna_tanpa_autentikasi_ditolak(): void
    {
        $this->app['auth']->forgetGuards();

        $response = $this->postJson('/api/karya', [
            'id_kategori' => $this->kategori->id_kategori,
            'judul' => 'Poster Uji',
            'jenis' => 'poster',
            'deskripsi' => 'Deskripsi poster uji.',
        ]);

        $response->assertStatus(401);
    }

    public function test_unggah_dan_edit_karya_dengan_pilihan_pameran(): void
    {
        $hall = Model3D::firstOrCreate(
            ['nama' => 'Hall Test Karya'],
            ['jenis' => 'hall', 'file' => 'models/template01.glb', 'kapasitas' => '9', 'gambar' => 'models/preview.png']
        );

        $pameran = Pameran::create([
            'id_pengguna' => $this->pengguna->id_pengguna,
            'id_model' => $hall->id_model,
            'judul' => 'Pameran Uji Karya',
            'banner' => 'pameran/banner/test.png',
            'warna' => '#7C3AED',
            'tipe' => 'kecil',
        ]);

        try {
            // 1. Unggah karya baru yang ditautkan ke pameran milik sendiri
            $createRes = $this->post('/api/karya', [
                'id_kategori' => $this->kategori->id_kategori,
                'id_pameran' => $pameran->id_pameran,
                'judul' => 'Poster Tertaut Pameran',
                'jenis' => 'poster',
                'deskripsi' => 'Deskripsi awal.',
                'file' => UploadedFile::fake()->image('tertaut.jpg', 600, 800),
            ]);

            $createRes->assertCreated()
                ->assertJsonPath('data.id_pameran', $pameran->id_pameran);

            $karyaId = $createRes->json('data.id_karya');

            // 2. Edit karya: ubah judul & lepas dari pameran (id_pameran kosong)
            $updateRes = $this->post("/api/karya/{$karyaId}", [
                'judul' => 'Poster Setelah Edit',
                'id_pameran' => '',
                'deskripsi' => 'Deskripsi diperbarui.',
            ]);

            $updateRes->assertOk()
                ->assertJsonPath('data.judul', 'Poster Setelah Edit')
                ->assertJsonPath('data.id_pameran', null);

            // 3. Edit pameran info & warna
            $editPameranRes = $this->post("/api/pameran/{$pameran->id_pameran}", [
                'judul' => 'Pameran Uji Diedit',
                'warna' => '#112233',
            ]);

            $editPameranRes->assertOk()
                ->assertJsonPath('data.judul', 'Pameran Uji Diedit')
                ->assertJsonPath('data.warna', '#112233');

            // 4. Update pameran 3D dari Unity mempertahankan id_karya yang sama
            $unityEditRes = $this->putJson("/api/exhibitions/{$pameran->id_pameran}", [
                'templateId' => 'template01',
                'title' => 'Pameran 3D Diedit',
                'wallColor' => '#445566',
                'items' => [
                    [
                        'prefabId' => 'Artwork Panel',
                        'surface' => 'wall',
                        'x' => 1.0,
                        'y' => 1.5,
                        'z' => 2.0,
                        'rotY' => 90.0,
                        'karyaId' => $karyaId,
                        'artworkTitle' => 'Poster Diperbarui di 3D',
                        'artworkDescription' => 'Deskripsi dari Unity Editor',
                        'categoryId' => $this->kategori->id_kategori,
                        'contentUrl' => '/storage/karya/poster_3d.png',
                    ],
                ],
            ]);

            $unityEditRes->assertOk()
                ->assertJsonPath('data.id_pameran', $pameran->id_pameran);

            $this->assertDatabaseHas('karya', [
                'id_karya' => $karyaId,
                'id_pameran' => $pameran->id_pameran,
                'judul' => 'Poster Diperbarui di 3D',
                'file' => 'karya/poster_3d.png',
            ]);
        } finally {
            DB::table('karya')->where('id_pameran', $pameran->id_pameran)->delete();
            DB::table('objek')->where('id_pameran', $pameran->id_pameran)->delete();
            DB::table('pameran')->where('id_pameran', $pameran->id_pameran)->delete();
        }
    }
}
