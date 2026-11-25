<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\Pameran;
use App\Support\AdminListOptions;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AdminExhibitionController extends Controller
{
    public function index(Request $request): View
    {
        $cari = $request->query('cari');
        $tipe = $request->query('tipe');
        $sort = $request->query('sort', 'newest');
        $perPage = AdminListOptions::perPage($request);
        $viewMode = AdminListOptions::viewMode($request);

        $query = Pameran::query()
            ->with(['pengguna'])
            ->withCount(['ruangan', 'karya'])
            ->when($cari, function ($q) use ($cari) {
                $pattern = '%'.addcslashes($cari, '%_').'%';
                $q->where(function ($sub) use ($pattern) {
                    $sub->where('judul', 'like', $pattern);
                });
            })
            ->when(in_array($tipe, ['kecil', 'sedang', 'besar'], true), function ($q) use ($tipe) {
                $q->where('tipe', $tipe);
            });

        match ($sort) {
            'oldest' => $query->oldest('id_pameran'),
            default => $query->latest('id_pameran'),
        };

        $pameran = $query->paginate($perPage);
        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        return view('admin.exhibitions.index', compact('pameran', 'viewMode', 'perPage', 'perPageOptions', 'tipe', 'sort'));
    }

    public function destroy(string $id): RedirectResponse
    {
        try {
            $exhibit = Pameran::find($id);

            if (! $exhibit) {
                return redirect()
                    ->route('admin.exhibitions')
                    ->with('error', 'Virtual exhibition could not be found. Nothing was removed.');
            }

            $exhibit->delete();
        } catch (\Throwable $e) {
            report($e);

            return redirect()
                ->route('admin.exhibitions')
                ->with('error', 'Failed to take down the virtual exhibition. Please check the application logs.');
        }

        return redirect()->route('admin.exhibitions')->with('success', 'Virtual exhibition was successfully taken down.');
    }
}
