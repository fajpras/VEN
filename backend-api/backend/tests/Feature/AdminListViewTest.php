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
            'admin.logs',
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
            ->assertSee('Log Aktivitas')
            ->assertSee('Log Keamanan')
            ->assertSee('Menambah pameran untuk uji tab log aktivitas')
            ->assertDontSee('login_uji_keamanan_tab');

        // 2. Tab Keamanan (?tab=keamanan)
        $responseKeamanan = $this->actingAs($admin, 'web')
            ->get(route('admin.logs', ['tab' => 'keamanan']))
            ->assertOk()
            ->assertSee('Log Aktivitas')
            ->assertSee('Log Keamanan')
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
}
