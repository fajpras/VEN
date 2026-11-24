<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\Karya;
use App\Models\Kategori;
use App\Support\AdminListOptions;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AdminPostController extends Controller
{
    public function index(Request $request): View
    {
        $cari = $request->query('cari');
        $kategori = $request->query('kategori');
        $perPage = AdminListOptions::perPage($request);
        $viewMode = AdminListOptions::viewMode($request);

        $karya = Karya::query()
            ->with(['pengguna', 'kategori'])
            ->when($cari, function ($q) use ($cari) {
                $pattern = '%'.addcslashes($cari, '%_').'%';
                $q->where(function ($sub) use ($pattern) {
                    $sub->where('judul', 'like', $pattern)
                        ->orWhere('deskripsi', 'like', $pattern);
                });
            })
            ->when($kategori, function ($q) use ($kategori) {
                $q->whereHas('kategori', function ($sub) use ($kategori) {
                    $sub->where('kategori', $kategori);
                });
            })
            ->latest('created_at')
            ->paginate($perPage);

        $kategoriList = Kategori::pluck('kategori')->filter()->values()->toArray();
        if (empty($kategoriList)) {
            $kategoriList = ['Internet of Things', 'Virtual Reality', 'Artificial Intelligence', 'Game Development', 'Graphic Design'];
        }

        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        return view('admin.posts.index', compact('karya', 'kategoriList', 'viewMode', 'perPage', 'perPageOptions'));
    }

    public function destroy(string $id): RedirectResponse
    {
        try {
            $post = Karya::find($id);

            if (! $post) {
                return redirect()
                    ->route('admin.posts')
                    ->with('error', 'Artwork post could not be found. Nothing was removed.');
            }

            $post->delete();
        } catch (\Throwable $e) {
            report($e);

            return redirect()
                ->route('admin.posts')
                ->with('error', 'Failed to remove the artwork post. Please check the application logs.');
        }

        return redirect()->route('admin.posts')->with('success', 'Artwork post was successfully removed from public feeds.');
    }
}
