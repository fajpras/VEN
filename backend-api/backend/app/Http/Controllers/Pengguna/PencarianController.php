<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Models\Karya;
use App\Models\Pameran;
use App\Models\Pengguna;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class PencarianController extends Controller
{
    private const PENGGUNA_PUBLIK = 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil';

    public function search(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'q' => ['nullable', 'string', 'max:100'],
            'tipe' => ['nullable', 'string', Rule::in(['semua', 'karya', 'pameran', 'pengguna'])],
            'kategori' => ['nullable', 'array', 'max:20'],
            'kategori.*' => ['string', 'max:100', Rule::exists('kategori', 'kategori')],
            'exclude' => ['nullable', 'string', 'max:4000'],
            'refresh' => ['nullable', 'boolean'],
        ]);

        $keyword = $this->escapeLike(trim($validated['q'] ?? ''));
        $tipe = $validated['tipe'] ?? 'semua';
        $kategori = array_values(array_filter($validated['kategori'] ?? []));
        $excludeIds = array_filter(array_map('trim', explode(',', $validated['exclude'] ?? '')));
        $refresh = (bool) ($validated['refresh'] ?? false);

        if ($tipe === 'karya') {
            $paginator = $this->queryKarya($request, $keyword, $excludeIds, $refresh, $kategori)->paginate(24);
            if ($paginator->isEmpty() && !empty($excludeIds)) {
                $paginator = $this->queryKarya($request, $keyword, [], true, $kategori)->paginate(24);
            }
            return $this->ok($paginator);
        }

        if ($tipe === 'pameran') {
            return $this->ok($this->queryPameran($request, $keyword, $kategori, $refresh)->paginate(24));
        }

        if ($tipe === 'pengguna') {
            return $this->ok($this->queryPengguna($keyword)->paginate(24));
        }

        return $this->ok([
            'karya' => $this->queryKarya($request, $keyword, $excludeIds, $refresh, $kategori)->take(8)->get(),
            'pameran' => $this->queryPameran($request, $keyword, $kategori, $refresh)->take(8)->get(),
            'pengguna' => $this->queryPengguna($keyword)->take(8)->get(),
        ]);
    }

    private function queryKarya(
        Request $request,
        string $keyword,
        array $excludeIds,
        bool $refresh,
        array $kategori,
    ): Builder {
        $query = Karya::query()->with(['kategori', self::PENGGUNA_PUBLIK]);

        if ($keyword !== '') {
            $query->where(function (Builder $sub) use ($keyword) {
                $sub->where('judul', 'like', "%{$keyword}%")
                    ->orWhere('deskripsi', 'like', "%{$keyword}%")
                    ->orWhereHas('kategori', fn (Builder $k) => $k->where('kategori', 'like', "%{$keyword}%"));
            });
        } else {
            $this->terapkanDiscoveryKarya($request, $query, $kategori);
        }

        if (!empty($kategori)) {
            $query->whereHas('kategori', fn (Builder $k) => $k->whereIn('kategori', $kategori));
        }

        return $query
            ->when(!empty($excludeIds), fn (Builder $q) => $q->whereNotIn('karya.id_karya', $excludeIds))
            ->when(
                $refresh,
                fn (Builder $q) => $q->inRandomOrder(),
                fn (Builder $q) => $q->latest('karya.created_at'),
            );
    }

    private function terapkanDiscoveryKarya(Request $request, Builder $query, array $kategori): void
    {
        if (!empty($kategori)) {
            return;
        }

        $user = $request->user();
        $userCategories = $user ? $user->kategori()->get() : collect();
        $hasOthers = $userCategories->contains(fn ($k) => strcasecmp($k->kategori, 'others') === 0);
        $minatIds = $userCategories->pluck('id_kategori')->all();

        if (empty($minatIds) || $hasOthers) {
            return;
        }

        $luarMinat = (clone $query)->whereNotIn('karya.id_kategori', $minatIds);
        if ((clone $luarMinat)->exists()) {
            $query->whereNotIn('karya.id_kategori', $minatIds);
        }
    }

    private function queryPameran(
        Request $request,
        string $keyword,
        array $kategori,
        bool $refresh,
    ): Builder {
        $query = Pameran::query()
            ->with(['model3d', self::PENGGUNA_PUBLIK])
            ->select('pameran.*')
            ->addSelect([
                'kategori' => Karya::query()
                    ->join('kategori', 'kategori.id_kategori', '=', 'karya.id_kategori')
                    ->whereColumn('karya.id_pameran', 'pameran.id_pameran')
                    ->orderBy('karya.created_at')
                    ->select('kategori.kategori')
                    ->limit(1),
            ]);

        if ($keyword !== '') {
            $query->where(function (Builder $sub) use ($keyword) {
                $sub->where('judul', 'like', "%{$keyword}%")
                    ->orWhere('tipe', 'like', "%{$keyword}%");
            });
        } else {
            $this->terapkanDiscoveryPameran($request, $query, $kategori);
        }

        if (!empty($kategori)) {
            $query->whereHas('karya.kategori', fn (Builder $k) => $k->whereIn('kategori', $kategori));
        }

        return $refresh
            ? $query->inRandomOrder()
            : $query->latest('pameran.id_pameran');
    }

    private function terapkanDiscoveryPameran(Request $request, Builder $query, array $kategori): void
    {
        if (!empty($kategori)) {
            return;
        }

        $user = $request->user();
        $userCategories = $user ? $user->kategori()->get() : collect();
        $hasOthers = $userCategories->contains(fn ($k) => strcasecmp($k->kategori, 'others') === 0);
        $minatIds = $userCategories->pluck('id_kategori')->all();

        if (empty($minatIds) || $hasOthers) {
            return;
        }

        $luarMinat = (clone $query)->whereHas(
            'karya',
            fn (Builder $k) => $k->whereNotIn('id_kategori', $minatIds),
        );

        if ((clone $luarMinat)->exists()) {
            $query->whereHas('karya', fn (Builder $k) => $k->whereNotIn('id_kategori', $minatIds));
        }
    }

    private function queryPengguna(string $keyword): Builder
    {
        return Pengguna::query()
            ->where('status', 'aktif')
            ->when($keyword !== '', function (Builder $q) use ($keyword) {
                $q->where(function (Builder $sub) use ($keyword) {
                    $sub->where('nama', 'like', "%{$keyword}%")
                        ->orWhere('nama_panggilan', 'like', "%{$keyword}%");
                });
            })
            ->select(['id_pengguna', 'nama', 'nama_panggilan', 'foto_profil', 'deskripsi'])
            ->latest('created_at');
    }
}
