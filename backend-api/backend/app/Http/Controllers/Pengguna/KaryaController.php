<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Models\Karya;
use App\Models\Objek;
use App\Models\PosisiObjek;
use Illuminate\Http\Request;
use Illuminate\Support\Arr;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;
use Throwable;

class KaryaController extends Controller
{
    private const PENGGUNA_PUBLIK = 'pengguna:id_pengguna,nama,nama_panggilan,foto_profil';

    public function index(Request $request)
    {
        $request->validate([
            'id_kategori' => ['nullable', 'integer'],
            'id_pameran' => ['nullable', 'string'],
            'jenis' => ['nullable', Rule::in(['poster', 'video'])],
            'judul' => ['nullable', 'string', 'max:100'],
        ]);

        $karya = Karya::with(['kategori', self::PENGGUNA_PUBLIK, 'pameran'])
            ->when($request->filled('id_kategori'), fn ($q) => $q->where('id_kategori', $request->integer('id_kategori')))
            ->when($request->filled('id_pameran'), fn ($q) => $q->where('id_pameran', $request->string('id_pameran')->toString()))
            ->when($request->filled('jenis'), fn ($q) => $q->where('jenis', $request->query('jenis')))
            ->when($request->filled('judul'), fn ($q) => $q->where('judul', 'like', '%'.$this->escapeLike($request->query('judul')).'%'))
            ->when($request->boolean('milikku'), fn ($q) => $q->where('id_pengguna', $request->user()->id_pengguna))
            ->latest('created_at')
            ->paginate(24);

        return $this->ok($karya);
    }

    public function rekomendasi(Request $request)
    {
        $user = $request->user();
        $perPage = min($request->integer('per_page', 12), 24);
        $excludeIds = array_filter(array_map('trim', explode(',', (string) $request->query('exclude', ''))));
        $refresh = $request->boolean('refresh', false);

        // Ambil kategori minat pengguna
        $userCategories = $user ? $user->kategori()->get() : collect();
        $hasOthers = $userCategories->contains(fn ($k) => strcasecmp($k->kategori, 'others') === 0);
        $minatIds = $userCategories->pluck('id_kategori')->toArray();

        // KASUS 1: Pengguna memilih "Others" ATAU belum memiliki minat (guest/skip)
        // -> Tampilkan karya dari SELURUH kategori yang ada, prioritaskan karya kreator lain
        if ($hasOthers || empty($minatIds)) {
            $query = Karya::query()->select('karya.*')->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran']);

            if (!empty($excludeIds)) {
                $query->whereNotIn('karya.id_karya', $excludeIds);
            }

            if ($user) {
                $query->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
            }

            if ($refresh) {
                $query->inRandomOrder();
            } else {
                $query->latest('karya.created_at');
            }

            $karya = $query->paginate($perPage);

            // Fallback: Jika seluruh karya sudah pernah dilihat, acak ulang seluruh karya (tetap prioritaskan kreator lain)
            if ($karya->isEmpty() && !empty($excludeIds)) {
                $fallback = Karya::query()->select('karya.*')->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran']);
                if ($user) {
                    $fallback->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
                }
                $karya = $fallback->inRandomOrder()->paginate($perPage);
            }

            return $this->ok($karya);
        }

        // KASUS 2: Pengguna memilih kategori spesifik (misal: 3D, UI/UX) TANPA "Others"
        // Hitung apakah masih ada karya dari kategori yang dipilih yang belum pernah dilihat
        $totalMinatTersedia = Karya::whereIn('id_kategori', $minatIds)
            ->when(!empty($excludeIds), fn ($q) => $q->whereNotIn('id_karya', $excludeIds))
            ->count();

        if ($totalMinatTersedia > 0) {
            // Fase A: Tampilkan karya yang sesuai kategori pilihan, prioritaskan karya kreator lain
            $minatQuery = Karya::query()->select('karya.*')
                ->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran'])
                ->whereIn('karya.id_kategori', $minatIds);

            if (!empty($excludeIds)) {
                $minatQuery->whereNotIn('karya.id_karya', $excludeIds);
            }

            if ($user) {
                $minatQuery->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
            }

            if ($refresh) {
                $minatQuery->inRandomOrder();
            } else {
                $minatQuery->latest('karya.created_at');
            }

            $karya = $minatQuery->paginate($perPage);
        } else {
            // Fase B: Karya dari kategori pilihan sudah habis -> Tampilkan karya dari kategori lain
            $lainQuery = Karya::query()->select('karya.*')
                ->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran'])
                ->whereNotIn('karya.id_kategori', $minatIds);

            if (!empty($excludeIds)) {
                $lainQuery->whereNotIn('karya.id_karya', $excludeIds);
            }

            if ($user) {
                $lainQuery->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
            }

            if ($refresh) {
                $lainQuery->inRandomOrder();
            } else {
                $lainQuery->latest('karya.created_at');
            }

            $karya = $lainQuery->paginate($perPage);

            // Fase C (Fallback): Jika seluruh karya kategori lain sudah pernah dilihat, acak ulang
            if ($karya->isEmpty() && !empty($excludeIds)) {
                $fallbackLain = Karya::query()->select('karya.*')->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran'])
                    ->whereNotIn('karya.id_kategori', $minatIds);
                if ($user) {
                    $fallbackLain->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
                }
                $karya = $fallbackLain->inRandomOrder()->paginate($perPage);

                // Jika di database tidak ada karya kategori lain, fallback acak seluruh karya
                if ($karya->isEmpty()) {
                    $fallbackSemua = Karya::query()->select('karya.*')->with(['kategori', self::PENGGUNA_PUBLIK, 'pameran']);
                    if ($user) {
                        $fallbackSemua->orderByRaw('CASE WHEN karya.id_pengguna = ? THEN 1 ELSE 0 END ASC', [$user->id_pengguna]);
                    }
                    $karya = $fallbackSemua->inRandomOrder()->paginate($perPage);
                }
            }
        }

        return $this->ok($karya);
    }

