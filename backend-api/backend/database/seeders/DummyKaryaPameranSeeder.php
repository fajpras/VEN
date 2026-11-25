<?php

namespace Database\Seeders;

use App\Models\Karya;
use App\Models\Kategori;
use App\Models\Model3D;
use App\Models\Pameran;
use App\Models\Pengguna;
use App\Models\Ruangan;
use Carbon\Carbon;
use Database\Seeders\Concerns\GeneratesSeedAssets;
use Illuminate\Database\Seeder;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use RuntimeException;

class DummyKaryaPameranSeeder extends Seeder
{
    use GeneratesSeedAssets;

    public function run(): void
    {
        $original = DB::getDefaultConnection();
        if (config()->has('database.connections.mysql_dba')) {
            DB::setDefaultConnection('mysql_dba');
        }

        try {
            $this->seedAll();
        } finally {
            DB::setDefaultConnection($original);
        }
    }

    private function seedAll(): void
    {
        $this->resetAssetCounters();

        // 1. Pastikan kategori resmi (14 kategori) & model hall 3D tersedia
        $this->call([
            KategoriSeeder::class,
            ModelSeeder::class,
        ]);

        $dataset = require database_path('seeders/data/dummy-karya-pameran.php');

        /** @var Collection<string, int> $kategoriMap */
        $kategoriMap = Kategori::query()->pluck('id_kategori', 'kategori');

        /** @var Collection<string, Model3D> $hallMap */
        $hallMap = Model3D::query()->where('jenis', 'hall')->get()->keyBy('nama');
        $fallbackHall = $hallMap->first() ?? Model3D::firstOrCreate(
            ['nama' => 'Hall Pavilion 24'],
            [
                'jenis' => 'hall',
                'file' => 'models/halls/hall_24.glb',
                'kapasitas' => '24',
                'gambar' => 'models/halls/preview_hall_24.png',
            ]
        );

        // 2. Seed kreator dummy + bobot minat kategori
        $kreatorMap = $this->seedKreator($dataset['kreator'], $kategoriMap);

        // 3. Seed pameran tematik + banner PNG + Ruangan umum
        $pameranMap = $this->seedPameran($dataset['pameran'], $kreatorMap, $hallMap, $fallbackHall);

        // 4. Seed karya lintas 14 kategori (poster PNG + video MP4 valid)
        $stats = $this->seedKarya($dataset['karya'], $kategoriMap, $kreatorMap, $pameranMap);

        $assetCounters = $this->getAssetCounters();
        $pameranAktif = collect($pameranMap)->filter(fn (Pameran $p) => $p->deleted_at === null)->count();
        $pameranArsip = count($pameranMap) - $pameranAktif;

        $this->command?->info(sprintf(
            'DummyKaryaPameranSeeder selesai: %d kreator, %d pameran (%d aktif, %d arsip), %d karya (%d poster, %d video, %d arsip), aset (%d dibuat, %d dipakai ulang).',
            count($kreatorMap),
            count($pameranMap),
            $pameranAktif,
            $pameranArsip,
            $stats['total'],
            $stats['poster'],
            $stats['video'],
            $stats['arsip'],
            $assetCounters['generated'],
            $assetCounters['reused'],
        ));
    }

    /**
     * @param  list<array<string, mixed>>  $kreatorList
     * @param  Collection<string, int>  $kategoriMap
     * @return array<string, Pengguna>
     */
    private function seedKreator(array $kreatorList, Collection $kategoriMap): array
    {
        $result = [];

        foreach ($kreatorList as $item) {
            $pengguna = Pengguna::withTrashed()->firstWhere('email', $item['email']);

            if ($pengguna) {
                $pengguna->fill([
                    'nama' => $item['nama'],
                    'nama_panggilan' => $item['nama_panggilan'],
                    'deskripsi' => $item['deskripsi'],
                    'role' => 'pengguna',
                    'status' => 'aktif',
                    'onboarding_selesai' => true,
                ]);
                if ($pengguna->trashed()) {
                    $pengguna->deleted_at = null;
                }
                $pengguna->save();
            } else {
                $pengguna = Pengguna::create([
                    'email' => $item['email'],
                    'nama' => $item['nama'],
                    'nama_panggilan' => $item['nama_panggilan'],
                    'kata_sandi' => Hash::make('password123'),
                    'deskripsi' => $item['deskripsi'],
                    'role' => 'pengguna',
                    'status' => 'aktif',
                    'onboarding_selesai' => true,
                ]);
            }

            $pivotData = [];
            foreach ($item['minat'] ?? [] as $namaKategori => $bobot) {
                if (isset($kategoriMap[$namaKategori])) {
                    $pivotData[$kategoriMap[$namaKategori]] = ['bobot' => $bobot];
                }
            }
            if (! empty($pivotData)) {
                $pengguna->kategori()->syncWithoutDetaching($pivotData);
            }

            $result[$item['email']] = $pengguna;
        }

        return $result;
    }

