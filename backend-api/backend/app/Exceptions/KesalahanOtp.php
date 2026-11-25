<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

final class KesalahanOtp extends RuntimeException
{
    private function __construct(string $pesan, private readonly int $kodeStatus)
    {
        parent::__construct($pesan);
    }

    public static function tidakValid(): self
    {
        return new self(__('api.otp.invalid'), 422);
    }

    public static function terkunci(): self
    {
        return new self(__('api.otp.locked'), 429);
    }

    public static function terlaluSering(int $sisaDetik): self
    {
        return new self(__('api.otp.cooldown', ['seconds' => $sisaDetik]), 429);
    }

    public static function melampauiBatasPerJam(): self
    {
        return new self(__('api.otp.hourly_limit'), 429);
    }

    public static function gagalKirim(): self
    {
        return new self(__('api.otp.send_failed'), 503);
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
