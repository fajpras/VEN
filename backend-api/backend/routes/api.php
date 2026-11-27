<?php

use App\Http\Controllers\Admin;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\Pengguna;
use App\Http\Middleware\AkunAktif;
use App\Http\Middleware\CatatKeamanan;
use App\Http\Middleware\HanyaAdmin;
use App\Http\Middleware\PilihKoneksi;
use Illuminate\Support\Facades\Route;

Route::middleware(CatatKeamanan::class)->group(function () {
    
    Route::prefix('auth')->controller(AuthController::class)->group(function () {
        Route::post('daftar/kirim-otp', 'kirimOtpRegistrasi')->middleware('throttle:5,1');
        Route::post('daftar', 'register')->middleware('throttle:5,1');
        Route::post('masuk', 'login')->middleware('throttle:5,1');
        Route::post('google/daftar', 'registerGoogle')->middleware('throttle:10,1');
        Route::post('google/masuk', 'loginGoogle')->middleware('throttle:10,1');
        Route::post('lupa-kata-sandi', 'lupaKataSandi')->middleware('throttle:3,1');
        Route::post('reset-kata-sandi', 'resetKataSandi')->middleware('throttle:5,1');
        Route::post('keluar', 'logout')->middleware('auth:sanctum');
    });

    Route::get('karya/{karya}/berkas', [Pengguna\KaryaController::class, 'berkas'])
        ->whereUuid('karya')->middleware(['signed', 'throttle:120,1'])->name('karya.berkas');

    Route::middleware(['auth:sanctum', AkunAktif::class, PilihKoneksi::class, 'throttle:120,1'])->group(function () {
        Route::controller(Pengguna\ProfilController::class)->prefix('profil')->group(function () {
            Route::get('/', 'show');
            Route::put('/', 'update');
            Route::post('foto', 'updateFoto');
            Route::post('email/kirim-otp', 'kirimOtpGantiEmail')->middleware('throttle:5,1');
            Route::put('email', 'gantiEmail')->middleware('throttle:5,1');
            Route::put('kata-sandi', 'updatePassword');
            Route::put('status', 'updateStatus');
            Route::get('kategori', 'kategoriTersedia');
            Route::get('minat', 'minat');
            Route::put('minat', 'updateMinat');
            Route::delete('minat/{kategori}', 'destroyMinat');
        });

        Route::controller(Pengguna\PameranController::class)->group(function () {
            Route::get('model-3d', 'templates');
            Route::get('model-3d/{model3d}', 'templateShow');
            Route::get('exhibition_templates', 'templates');
            Route::get('pameran', 'index');
            Route::get('pameran/rekomendasi', 'rekomendasi');
            Route::post('pameran', 'store');
            Route::post('pameran/banner', 'uploadBanner');
            Route::post('exhibitions', 'storeFromUnity');
            Route::put('exhibitions/{pameran}', 'updateFromUnity')->whereUuid('pameran');
            Route::get('exhibitions/{pameran}/room-dto', 'roomDto')->whereUuid('pameran');
            Route::get('pameran/{pameran}', 'show')->whereUuid('pameran');
            Route::get('pameran/{pameran}/room-dto', 'roomDto')->whereUuid('pameran');
            Route::match(['put', 'patch', 'post'], 'pameran/{pameran}', 'update')->whereUuid('pameran');
            Route::delete('pameran/{pameran}', 'destroy')->whereUuid('pameran');
            Route::post('pameran/{pameran}/ruangan', 'storeRuangan')->whereUuid('pameran');
        });

        Route::controller(Pengguna\KaryaController::class)->group(function () {
            Route::get('karya', 'index');
            Route::get('karya/rekomendasi', 'rekomendasi');
            Route::post('karya', 'store');
            Route::post('karya/upload-media', 'uploadMedia');
            Route::get('karya/{karya}', 'show')->whereUuid('karya');

            Route::match(['put', 'patch', 'post'], 'karya/{karya}', 'update')->whereUuid('karya');
            Route::delete('karya/{karya}', 'destroy')->whereUuid('karya');
            Route::put('karya/{karya}/posisi', 'updatePosisiObjek')->whereUuid('karya');
        });

        Route::controller(Pengguna\DiskusiController::class)->group(function () {
            Route::get('undangan', 'index');
            Route::post('undangan', 'store');
            Route::get('undangan/tautan/{token}', 'aksesTautan')->where('token', '[A-Za-z0-9_-]+');
            Route::get('undangan/{undangan}', 'show')->whereNumber('undangan');
            Route::get('undangan/{undangan}/status', 'status')->whereNumber('undangan');
            Route::patch('undangan/{undangan}', 'tanggapi')->whereNumber('undangan');
            Route::get('ruangan/{ruangan}/antrean', 'antrean')->whereNumber('ruangan');
            Route::patch('ruangan/{ruangan}/antrean/{permintaan}', 'tanggapiAntrean')->whereNumber('ruangan')->whereNumber('permintaan');
            Route::get('ruangan/{ruangan}/voice', 'voiceToken')->whereNumber('ruangan')->middleware('throttle:30,1');
            Route::get('ruangan/{ruangan}/room-dto', 'roomDto')->whereNumber('ruangan');
        });

        Route::controller(Pengguna\RoomServerController::class)->middleware('throttle:30,1')->group(function () {
            Route::post('room-server/{roomKey}', 'byRoomKey')->where('roomKey', '[A-Za-z0-9_-]+');
            Route::post('pameran/{pameran}/server', 'pameran')->whereUuid('pameran');
            Route::post('ruangan/{ruangan}/server', 'ruangan')->whereNumber('ruangan');
        });

        Route::post('ai/tanya', [Pengguna\AIChatController::class, 'ask'])->middleware('throttle:10,1');
        Route::get('pencarian', [Pengguna\PencarianController::class, 'search']);
        Route::get('pengguna/{pengguna}', [Pengguna\ProfilController::class, 'detailPengguna'])->whereUuid('pengguna');

        Route::prefix('admin')->middleware(HanyaAdmin::class)->group(function () {
            Route::controller(Admin\PenggunaController::class)->group(function () {
                Route::get('pengguna', 'index');
                Route::get('pengguna/{pengguna}', 'show')->whereUuid('pengguna');
                Route::post('pengguna/{pengguna}/sanksi', 'sanksiStore')->whereUuid('pengguna');
                Route::get('sanksi', 'sanksiIndex');
                Route::get('sanksi/{sanksi}', 'sanksiShow');
                Route::patch('sanksi/{sanksi}', 'sanksiUpdate');
                Route::delete('sanksi/{sanksi}', 'sanksiCabut');
            });

            Route::controller(Admin\ModerasiController::class)->group(function () {
                Route::get('karya', 'karyaIndex');
                Route::get('karya/{karya}', 'karyaShow')->whereUuid('karya')->withTrashed();
                Route::delete('karya/{karya}', 'karyaDestroy')->whereUuid('karya');
                Route::post('karya/{karya}/pulihkan', 'karyaRestore')->whereUuid('karya')->withTrashed();
                Route::get('pameran', 'pameranIndex');
                Route::get('pameran/{pameran}', 'pameranShow')->whereUuid('pameran')->withTrashed();
                Route::delete('pameran/{pameran}', 'pameranDestroy')->whereUuid('pameran');
                Route::post('pameran/{pameran}/pulihkan', 'pameranRestore')->whereUuid('pameran')->withTrashed();
            });

            Route::controller(Admin\LogController::class)->prefix('log')->group(function () {
                Route::get('aktivitas', 'aktivitasIndex');
                Route::get('aktivitas/{log}', 'aktivitasShow');
                Route::get('keamanan', 'keamananIndex');
                Route::get('keamanan/{log}', 'keamananShow');
            });
        });
    });
});
