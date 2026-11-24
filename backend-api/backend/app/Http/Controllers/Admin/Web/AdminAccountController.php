<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\Pengguna;
use App\Models\SanksiAkun;
use App\Support\AdminListOptions;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AdminAccountController extends Controller
{
    public function index(Request $request): View
    {
        $cari = $request->query('cari');
        $perPage = AdminListOptions::perPage($request);
        $viewMode = AdminListOptions::viewMode($request);

        $pengguna = Pengguna::query()
            ->withCount(['karya', 'pameran'])
            ->when($cari, function ($q) use ($cari) {
                $pattern = '%'.addcslashes($cari, '%_').'%';
                $q->where(function ($sub) use ($pattern) {
                    $sub->where('nama', 'like', $pattern)
                        ->orWhere('nama_panggilan', 'like', $pattern)
                        ->orWhere('email', 'like', $pattern);
                });
            })
            ->latest('created_at')
            ->paginate($perPage);

        $perPageOptions = AdminListOptions::PER_PAGE_OPTIONS;

        return view('admin.accounts.index', compact('pengguna', 'viewMode', 'perPage', 'perPageOptions'));
    }

    public function suspend(Request $request, string $id): RedirectResponse
    {
        $validated = $request->validate([
            'jenis_sanksi' => ['required', 'in:ditangguhkan,diblokir'],
            'alasan' => ['required', 'string', 'max:255'],
            'duration' => ['required', 'string'],
        ]);

        try {
            $user = Pengguna::find($id);

            if (! $user) {
                return redirect()
                    ->route('admin.accounts')
                    ->with('error', 'User account could not be found. No sanction was applied.');
            }

            $days = match ($validated['duration']) {
                '24h' => 1,
                '7d' => 7,
                '30d' => 30,
                default => null,
            };

            SanksiAkun::create([
                'id_pengguna' => $user->id_pengguna,
                'jenis_sanksi' => $validated['jenis_sanksi'],
                'alasan' => $validated['alasan'],
                'mulai' => now(),
                'berakhir' => $days ? now()->addDays($days) : null,
            ]);

            $user->sinkronStatusSanksi();
        } catch (\Throwable $e) {
            report($e);

            return redirect()
                ->route('admin.accounts')
                ->with('error', 'Failed to apply the suspension. Please check the application logs.');
        }

        return redirect()->route('admin.accounts')->with('success', 'User account suspension policy has been successfully applied.');
    }
}
