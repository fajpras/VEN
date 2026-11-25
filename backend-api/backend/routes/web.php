<?php

use App\Http\Controllers\Admin\Web\AdminAccountController;
use App\Http\Controllers\Admin\Web\AdminArchiveController;
use App\Http\Controllers\Admin\Web\AdminAuthController;
use App\Http\Controllers\Admin\Web\AdminExhibitionController;
use App\Http\Controllers\Admin\Web\AdminLogController;
use App\Http\Controllers\Admin\Web\AdminPostController;
use App\Http\Controllers\Admin\Web\AdminSettingsController;
use App\Http\Middleware\HanyaAdminWeb;
use App\Http\Middleware\SinkronBahasaAdmin;
use Illuminate\Support\Facades\Route;

// Root publik tidak boleh membuka beranda maupun login admin.
// Akses langsung ke "/" sengaja dijawab 404 agar panel admin tidak terekspos.
Route::get('/', function () {
    abort(404);
});

// Admin Authentication Routes
Route::prefix('admin')->middleware([SinkronBahasaAdmin::class])->group(function () {
    Route::get('/login', [AdminAuthController::class, 'showLoginForm'])->name('admin.login');
    Route::post('/login', [AdminAuthController::class, 'login'])->middleware('throttle:5,1')->name('admin.login.submit');

    // Admin Protected Views
    Route::middleware([HanyaAdminWeb::class, 'admin.db'])->group(function () {
        Route::post('/logout', [AdminAuthController::class, 'logout'])->name('admin.logout');
        Route::get('/locale/{locale}', [AdminSettingsController::class, 'switchLocale'])->name('admin.locale');

        Route::get('/', function () {
            return redirect()->route('admin.accounts');
        });

        Route::get('/accounts', [AdminAccountController::class, 'index'])->name('admin.accounts');
        Route::post('/accounts/{id}/suspend', [AdminAccountController::class, 'suspend'])->name('admin.accounts.suspend');

        Route::get('/posts', [AdminPostController::class, 'index'])->name('admin.posts');
        Route::delete('/posts/{id}', [AdminPostController::class, 'destroy'])->name('admin.posts.destroy');

        Route::get('/exhibitions', [AdminExhibitionController::class, 'index'])->name('admin.exhibitions');
        Route::delete('/exhibitions/{id}', [AdminExhibitionController::class, 'destroy'])->name('admin.exhibitions.destroy');

        Route::get('/activity-log', [AdminLogController::class, 'index'])->name('admin.logs');

        Route::get('/archive', [AdminArchiveController::class, 'index'])->name('admin.archive');
        Route::post('/archive/{type}/{id}/restore', [AdminArchiveController::class, 'restore'])->name('admin.archive.restore');

        Route::get('/settings', [AdminSettingsController::class, 'index'])->name('admin.settings');
        Route::put('/settings/email', [AdminSettingsController::class, 'updateEmail'])->name('admin.settings.email');
        Route::put('/settings/password', [AdminSettingsController::class, 'updatePassword'])->name('admin.settings.password');
    });
});
