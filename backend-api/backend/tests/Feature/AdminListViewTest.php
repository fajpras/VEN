<?php

namespace Tests\Feature;

use App\Models\LogAktivitas;
use App\Models\LogKeamanan;
use App\Models\Pengguna;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

/**
 * Smoke test: setiap halaman daftar admin harus bisa dirender dalam mode
 * Card maupun List, dengan jumlah item per halaman hingga 200, serta
 * pemisahan tab Log Aktivitas dan Log Keamanan.
 */
class AdminListViewTest extends TestCase
{
    use DatabaseTransactions;

    public function test_semua_halaman_daftar_bisa_dirender_dalam_mode_card_dan_list(): void
    {
        $admin = Pengguna::factory()->create([
            'kata_sandi' => Hash::make('password123'),
            'role' => 'admin',
        ]);

        $routes = [
            'admin.accounts',
            'admin.posts',
            'admin.exhibitions',
        ];

        foreach ($routes as $route) {
            foreach (['card', 'list'] as $view) {
                $this->actingAs($admin, 'web')
                    ->get(route($route, ['view' => $view, 'per_page' => 100]))
                    ->assertOk()
                    ->assertSee('Card')
                    ->assertSee('List')
                    ->assertSee('per page');
            }
        }

        // Halaman log khusus mode list (tanpa toggle Card/List)
        $this->actingAs($admin, 'web')
            ->get(route('admin.logs', ['per_page' => 100]))
            ->assertOk()
            ->assertDontSee('Mode tampilan')
            ->assertSee('per page');
    }

    public function test_halaman_log_admin_dapat_berpindah_antara_tab_aktivitas_dan_keamanan(): void
    {
        $this->withoutMiddleware(\App\Http\Middleware\GunakanKoneksiAdmin::class);

        $admin = Pengguna::factory()->create([
            'kata_sandi' => Hash::make('password123'),
            'role' => 'admin',
        ]);

        $logAktivitas = LogAktivitas::create([
            'id_pengguna' => $admin->id_pengguna,
            'entitas' => 'Pameran Test Unik',
            'jenis' => 'pameran',
            'aksi' => 'tambah',
            'status' => 'berhasil',
            'pesan' => 'Menambah pameran untuk uji tab log aktivitas',
            'ip_address' => '127.0.0.1',
        ]);

        $logKeamanan = LogKeamanan::create([
            'id_pengguna' => $admin->id_pengguna,
            'event' => 'login_uji_keamanan_tab',
            'endpoint' => '/api/auth/masuk',
            'method' => 'POST',
            'status_kode' => 200,
            'ip_address' => '127.0.0.1',
            'user_agent' => 'PHPUnit/SecurityTestAgent',
            'pesan' => 'Autentikasi admin sukses untuk pengujian tab',
        ]);

        // 1. Tab Aktivitas (Default atau ?tab=aktivitas)
        $responseAktivitas = $this->actingAs($admin, 'web')
            ->get(route('admin.logs', ['tab' => 'aktivitas']))
            ->assertOk()
            ->assertSee('Activity log')
            ->assertSee('Security log')
            ->assertSee('Menambah pameran untuk uji tab log aktivitas')
            ->assertDontSee('login_uji_keamanan_tab');

        // 2. Tab Keamanan (?tab=keamanan)
        $responseKeamanan = $this->actingAs($admin, 'web')
            ->get(route('admin.logs', ['tab' => 'keamanan']))
            ->assertOk()
            ->assertSee('Activity log')
            ->assertSee('Security log')
            ->assertSee('Actor')
            ->assertSee('Client / Agent')
            ->assertSee('login_uji_keamanan_tab')
            ->assertSee('/api/auth/masuk')
            ->assertDontSee('Menambah pameran untuk uji tab log aktivitas');
    }

    public function test_nilai_per_page_di_luar_allowlist_ditolak(): void
    {
        $admin = Pengguna::factory()->create([
            'kata_sandi' => Hash::make('password123'),
            'role' => 'admin',
        ]);

        $response = $this->actingAs($admin, 'web')
            ->get(route('admin.posts', ['per_page' => 9999]))
            ->assertOk();

        $response->assertDontSee('>9999<');
    }

