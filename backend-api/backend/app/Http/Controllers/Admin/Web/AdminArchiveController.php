<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\Karya;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Services\LogSistem;
use App\Support\AdminListOptions;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AdminArchiveController extends Controller
{
    public function index(Request $request): View
    {
        $tab = (string) $request->query('tab', 'exhibitions');
        if (! in_array($tab, ['exhibitions', 'posts', 'accounts'], true)) {
            $tab = 'exhibitions';
        }

        $cari = $request->query('cari');
        $perPage = AdminListOptions::perPage($request);
        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        $counts = [
            'exhibitions' => Pameran::onlyTrashed()->count(),
            'posts' => Karya::onlyTrashed()->count(),
            'accounts' => Pengguna::onlyTrashed()->count(),
        ];

        $items = match ($tab) {
            'posts' => Karya::onlyTrashed()
                ->with(['pengguna', 'kategori'])
                ->when($cari, function ($q) use ($cari) {
                    $pattern = '%'.addcslashes($cari, '%_').'%';
                    $q->where('judul', 'like', $pattern);
                })
                ->latest('deleted_at')
                ->paginate($perPage),
            'accounts' => Pengguna::onlyTrashed()
                ->when($cari, function ($q) use ($cari) {
                    $pattern = '%'.addcslashes($cari, '%_').'%';
                    $q->where(function ($sub) use ($pattern) {
                        $sub->where('nama', 'like', $pattern)
                            ->orWhere('nama_panggilan', 'like', $pattern)
                            ->orWhere('email', 'like', $pattern);
                    });
                })
                ->latest('deleted_at')
                ->paginate($perPage),
            default => Pameran::onlyTrashed()
                ->with(['pengguna'])
                ->when($cari, function ($q) use ($cari) {
                    $pattern = '%'.addcslashes($cari, '%_').'%';
                    $q->where('judul', 'like', $pattern);
                })
                ->latest('deleted_at')
                ->paginate($perPage),
        };

        return view('admin.archive.index', compact('items', 'tab', 'counts', 'perPage', 'perPageOptions'));
    }

    public function restore(Request $request, string $type, string $id): RedirectResponse
    {
        if (! in_array($type, ['exhibitions', 'posts', 'accounts'], true)) {
            return redirect()->route('admin.archive')->with('error', __('admin.archive.not_found'));
        }

        $model = match ($type) {
            'exhibitions' => Pameran::onlyTrashed()->find($id),
            'posts' => Karya::onlyTrashed()->find($id),
            'accounts' => Pengguna::onlyTrashed()->find($id),
        };

        if (! $model) {
            return redirect()
                ->route('admin.archive', ['tab' => $type])
                ->with('error', __('admin.archive.not_found'));
        }

        $model->restore();

        [$jenis, $label, $successKey] = match ($type) {
            'exhibitions' => ['pameran', "pameran #{$model->id_pameran} \"{$model->judul}\"", 'admin.archive.restored_exhibition'],
            'posts' => ['karya', "karya #{$model->id_karya} \"{$model->judul}\"", 'admin.archive.restored_post'],
            'accounts' => ['pengguna', "pengguna \"{$model->nama_panggilan}\"", 'admin.archive.restored_account'],
        };

        LogSistem::aktivitas(
            entitas: $request->user('web')?->nama,
            jenis: $jenis,
            pesan: "Memulihkan {$label} dari arsip",
            status: 'berhasil',
            idPengguna: $request->user('web')?->id_pengguna,
            aksi: 'pulih',
        );

        return redirect()
            ->route('admin.archive', ['tab' => $type])
            ->with('success', __($successKey));
    }
}
