<?php

namespace Tests\Unit;

use App\Support\AdminListOptions;
use Illuminate\Http\Request;
use Tests\TestCase;

/**
 * Uji allowlist opsi tampilan daftar admin (per_page & mode Card/List).
 * Tidak menyentuh basis data — cukup jalankan:
 *   php vendor/bin/phpunit --testdox tests/Unit/AdminListOptionsTest.php
 */
class AdminListOptionsTest extends TestCase
{
    public function test_per_page_menerima_nilai_dari_allowlist(): void
    {
        foreach (AdminListOptions::PER_PAGE_OPTIONS as $option) {
            $request = Request::create('/admin/posts', 'GET', ['per_page' => (string) $option]);

            $this->assertSame($option, AdminListOptions::perPage($request));
        }
    }

    public function test_per_page_fallback_ke_default_jika_tidak_valid(): void
    {
        $this->assertSame(12, AdminListOptions::perPage(Request::create('/admin/posts')));

        $this->assertSame(
            12,
            AdminListOptions::perPage(Request::create('/admin/posts', 'GET', ['per_page' => '9999']))
        );

        $this->assertSame(
            12,
            AdminListOptions::perPage(Request::create('/admin/posts', 'GET', ['per_page' => 'abc']))
        );
    }

    public function test_per_page_mendukung_seratus_data_per_halaman(): void
    {
        $request = Request::create('/admin/logs', 'GET', ['per_page' => '100']);

        $this->assertSame(100, AdminListOptions::perPage($request));
        $this->assertContains(100, AdminListOptions::PER_PAGE_OPTIONS);
    }

    public function test_view_mode_hanya_menerima_card_atau_list(): void
    {
        $this->assertSame(
            'card',
            AdminListOptions::viewMode(Request::create('/admin/posts', 'GET', ['view' => 'card']))
        );

        $this->assertSame(
            'list',
            AdminListOptions::viewMode(Request::create('/admin/posts', 'GET', ['view' => 'list']))
        );

        $this->assertSame(
            'list',
            AdminListOptions::viewMode(Request::create('/admin/posts', 'GET', ['view' => 'grid']))
        );

        $this->assertSame('list', AdminListOptions::viewMode(Request::create('/admin/posts')));
    }
}