    public function test_admin_media_menormalisasi_url_gambar_dan_user_agent(): void
    {
        $this->assertStringEndsWith(
            '/storage/pameran/banner/demo.png',
            (string) \App\Support\AdminMedia::imageUrl('/storage/pameran/banner/demo.png')
        );
        $this->assertStringNotContainsString(
            '/storage/storage/',
            (string) \App\Support\AdminMedia::imageUrl('/storage/pameran/banner/demo.png')
        );
        $this->assertStringEndsWith(
            '/storage/foto-profil/avatar.png',
            (string) \App\Support\AdminMedia::imageUrl('http://10.0.2.2:8000/storage/foto-profil/avatar.png')
        );
        $this->assertSame(
            'https://lh3.googleusercontent.com/a/test=s96-c',
            \App\Support\AdminMedia::imageUrl('https://lh3.googleusercontent.com/a/test=s96-c')
        );
        $this->assertSame('Android App (OkHttp)', \App\Support\AdminMedia::parseUserAgent('okhttp/4.12.0'));
        $this->assertSame('Chrome · Windows', \App\Support\AdminMedia::parseUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0 Safari/537.36'));
    }

    public function test_halaman_log_tidak_memakai_modal_detail_dan_menampilkan_user_agent(): void
    {
        $this->withoutMiddleware(\App\Http\Middleware\GunakanKoneksiAdmin::class);

        $admin = Pengguna::factory()->create([
            'kata_sandi' => Hash::make('password123'),
            'role' => 'admin',
        ]);

        LogKeamanan::create([
            'id_pengguna' => $admin->id_pengguna,
            'event' => 'login_berhasil',
            'endpoint' => '/api/auth/masuk',
            'method' => 'POST',
            'status_kode' => 200,
            'ip_address' => '127.0.0.1',
            'user_agent' => 'okhttp/4.12.0',
            'pesan' => 'Login dari aplikasi Android',
        ]);

        $this->actingAs($admin, 'web')
            ->get(route('admin.logs', ['tab' => 'keamanan']))
            ->assertOk()
            ->assertSee('Android App (OkHttp)')
            ->assertDontSee('openDetailModal');
    }

    public function test_admin_dapat_ganti_bahasa_dan_ubah_email_serta_kata_sandi(): void
    {
        $this->withoutMiddleware(\App\Http\Middleware\GunakanKoneksiAdmin::class);

        $admin = Pengguna::factory()->create([
            'email' => 'admin_settings_test@ven.test',
            'kata_sandi' => Hash::make('OldPass123!'),
            'role' => 'admin',
        ]);

        // Switch bahasa ke ID
        $this->actingAs($admin, 'web')
            ->get(route('admin.locale', ['locale' => 'id']))
            ->assertRedirect();

        $this->actingAs($admin, 'web')
            ->withSession(['admin_locale' => 'id'])
            ->get(route('admin.settings'))
            ->assertOk()
            ->assertSee('Pengaturan Admin');

        // Ubah email admin
        $this->actingAs($admin, 'web')
            ->put(route('admin.settings.email'), [
                'email' => 'admin_baru@ven.test',
                'kata_sandi_saat_ini' => 'OldPass123!',
            ])
            ->assertRedirect(route('admin.settings'));

        $this->assertSame('admin_baru@ven.test', $admin->fresh()->email);

        // Ubah password admin
        $this->actingAs($admin, 'web')
            ->put(route('admin.settings.password'), [
                'kata_sandi_saat_ini' => 'OldPass123!',
                'kata_sandi' => 'NewPass456!',
                'kata_sandi_confirmation' => 'NewPass456!',
            ])
            ->assertRedirect(route('admin.settings'));

        $this->assertTrue(Hash::check('NewPass456!', $admin->fresh()->kata_sandi));
    }

    public function test_admin_dapat_melihat_arsip_dan_memulihkan_pameran_terhapus(): void
    {
        $this->withoutMiddleware(\App\Http\Middleware\GunakanKoneksiAdmin::class);

        $admin = Pengguna::factory()->create([
            'kata_sandi' => Hash::make('password123'),
            'role' => 'admin',
        ]);

        $model = \App\Models\Model3D::first() ?? \App\Models\Model3D::create([
            'nama' => 'Ruang Uji Arsip',
            'file' => 'model/ruang-uji.glb',
            'gambar' => 'model/ruang-uji.png',
            'kapasitas' => 10,
        ]);

        $pameran = \App\Models\Pameran::create([
            'id_pengguna' => $admin->id_pengguna,
            'id_model' => $model->id_model,
            'judul' => 'Pameran Arsip Uji',
            'deskripsi' => 'Uji arsip pameran',
            'banner' => 'pameran/banner/test.png',
            'tipe' => 'kecil',
        ]);
        $pameran->delete();

        $this->actingAs($admin, 'web')
            ->get(route('admin.archive', ['tab' => 'exhibitions']))
            ->assertOk()
            ->assertSee('Pameran Arsip Uji')
            ->assertSee('Restore');

        $this->actingAs($admin, 'web')
            ->post(route('admin.archive.restore', ['type' => 'exhibitions', 'id' => $pameran->id_pameran]))
            ->assertRedirect(route('admin.archive', ['tab' => 'exhibitions']));

        $this->assertNull($pameran->fresh()->deleted_at);
    }
}
