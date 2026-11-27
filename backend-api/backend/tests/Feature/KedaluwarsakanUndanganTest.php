<?php

namespace Tests\Feature;

use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Tests\TestCase;

class KedaluwarsakanUndanganTest extends TestCase
{
    use DatabaseTransactions;

    public function test_undangan_diproses_yang_lewat_ditandai_ditolak(): void
    {
        $pemilik = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);
        $pengundang = Pengguna::factory()->create(['role' => 'pengguna', 'status' => 'aktif']);

        $model = Model3D::firstOrCreate(
            ['nama' => 'Template Test Hall'],
            ['jenis' => 'hall', 'file' => 'models/hall.glb', 'kapasitas' => '1', 'gambar' => 'models/hall.png']
        );

        $pameran = Pameran::create([
            'id_pengguna' => $pemilik->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => 'Pameran Kedaluwarsa',
            'banner' => 'pameran/banner/test.jpg',
            'warna' => '#1565C0',
            'tipe' => 'kecil',
        ]);

        $ruangan = Ruangan::create([
            'id_pameran' => $pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        $lewat = Undangan::forceCreate([
            'id_pengguna' => $pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->subMinutes(20),
            'status' => 'diproses',
        ]);

        $akanDatang = Undangan::forceCreate([
            'id_pengguna' => $pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addMinutes(30),
            'status' => 'diproses',
        ]);

        $this->artisan('ven:kedaluwarsakan-undangan')->assertExitCode(0);

        $this->assertDatabaseHas('undangan', [
            'id_undangan' => $lewat->id_undangan,
            'status' => 'ditolak',
            'alasan' => 'Jadwal terlewat tanpa tanggapan.',
        ]);

        $this->assertDatabaseHas('undangan', [
            'id_undangan' => $akanDatang->id_undangan,
            'status' => 'diproses',
        ]);
    }
}
