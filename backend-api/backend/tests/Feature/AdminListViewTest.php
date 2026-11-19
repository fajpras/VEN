<?php

namespace Tests\Feature;

use App\Models\Pengguna;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

/**
 * Smoke test: setiap halaman daftar admin harus bisa dirender dalam mode
 * Card maupun List, dengan jumlah item per halaman hingga 200.
 */
class AdminListViewTest extends TestCase
{
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
