<?php

namespace Tests\Feature;

use App\Models\Pengguna;
use Illuminate\Foundation\Testing\DatabaseTransactions;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class SingleSessionLoginTest extends TestCase
{
    use DatabaseTransactions;

    public function test_login_baru_mencabut_token_sesi_sebelumnya(): void
    {
        $pengguna = Pengguna::create([
            'nama' => 'Pengguna Uji Sesi',
            'nama_panggilan' => 'uji_single_session_' . random_int(1000, 9999),
            'email' => 'single_session_' . random_int(1000, 9999) . '@ven.test',
            'kata_sandi' => 'Rahasia123!',
            'status' => 'aktif',
        ]);

        // Perangkat A login
        $loginA = $this->postJson('/api/auth/masuk', [
            'identitas' => $pengguna->email,
            'kata_sandi' => 'Rahasia123!',
        ]);
        $loginA->assertOk();
        $tokenA = $loginA->json('data.token');
        $this->assertNotEmpty($tokenA);

        // Perangkat A bisa akses profil
        $this->getJson('/api/profil', ['Authorization' => 'Bearer ' . $tokenA])
            ->assertOk();

        // Kembalikan koneksi default agar DatabaseTransactions tetap melihat data uji
        \Illuminate\Support\Facades\DB::setDefaultConnection('mysql');

        // Perangkat B login ke akun yang sama
        $loginB = $this->postJson('/api/auth/masuk', [
            'identitas' => $pengguna->email,
            'kata_sandi' => 'Rahasia123!',
        ]);
        $loginB->assertOk();
        $tokenB = $loginB->json('data.token');
        $this->assertNotEmpty($tokenB);
        $this->assertNotSame($tokenA, $tokenB);

        // Hanya tersisa 1 token aktif di database (milik Perangkat B)
        $this->assertSame(1, $pengguna->tokens()->count());

        // Perangkat A otomatis tidak valid (401 Unauthorized)
        app('auth')->forgetGuards();
        \Illuminate\Support\Facades\DB::setDefaultConnection('mysql');
        $this->getJson('/api/profil', ['Authorization' => 'Bearer ' . $tokenA])
            ->assertStatus(401);

        // Perangkat B tetap valid (200 OK)
        app('auth')->forgetGuards();
        \Illuminate\Support\Facades\DB::setDefaultConnection('mysql');
        $this->getJson('/api/profil', ['Authorization' => 'Bearer ' . $tokenB])
            ->assertOk();
    }
}
