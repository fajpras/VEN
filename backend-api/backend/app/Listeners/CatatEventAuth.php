<?php

namespace App\Listeners;

use App\Services\LogSistem;
use Illuminate\Auth\Events\Failed;
use Illuminate\Auth\Events\Login;
use Illuminate\Auth\Events\Logout;
use Illuminate\Auth\Events\PasswordReset;
use Illuminate\Events\Dispatcher;

class CatatEventAuth
{
    public function login(Login $e): void
    {
        LogSistem::keamanan(
            'login_berhasil',
            "Otentikasi berhasil untuk {$e->user->nama} ({$e->user->email}); sesi diterbitkan sesuai hak akses peran.",
            200,
            $e->user->id_pengguna,
        );
    }

    public function gagal(Failed $e): void
    {
        $alasan = $e->user ? 'kata sandi tidak cocok' : 'identitas akun tidak terdaftar';
        LogSistem::keamanan(
            'login_gagal',
            "Verifikasi kredensial ditolak ({$alasan}); akses diblokir untuk mitigasi brute-force.",
            401,
            $e->user?->id_pengguna,
        );
    }

    public function logout(Logout $e): void
    {
        $nama = $e->user?->nama ?? 'Pengguna';
        LogSistem::keamanan(
            'logout',
            "Sesi aktif diakhiri secara sah dan token otentikasi dicabut oleh {$nama}.",
            200,
            $e->user?->id_pengguna,
        );
    }

    public function resetSandi(PasswordReset $e): void
    {
        LogSistem::keamanan(
            'reset_kata_sandi',
            "Pembaruan kredensial kata sandi berhasil diverifikasi untuk {$e->user->email}.",
            200,
            $e->user->id_pengguna,
        );
    }

    public function subscribe(Dispatcher $events): array
    {
        return [
            Login::class => 'login',
            Failed::class => 'gagal',
            Logout::class => 'logout',
            PasswordReset::class => 'resetSandi',
        ];
    }
}
