<?php

use App\Console\Commands\BackupDatabase;
use App\Console\Commands\EnkripsiGoogleId;
use App\Console\Commands\InitializeDatabase;
use App\Console\Commands\KirimPengingatUndanganCommand;
use App\Console\Commands\RestoreDatabase;
use App\Console\Commands\SetupDatabase;
use App\Console\Commands\UjiTransaksiPameran;
use App\Exceptions\PenanganKesalahanApi;
use App\Http\Middleware\GunakanKoneksiAdmin;
use App\Http\Middleware\HanyaAdmin;
use App\Http\Middleware\PilihKoneksi;
use App\Http\Middleware\SinkronBahasaApi;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Illuminate\Routing\Middleware\SubstituteBindings;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withCommands([
        BackupDatabase::class,
        RestoreDatabase::class,
        SetupDatabase::class,
        InitializeDatabase::class,
        EnkripsiGoogleId::class,
        KirimPengingatUndanganCommand::class,
        UjiTransaksiPameran::class,
    ])
    ->withMiddleware(function (Middleware $middleware): void {
        // Request API tidak boleh dialihkan ke halaman login web. Bila tidak ada
        // tujuan redirect (null), middleware auth akan melempar 401 yang kemudian
        // dirender sebagai JSON oleh PenanganKesalahanApi.
        $middleware->redirectGuestsTo(
            fn (Request $request) => $request->is('api', 'api/*') ? null : route('admin.login'),
        );

        $middleware->api(prepend: [
            SinkronBahasaApi::class,
        ]);

        $middleware->alias([
            'admin' => HanyaAdmin::class,
            'admin.db' => GunakanKoneksiAdmin::class,
        ]);


        $middleware->prependToPriorityList(SubstituteBindings::class, PilihKoneksi::class);
        $middleware->prependToPriorityList(SubstituteBindings::class, GunakanKoneksiAdmin::class);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api', 'api/*') || $request->expectsJson(),
        );

        $exceptions->render(
            fn (Throwable $e, Request $request) => PenanganKesalahanApi::render($e, $request),
        );
    })
    ->create();
