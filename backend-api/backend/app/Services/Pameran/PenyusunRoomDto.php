<?php

namespace App\Services\Pameran;

use App\Models\Pameran;
use App\Models\Pengguna;

/**
 * Menyusun payload room 3D (RoomDto) yang dikonsumsi Unity untuk sebuah pameran.
 *
 * Dipakai bersama oleh endpoint pameran (ruang umum) dan ruangan privat,
 * sehingga struktur JSON untuk keduanya selalu identik.
 */
final class PenyusunRoomDto
{
    private const TEMPLATE_VALID = ['template01', 'template02', 'template03', 'template04'];

    /**
     * @return array<string, mixed> struktur JSON RoomDto untuk Unity
     */
    public function susun(Pameran $pameran, ?Pengguna $pengunjung = null, bool $isPublic = true, ?string $roomId = null): array
    {
        $pameran->load([
            'model3d', 'ruangan',
            'objek.posisi', 'objek.model3d', 'objek.karya.kategori',
        ]);

        $items = [];
        foreach ($pameran->objek as $obj) {
            $posisi = $obj->posisi;
            $karya = $obj->karya->first();
            $items[] = [
                'prefabId' => $obj->model3d?->nama ?? 'panel',
                'position' => [
                    'x' => (float) ($posisi?->posisi_x ?? 0.0),
                    'y' => (float) ($posisi?->posisi_y ?? 0.0),
                    'z' => (float) ($posisi?->posisi_z ?? 0.0),
                ],
                'rotation' => [
                    'x' => 0.0,
                    'y' => (float) ($posisi?->rotasi_z ?? 0.0),
                    'z' => 0.0,
                ],
                'contentType' => $karya?->jenis ?? '',
                'contentUrl' => $karya?->file_url ?? '',
                'title' => $karya?->judul ?? '',
                'description' => $karya?->deskripsi ?? '',
                'karyaId' => (string) ($karya?->id_karya ?? ''),
                'categoryId' => (int) ($karya?->id_kategori ?? 1),
                'isVideo' => $karya?->jenis === 'video',
            ];
        }

        if (empty($items)) {
            $items = $this->itemsContoh($pameran->judul);
        }

        return [
            'roomId' => $roomId ?? (string) $pameran->id_pameran,
            'templateId' => $this->resolusiTemplate($pameran),
            'title' => $pameran->judul,
            'description' => 'Pameran Virtual 3D',
            'wallColor' => $pameran->warna ?? '#FFFFFF',
            'isPublic' => $isPublic,
            'ownerId' => (string) $pameran->id_pengguna,
            'viewerId' => (string) ($pengunjung?->id_pengguna ?? ''),
            'viewerName' => (string) ($pengunjung?->nama_panggilan ?: $pengunjung?->nama ?: ''),
            'items' => $items,
        ];
    }

    /** Tata letak contoh agar Unity tidak menampilkan ruangan kosong. */
    private function itemsContoh(string $judulPameran): array
    {
        return [
            ['prefabId' => 'hpanel_01', 'position' => ['x' => 0.0, 'y' => 1.8, 'z' => 5.2], 'rotation' => ['x' => 0.0, 'y' => 180.0, 'z' => 0.0], 'contentType' => 'poster', 'contentUrl' => '', 'title' => $judulPameran, 'description' => 'Karya Utama'],
            ['prefabId' => 'vpanel_01', 'position' => ['x' => -4.8, 'y' => 1.8, 'z' => 1.5], 'rotation' => ['x' => 0.0, 'y' => 90.0, 'z' => 0.0], 'contentType' => 'poster', 'contentUrl' => '', 'title' => 'Karya Visual 1', 'description' => ''],
            ['prefabId' => 'vpanel_02', 'position' => ['x' => 4.8, 'y' => 1.8, 'z' => 1.5], 'rotation' => ['x' => 0.0, 'y' => -90.0, 'z' => 0.0], 'contentType' => 'poster', 'contentUrl' => '', 'title' => 'Karya Visual 2', 'description' => ''],
            ['prefabId' => 'sculpture_01', 'position' => ['x' => 2.0, 'y' => 0.0, 'z' => 0.0], 'rotation' => ['x' => 0.0, 'y' => -45.0, 'z' => 0.0], 'contentType' => '3d', 'contentUrl' => '', 'title' => 'Instalasi 3D', 'description' => ''],
            ['prefabId' => 'bench_01', 'position' => ['x' => -2.0, 'y' => 0.0, 'z' => 0.0], 'rotation' => ['x' => 0.0, 'y' => 90.0, 'z' => 0.0], 'contentType' => 'aset', 'contentUrl' => '', 'title' => '', 'description' => ''],
        ];
    }

    private function resolusiTemplate(Pameran $pameran): string
    {
        $rawTpl = $pameran->model3d?->file ? strtolower(basename($pameran->model3d->file, '.glb')) : '';

        if (in_array($rawTpl, self::TEMPLATE_VALID, true)) {
            return $rawTpl;
        }

        return match (strtolower((string) $pameran->tipe)) {
            'sedang' => 'template02',
            'besar' => 'template03',
            'acara' => 'template04',
            default => 'template01',
        };
    }
}