    public function show(Karya $karya)
    {
        return $this->ok($karya->load(['kategori', self::PENGGUNA_PUBLIK, 'pameran', 'objek.posisi', 'objek.model3d']));
    }

    public function store(Request $request)
    {
        $user = $request->user();
        if ($request->has('id_pameran') && $request->input('id_pameran') === '') {
            $request->merge(['id_pameran' => null]);
        }
        $validated = $request->validate($this->rules($request, true));

        $path = $request->file('file')->store('karya', 'local');

        try {
            $karya = DB::transaction(function () use ($validated, $path, $user) {
                $data = Arr::except($validated, ['file', 'objek']) + ['file' => $path];

                if (! empty($validated['objek'])) {
                    $data['id_objek'] = Objek::forceCreate($validated['objek'])->id_objek;
                }

                return $user->karya()->create($data);
            });
        } catch (Throwable $e) {
            Storage::disk('local')->delete($path);
            throw $e;
        }

        return $this->created($karya->load(['kategori', 'objek']), 'Karya dibuat.');
    }

    public function update(Request $request, Karya $karya)
    {
        $this->pastikanPemilik($request, $karya->id_pengguna);

        if ($request->has('id_pameran') && $request->input('id_pameran') === '') {
            $request->merge(['id_pameran' => null]);
        }

        $validated = $request->validate($this->rules($request, false));
        $pathLama = null;
        $pathBaru = null;

        if ($request->hasFile('file')) {
            $pathLama = $karya->file;
            $pathBaru = $request->file('file')->store('karya', 'local');
        }

        try {
            DB::transaction(function () use ($validated, $karya, $pathBaru) {
                $data = Arr::except($validated, ['file', 'objek']);

                if ($pathBaru) {
                    $data['file'] = $pathBaru;
                }

                // Jika pameran dilepas atau dipindah ke pameran lain, bersihkan tautan objek 3D lama
                if (array_key_exists('id_pameran', $data) && $data['id_pameran'] != $karya->id_pameran) {
                    $data['id_objek'] = null;
                }

                if (! empty($validated['objek'])) {
                    if ($karya->objek) {
                        $karya->objek->forceFill($validated['objek'])->save();
                    } else {
                        $data['id_objek'] = Objek::forceCreate($validated['objek'])->id_objek;
                    }
                }

                $karya->fill($data)->save();
            });
        } catch (Throwable $e) {
            if ($pathBaru) {
                Storage::disk('local')->delete($pathBaru);
            }
            throw $e;
        }

        if ($pathLama) {
            Storage::disk('local')->delete($pathLama);
        }

        return $this->ok($karya->fresh(['kategori', 'objek.posisi']), 'Karya diperbarui.');
    }

