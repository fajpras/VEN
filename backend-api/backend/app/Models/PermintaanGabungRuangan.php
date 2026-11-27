<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class PermintaanGabungRuangan extends Model
{
    protected $table = 'permintaan_gabung_ruangan';
    protected $primaryKey = 'id_permintaan';

    protected $fillable = [
        'id_undangan',
        'id_ruangan',
        'id_pengguna',
        'status',
    ];

    public function undangan(): BelongsTo
    {
        return $this->belongsTo(Undangan::class, 'id_undangan', 'id_undangan');
    }

    public function ruangan(): BelongsTo
    {
        return $this->belongsTo(Ruangan::class, 'id_ruangan', 'id_ruangan');
    }

    public function pengguna(): BelongsTo
    {
        return $this->belongsTo(Pengguna::class, 'id_pengguna', 'id_pengguna');
    }
}
