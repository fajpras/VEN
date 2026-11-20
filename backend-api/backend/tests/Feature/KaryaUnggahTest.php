<?php

namespace Tests\Feature;

use App\Models\Karya;
use App\Models\Kategori;
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
}