    public function destroy(Request $request, Karya $karya)
    {
        $this->pastikanPemilik($request, $karya->id_pengguna);

        $filePath = $karya->file;
        $karya->forceDelete();

        if ($filePath && ! str_contains($filePath, 'default_artwork')) {
            $cleanPath = ltrim(preg_replace('#^/?storage/#', '', $filePath), '/');
            Storage::disk('public')->delete($cleanPath);
            Storage::disk('local')->delete($filePath);
        }

        return $this->ok(null, 'Karya dihapus.');
    }

    public function updatePosisiObjek(Request $request, Karya $karya)
    {
        $pemilikPameran = $karya->pameran?->id_pengguna;
        $user = $request->user();

        if (! hash_equals((string) $karya->id_pengguna, $user->id_pengguna)
            && ! ($pemilikPameran && hash_equals($pemilikPameran, $user->id_pengguna))) {
            abort(403, 'Anda tidak berhak mengatur posisi objek ini.');
        }

        $objek = $karya->objek;

        if (! $objek) {
            return $this->error('Karya ini belum memiliki objek 3D.', 422);
        }

        $angka = ['required', 'numeric', 'between:-100000,100000'];
        $validated = $request->validate([
            'posisi_x' => $angka, 'posisi_y' => $angka, 'posisi_z' => $angka,
            'rotasi_x' => $angka, 'rotasi_y' => $angka, 'rotasi_z' => $angka,
        ]);

        DB::transaction(function () use ($objek, $validated) {
            $lama = $objek->posisi;
            $baru = PosisiObjek::forceCreate($validated);

            $objek->forceFill(['id_posisi' => $baru->id_posisi])->save();
            $lama?->delete();
        });

        return $this->ok($objek->fresh()->load('posisi'), 'Posisi objek diperbarui.');
    }

    public function uploadMedia(Request $request)
    {
        $request->validate([
            'file' => ['required', 'file', 'max:51200', 'mimes:jpeg,jpg,png,webp,mp4,webm,mov,mp3,wav,ogg'],
        ]);

        $path = $request->file('file')->store('karya', 'public');

        return $this->ok([
            'url' => Storage::url($path),
            'path' => $path,
        ], 'Berkas karya berhasil diunggah.');
    }

    public function berkas(Karya $karya)
    {
        if (! $karya->file) {
            abort(404);
        }

        $cleanPath = ltrim(preg_replace('#^/?storage/#', '', $karya->file), '/');

        if (Storage::disk('public')->exists($cleanPath)) {
            return Storage::disk('public')->response($cleanPath, null, [
                'X-Content-Type-Options' => 'nosniff',
                'Cache-Control' => 'public, max-age=86400',
            ]);
        }

        if (Storage::disk('local')->exists($karya->file)) {
            return Storage::disk('local')->response($karya->file, null, [
                'X-Content-Type-Options' => 'nosniff',
                'Cache-Control' => 'private, max-age=1800',
            ]);
        }

        if (Storage::disk('local')->exists($cleanPath)) {
            return Storage::disk('local')->response($cleanPath, null, [
                'X-Content-Type-Options' => 'nosniff',
                'Cache-Control' => 'private, max-age=1800',
            ]);
        }

        abort(404);
    }

    private function rules(Request $request, bool $wajib): array
    {
        $r = $wajib ? 'required' : 'sometimes';
        $jenis = $request->input('jenis', $request->route('karya')?->jenis);

        return [
            'id_kategori' => [$r, 'integer', 'exists:kategori,id_kategori'],

            'id_pameran' => ['nullable', 'string', Rule::exists('pameran', 'id_pameran')
                ->where('id_pengguna', $request->user()->id_pengguna)
                ->whereNull('deleted_at')],
            'judul' => [$r, 'string', 'max:255'],
            'jenis' => [$r, Rule::in(['poster', 'video'])],
            'deskripsi' => [$r, 'string', 'max:5000'],
            'file' => [$r, 'file', 'max:51200', $jenis === 'video'
                ? 'mimetypes:video/mp4,video/webm,video/quicktime'
                : 'mimes:jpg,jpeg,png,webp'],
            'objek' => ['sometimes', 'array'],
            'objek.id_model' => ['required_with:objek', 'integer', Rule::exists('model', 'id_model')->where('jenis', 'objek')],
            'objek.jenis' => ['required_with:objek', Rule::in(['panel', 'aset'])],
        ];
    }
}
