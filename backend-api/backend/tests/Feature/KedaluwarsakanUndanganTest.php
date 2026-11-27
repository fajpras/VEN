<?php

namespace Tests\Feature;

use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use App\Models\Undangan;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class KedaluwarsakanUndanganTest extends TestCase
{
    use RefreshDatabase;

    private function buatRuanganPrivat(): array
    {
        $pemilik = Pengguna::create([
            'google_id' => 'google-exp-pemilik',
            'nama' => 'Pemilik',
            'nama_panggilan' => 'pemilik_exp',
            'email' => 'pemilik_exp@example.com',
            'sandi' => Hash::make('rahasia123'),
            'peran' => 'pengguna',
        ]);

        $pengundang = Pengguna::create([
            'google_id' => 'google-exp-tamu',
            'nama' => 'Tamu',
            'nama_panggilan' => 'tamu_exp',
            'email' => 'tamu_exp@example.com',
            'sandi' => Hash::make('rahasia123'),
            'peran' => 'pengguna',
        ]);

        $model = Model3D::create([
            'nama' => 'Hall Kecil',
            'jenis' => 'hall',
            'file' => 'models/template01.glb',
            'gambar' => 'models/template01.png',
            'kapasitas' => '9',
        ]);

        $pameran = Pameran::create([
            'id_pengguna' => $pemilik->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => 'Pameran Exp',
            'banner' => 'banner/test.png',
            'warna' => '#FFFFFF',
            'tipe' => 'kecil',
        ]);

        $ruangan = Ruangan::create([
            'id_pameran' => $pameran->id_pameran,
            'jenis' => 'privat',
        ]);

        return [$ruangan, $pengundang];
    }

    public function test_undangan_diproses_lewat_jadwal_ditandai_ditolak_dengan_alasan(): void
    {
        [$ruangan, $pengundang] = $this->buatRuanganPrivat();

        $lewat = Undangan::forceCreate([
            'id_pengguna' => $pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->subMinutes(30),
            'durasi_menit' => 30,
            'catatan' => 'Catatan asli pengundang',
            'status' => 'diproses',
        ]);

        $mendatang = Undangan::forceCreate([
            'id_pengguna' => $pengundang->id_pengguna,
            'id_ruangan' => $ruangan->id_ruangan,
            'waktu' => now()->addHours(2),
            'durasi_menit' => 30,
            'catatan' => 'Sesi mendatang',
            'status' => 'diproses',
        ]);

        $this->artisan('ven:kedaluwarsakan-undangan')->assertExitCode(0);

        $lewat->refresh();
        $mendatang->refresh();

        $this->assertSame('ditolak', $lewat->status);
        $this->assertSame('Jadwal terlewat tanpa tanggapan.', $lewat->alasan);
        $this->assertSame('Catatan asli pengundang', $lewat->catatan);
        $this->assertSame('diproses', $mendatang->status);
    }
}
