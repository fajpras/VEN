<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\LogAktivitas;
use App\Models\LogKeamanan;
use App\Support\AdminListOptions;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AdminLogController extends Controller
{
    public function index(Request $request): View
    {
        $tab = $request->query('tab', 'aktivitas');
        if (! in_array($tab, ['aktivitas', 'keamanan'], true)) {
            $tab = 'aktivitas';
        }

        $cari = $request->query('cari');
        $aksi = $request->query('aksi');
        $status = $request->query('status');
        $method = $request->query('method');
        $statusGroup = $request->query('status_group');
        $perPage = AdminListOptions::perPage($request);
        $viewMode = AdminListOptions::viewMode($request);

        if ($tab === 'keamanan') {
            $logs = LogKeamanan::query()
                ->with(['pengguna'])
                ->when($cari, function ($q) use ($cari) {
                    $pattern = '%'.addcslashes($cari, '%_').'%';
                    $q->where(function ($sub) use ($pattern) {
                        $sub->where('event', 'like', $pattern)
                            ->orWhere('endpoint', 'like', $pattern)
                            ->orWhere('method', 'like', $pattern)
                            ->orWhere('ip_address', 'like', $pattern)
                            ->orWhere('user_agent', 'like', $pattern)
                            ->orWhere('pesan', 'like', $pattern);
                    });
                })
                ->when(in_array(strtoupper((string) $method), ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'], true), function ($q) use ($method) {
                    $q->where('method', strtoupper((string) $method));
                })
                ->when(in_array($statusGroup, ['2xx', '4xx', '5xx'], true), function ($q) use ($statusGroup) {
                    match ($statusGroup) {
                        '2xx' => $q->whereBetween('status_kode', [200, 299]),
                        '4xx' => $q->whereBetween('status_kode', [400, 499]),
                        '5xx' => $q->where('status_kode', '>=', 500),
                    };
                })
                ->latest('id_log')
                ->paginate($perPage);
        } else {
            $logs = LogAktivitas::query()
                ->with(['pengguna'])
                ->when($cari, function ($q) use ($cari) {
                    $pattern = '%'.addcslashes($cari, '%_').'%';
                    $q->where(function ($sub) use ($pattern) {
                        $sub->where('pesan', 'like', $pattern)
                            ->orWhere('entitas', 'like', $pattern)
                            ->orWhere('aksi', 'like', $pattern);
                    });
                })
                ->when(in_array($aksi, LogAktivitas::AKSI, true), function ($q) use ($aksi) {
                    $q->where('aksi', $aksi);
                })
                ->when(in_array($status, ['berhasil', 'gagal'], true), function ($q) use ($status) {
                    $q->where('status', $status);
                })
                ->latest('id_log')
                ->paginate($perPage);
        }

        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        return view('admin.logs.index', compact(
            'logs', 'tab', 'viewMode', 'perPage', 'perPageOptions',
            'aksi', 'status', 'method', 'statusGroup'
        ));
    }
}
