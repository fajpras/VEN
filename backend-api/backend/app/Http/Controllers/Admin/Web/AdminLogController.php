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
                            ->orWhere('pesan', 'like', $pattern);
                    });
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
                ->latest('id_log')
                ->paginate($perPage);
        }

        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        return view('admin.logs.index', compact('logs', 'tab', 'viewMode', 'perPage', 'perPageOptions'));
    }
}