    /**
     * @param  list<array<string, mixed>>  $pameranList
     * @param  array<string, Pengguna>  $kreatorMap
     * @param  Collection<string, Model3D>  $hallMap
     * @return array<string, Pameran>
     */
    private function seedPameran(
        array $pameranList,
        array $kreatorMap,
        Collection $hallMap,
        Model3D $fallbackHall,
    ): array {
        $result = [];

        foreach ($pameranList as $item) {
            $kreator = $kreatorMap[$item['kreator']]
                ?? throw new RuntimeException("Kreator pameran tidak ditemukan: {$item['kreator']}");

            $hall = $hallMap->get($item['hall'] ?? '') ?? $fallbackHall;
            $bannerPath = $this->ensureBannerPng($item['judul'], $item['warna'], $item['tipe']);

            $createdAt = Carbon::now()->subDays((int) ($item['days_ago'] ?? 1));
            $deletedAt = isset($item['deleted_days_ago'])
                ? Carbon::now()->subDays((int) $item['deleted_days_ago'])
                : null;

            $pameran = Pameran::withTrashed()->firstWhere('judul', $item['judul']) ?? new Pameran();
            $pameran->forceFill([
                'judul' => $item['judul'],
                'id_pengguna' => $kreator->id_pengguna,
                'id_model' => $hall->id_model,
                'warna' => $item['warna'],
                'banner' => $bannerPath,
                'tipe' => $item['tipe'],
                'created_at' => $createdAt,
                'updated_at' => $createdAt,
                'deleted_at' => $deletedAt,
            ])->save();

            Ruangan::firstOrCreate([
                'id_pameran' => $pameran->id_pameran,
                'jenis' => 'umum',
            ]);

            $result[$item['judul']] = $pameran;
        }

        return $result;
    }

    /**
     * @param  list<array<string, mixed>>  $karyaList
     * @param  Collection<string, int>  $kategoriMap
     * @param  array<string, Pengguna>  $kreatorMap
     * @param  array<string, Pameran>  $pameranMap
     * @return array{total: int, poster: int, video: int, arsip: int}
     */
    private function seedKarya(
        array $karyaList,
        Collection $kategoriMap,
        array $kreatorMap,
        array $pameranMap,
    ): array {
        $stats = ['total' => 0, 'poster' => 0, 'video' => 0, 'arsip' => 0];
        $videoIndex = 0;

        foreach ($karyaList as $item) {
            $kategoriId = $kategoriMap[$item['kategori']]
                ?? throw new RuntimeException("Kategori tidak ditemukan: {$item['kategori']}");
            $kreator = $kreatorMap[$item['kreator']]
                ?? throw new RuntimeException("Kreator karya tidak ditemukan: {$item['kreator']}");

            $pameranJudul = $item['pameran'] ?? null;
            $pameranId = $pameranJudul !== null && isset($pameranMap[$pameranJudul])
                ? $pameranMap[$pameranJudul]->id_pameran
                : null;

            $jenis = $item['jenis'];
            if ($jenis === 'video') {
                $filePath = $this->ensureVideoMp4($item['judul'], $videoIndex);
                $videoIndex++;
                $stats['video']++;
            } else {
                $filePath = $this->ensurePosterPng($item['judul'], $item['kategori']);
                $stats['poster']++;
            }

            $deskripsi = trim($item['subtitle'])."\n\n".trim($item['narasi']);
            $createdAt = Carbon::now()->subDays((int) ($item['days_ago'] ?? 0));
            $deletedAt = isset($item['deleted_days_ago'])
                ? Carbon::now()->subDays((int) $item['deleted_days_ago'])
                : null;

            if ($deletedAt !== null) {
                $stats['arsip']++;
            }

            $karya = Karya::withTrashed()->firstWhere('judul', $item['judul']) ?? new Karya();
            $karya->forceFill([
                'judul' => $item['judul'],
                'id_kategori' => $kategoriId,
                'id_pengguna' => $kreator->id_pengguna,
                'id_pameran' => $pameranId,
                'jenis' => $jenis,
                'deskripsi' => $deskripsi,
                'file' => $filePath,
                'created_at' => $createdAt,
                'updated_at' => $createdAt,
                'deleted_at' => $deletedAt,
            ])->save();

            $stats['total']++;
        }

        return $stats;
    }
}
