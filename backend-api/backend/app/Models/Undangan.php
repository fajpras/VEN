<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Carbon;

class Undangan extends Model
{
    public $timestamps = false;

    protected $table = 'undangan';
    protected $primaryKey = 'id_undangan';

    protected $fillable = [
        'id_pengguna',
        'id_ruangan',
        'status',
        'waktu',
        'durasi_menit',
        'catatan',
        'alasan',
        'tautan',
        'kadaluarsa_pada',
        'pengingat_dikirim_pada',
    ];

    protected function casts(): array
    {
        return [
            'waktu'                  => 'datetime',
            'durasi_menit'           => 'integer',
            'kadaluarsa_pada'        => 'datetime',
            'pengingat_dikirim_pada' => 'datetime',
            'timestamp'              => 'datetime',
        ];
    }

    /**
     * Batas berlaku tautan & sesi ruang privat: otomatis mati pukul 00:00 di akhir hari jadwal.
     */
    public function batasKadaluarsa(): ?Carbon
    {
        if ($this->kadaluarsa_pada) {
            return $this->kadaluarsa_pada;
        }

        return $this->waktu?->copy()->endOfDay();
    }

    public function sudahKadaluarsa(): bool
    {
        $batas = $this->batasKadaluarsa();

        return $batas !== null && now()->greaterThan($batas);
    }

    public function pengguna(): BelongsTo
    {
        return $this->belongsTo(Pengguna::class, 'id_pengguna', 'id_pengguna');
    }

    public function ruangan(): BelongsTo
    {
        return $this->belongsTo(Ruangan::class, 'id_ruangan', 'id_ruangan');
    }

    public function permintaanGabung(): HasMany
    {
        return $this->hasMany(PermintaanGabungRuangan::class, 'id_undangan', 'id_undangan');
    }
}
