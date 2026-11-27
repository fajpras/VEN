<?php

namespace Tests\Feature;

use App\Http\Middleware\PilihKoneksi;
use App\Models\Karya;
use App\Models\Kategori;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class DiskusiUndanganTest extends TestCase
{
    use DatabaseTransactions;

    protected Pengguna $pengundang;
    protected Pengguna $penyelenggara;
    protected Model3D $model3d;
    protected Pameran $pameran;
    protected Kategori $kategori;

    protected function setUp(): void
    {
        parent::setUp();
        $this->withoutMiddleware(PilihKoneksi::class);

        $this->pengundang = Pengguna::factory()->create([
            'role' => 'pengguna',
            'status' => 'aktif',
        ]);

        $this->penyelenggara = Pengguna::factory()->create([
            'role' => 'pengguna',
            'status' => 'aktif',
        ]);

        $this->model3d = Model3D::firstOrCreate(
            ['nama' => 'Template Test Hall'],
            ['jenis' => 'hall', 'file' => 'models/hall.glb', 'kapasitas' => '1', 'gambar' => 'models/hall.png']
        );

        $this->pameran = Pameran::create([
            'id_pengguna' => $this->penyelenggara->id_pengguna,
            'id_model' => $this->model3d->id_model,
            'judul' => 'Pameran Virtual Uji',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        $this->kategori = Kategori::firstOrCreate(['kategori' => 'Lukisan']);
    }

    public function test_dapat_mengajukan_undangan_dengan_id_pameran_dan_otomatis_buat_ruangan_privat(): void
    {
        $waktu = now()->addDays(2)->format('Y-m-d H:i:s');

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktu,
            'catatan' => 'Halo, saya ingin berdiskusi mengenai proyek kolaborasi.',
        ]);

        $response->assertCreated();
        $response->assertJsonPath('data.status', 'diproses');
        $response->assertJsonPath('data.catatan', 'Halo, saya ingin berdiskusi mengenai proyek kolaborasi.');

        $this->assertDatabaseHas('ruangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $this->assertDatabaseHas('undangan', [
            'id_pengguna' => $this->pengundang->id_pengguna,
            'status' => 'diproses',
        ]);
    }

    public function test_dapat_mengajukan_undangan_dengan_id_karya(): void
    {
        $karya = Karya::create([
            'id_pengguna' => $this->penyelenggara->id_pengguna,
            'id_pameran' => $this->pameran->id_pameran,
            'id_kategori' => $this->kategori->id_kategori,
            'judul' => 'Karya Lukis Eksperimental',
            'jenis' => 'poster',
            'deskripsi' => 'Deskripsi lukisan',
            'file' => 'karya/lukisan_test.jpg',
        ]);

        $waktu = now()->addDays(3)->format('Y-m-d H:i:s');

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_karya' => $karya->id_karya,
            'waktu' => $waktu,
            'catatan' => 'Diskusi khusus karya ini',
        ]);

        $response->assertCreated();
        $response->assertJsonPath('data.status', 'diproses');
    }

    public function test_gagal_mengajukan_undangan_untuk_pameran_sendiri(): void
    {
        $waktu = now()->addDays(1)->format('Y-m-d H:i:s');

        $response = $this->actingAs($this->penyelenggara)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktu,
        ]);

        $response->assertStatus(422);
    }

    public function test_gagal_mengajukan_undangan_dengan_jadwal_lewat(): void
    {
        $waktuLewat = now()->subHours(2)->format('Y-m-d H:i:s');

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktuLewat,
        ]);

        $response->assertStatus(422);
    }

    public function test_gagal_mengajukan_undangan_duplikat_yang_masih_aktif(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2),
            'catatan' => 'Undangan pertama',
            'status' => 'diproses',
        ]);

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => now()->addDays(3)->format('Y-m-d H:i:s'),
        ]);

        $response->assertStatus(409);
    }

    public function test_penyelenggara_dapat_menyetujui_undangan(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2),
            'catatan' => 'Permohonan diskusi',
            'status' => 'diproses',
        ]);

        $response = $this->actingAs($this->penyelenggara)->patchJson("/api/undangan/{$undangan->id_undangan}", [
            'status' => 'disetujui',
        ]);

        $response->assertOk();
        $response->assertJsonPath('data.status', 'disetujui');
        $this->assertNotNull($response->json('data.tautan'));

        $this->assertDatabaseHas('undangan', [
            'id_undangan' => $undangan->id_undangan,
            'status' => 'disetujui',
        ]);
    }

    public function test_penyelenggara_dapat_menolak_undangan(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2),
            'catatan' => 'Permohonan diskusi',
            'status' => 'diproses',
        ]);

        $response = $this->actingAs($this->penyelenggara)->patchJson("/api/undangan/{$undangan->id_undangan}", [
            'status' => 'ditolak',
            'alasan' => 'Mohon maaf jadwal penuh',
        ]);

        $response->assertOk();
        $response->assertJsonPath('data.status', 'ditolak');
        $response->assertJsonPath('data.alasan', 'Mohon maaf jadwal penuh');
        $response->assertJsonPath('data.catatan', 'Permohonan diskusi');

        $this->assertDatabaseHas('undangan', [
            'id_undangan' => $undangan->id_undangan,
            'status' => 'ditolak',
            'catatan' => 'Permohonan diskusi',
            'alasan' => 'Mohon maaf jadwal penuh',
        ]);
    }

    public function test_index_undangan_memuat_relasi_pameran_dan_pengguna(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2),
            'catatan' => 'Catatan uji',
            'status' => 'diproses',
        ]);

        $responsePengundang = $this->actingAs($this->pengundang)
            ->getJson('/api/undangan?sebagai=pengundang&status=diproses');

        $responsePengundang->assertOk();
        $this->assertNotEmpty($responsePengundang->json('data.data'));
        $item = $responsePengundang->json('data.data.0');
        $this->assertEquals('Pameran Virtual Uji', $item['ruangan']['pameran']['judul']);
        $this->assertEquals('pameran/banner/test.jpg', $item['ruangan']['pameran']['banner']);

        $responsePenyelenggara = $this->actingAs($this->penyelenggara)
            ->getJson('/api/undangan?sebagai=penyelenggara&status=diproses');

        $responsePenyelenggara->assertOk();
        $this->assertNotEmpty($responsePenyelenggara->json('data.data'));
        $itemPenyelenggara = $responsePenyelenggara->json('data.data.0');
        $this->assertEquals($this->pengundang->id_pengguna, $itemPenyelenggara['pengguna']['id_pengguna']);
    }

    public function test_waktu_dengan_offset_disimpan_sebagai_utc(): void
    {
        $waktuLokal = now()->addDays(2)->setTimezone('Asia/Jakarta');

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => $waktuLokal->format('Y-m-d\TH:i:sP'),
        ]);

        $response->assertCreated();

        $tersimpan = Undangan::query()->latest('id_undangan')->firstOrFail();

        $this->assertSame(
            $waktuLokal->copy()->utc()->format('Y-m-d H:i:s'),
            $tersimpan->waktu->format('Y-m-d H:i:s')
        );
        $this->assertSame('+00:00', $tersimpan->waktu->format('P'));
    }

    public function test_dapat_mengajukan_ulang_setelah_undangan_disetujui_lewat(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->subMinutes(30),
            'status' => 'disetujui',
        ]);

        $response = $this->actingAs($this->pengundang)->postJson('/api/undangan', [
            'id_pameran' => $this->pameran->id_pameran,
            'waktu' => now()->addDays(2)->format('Y-m-d H:i:s'),
        ]);

        $response->assertCreated();
    }

    public function test_tidak_dapat_menyetujui_undangan_yang_sudah_lewat(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->subMinute(),
            'status' => 'diproses',
        ]);

        $response = $this->actingAs($this->penyelenggara)->patchJson("/api/undangan/{$undangan->id_undangan}", [
            'status' => 'disetujui',
        ]);

        $response->assertStatus(409);
        $response->assertJsonPath('message', 'Jadwal undangan sudah lewat.');
    }

    public function test_tidak_dapat_menyetujui_slot_yang_bentrok(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $pengundangLain = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);

        Undangan::forceCreate([
            'id_pengguna' => $pengundangLain->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2),
            'status' => 'disetujui',
            'tautan' => 'tautan-lain',
        ]);

        $undangan = Undangan::forceCreate([
            'id_pengguna' => $this->pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addDays(2)->addMinutes(30),
            'status' => 'diproses',
        ]);

        $response = $this->actingAs($this->penyelenggara)->patchJson("/api/undangan/{$undangan->id_undangan}", [
            'status' => 'disetujui',
        ]);

        $response->assertStatus(409);
        $response->assertJsonPath('message', 'Slot jadwal pada ruangan ini sudah terisi.');
    }

    public function test_index_mendukung_per_page_dan_urutan_waktu(): void
    {
        $ruangan = Ruangan::firstOrCreate([
            'id_pameran' => $this->pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        foreach ([3, 1, 2] as $hari) {
            Undangan::forceCreate([
                'id_pengguna' => $this->pengundang->id_pengguna,
                'id_ruangan' => $ruangan->id_ruangan,
                'waktu' => now()->addDays($hari),
                'status' => 'diproses',
            ]);
        }

        $response = $this->actingAs($this->pengundang)
            ->getJson('/api/undangan?sebagai=pengundang&per_page=2');

        $response->assertOk();
        $response->assertJsonCount(2, 'data.data');
        $response->assertJsonPath('data.last_page', 2);

        $data = $response->json('data.data');
        $this->assertTrue(Carbon::parse($data[0]['waktu'])->lt(Carbon::parse($data[1]['waktu'])));
        $this->assertTrue(Carbon::parse($data[0]['waktu'])->isSameDay(now()->addDay()));
    }
}
