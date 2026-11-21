<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

final class KesalahanVoice extends RuntimeException
{
    private function __construct(string $pesan, private readonly int $kodeStatus)
    {
        parent::__construct($pesan);
    }

    public static function hanyaPrivat(): self
    {
        return new self('Voice chat hanya tersedia di ruangan privat.', 403);
    }

    public static function tanpaSesiAktif(): self
    {
        return new self('Anda tidak memiliki sesi diskusi aktif di ruangan ini.', 403);
    }

    public function kodeStatus(): int
    {
        return $this->kodeStatus;
    }

    public function render(): JsonResponse
    {
        return response()->json(['data' => null, 'message' => $this->getMessage()], $this->kodeStatus);
    }
}
