<?php

namespace App\Services\Edgegap;

use Illuminate\Http\Client\Response;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use RuntimeException;

/**
 * Mengelola siklus hidup dedicated server multiplayer Unity di Edgegap per ruangan pameran.
 *
 * Jika sudah ada deployment aktif untuk room yang sama, deployment tersebut dipakai ulang
 * agar seluruh pengunjung di ruangan yang sama terhubung ke instance server yang sama.
 */
final class LayananEdgegapServer
{
    private const CACHE_PREFIX = 'edgegap:room_deployment:';
    private const CACHE_TTL_SECONDS = 3600;
    private const FALLBACK_PUBLIC_IP = '1.1.1.1';

    public function __construct(
        private readonly ?string $baseUrl = null,
        private readonly ?string $apiToken = null,
        private readonly ?string $appName = null,
        private readonly ?string $appVersion = null,
    ) {}

    public function terkonfigurasi(): bool
    {
        return filled($this->token()) && filled($this->appName()) && filled($this->appVersion());
    }

    /**
     * Memastikan ada deployment Edgegap untuk $roomId dan mengembalikan status + host/port terbaru.
     *
     * @return array{request_id: string, room_id: string, ready: bool, status: string, host: ?string, public_ip: ?string, port: ?int}
     */
    public function sediakanServer(string $roomId, ?string $clientIp = null): array
    {
        if (! $this->terkonfigurasi()) {
            throw new RuntimeException('Layanan multiplayer Edgegap belum dikonfigurasi.');
        }

        $cacheKey = self::CACHE_PREFIX.$roomId;
        $cachedRequestId = Cache::get($cacheKey);

        if (is_string($cachedRequestId) && $cachedRequestId !== '') {
            $existing = $this->cekStatus($cachedRequestId, $roomId);
            if ($existing !== null) {
                return $existing;
            }
            Cache::forget($cacheKey);
        }

        $byTag = $this->cariDeploymentAktifBerdasarkanTag($roomId);
        if ($byTag !== null) {
            Cache::put($cacheKey, $byTag['request_id'], self::CACHE_TTL_SECONDS);

            return $byTag;
        }

        return Cache::lock('lock:'.$cacheKey, 15)->block(10, function () use ($cacheKey, $roomId, $clientIp) {
            $cachedAfterLock = Cache::get($cacheKey);
            if (is_string($cachedAfterLock) && $cachedAfterLock !== '') {
                $existing = $this->cekStatus($cachedAfterLock, $roomId);
                if ($existing !== null) {
                    return $existing;
                }
            }

            $ipTarget = $this->resolusiIpPublik($clientIp);

            $response = $this->http()->post($this->url('/v1/deploy'), [
                'app_name' => $this->appName(),
                'version_name' => $this->appVersion(),
                'ip_list' => [$ipTarget],
                'tags' => ['ven', $this->tagRoom($roomId)],
                'env_vars' => [
                    [
                        'key' => 'ROOM_ID',
                        'value' => $roomId,
                        'is_hidden' => false,
                    ],
                ],
            ]);

            if ($response->failed()) {
                throw new RuntimeException('Gagal memulai server Edgegap (HTTP '.$response->status().').');
            }

            $requestId = (string) $response->json('request_id', '');
            if ($requestId === '') {
                throw new RuntimeException('Edgegap tidak mengembalikan request_id deployment.');
            }

            Cache::put($cacheKey, $requestId, self::CACHE_TTL_SECONDS);

            return $this->cekStatus($requestId, $roomId) ?? [
                'request_id' => $requestId,
                'room_id' => $roomId,
                'ready' => false,
                'status' => 'Status.INITIALIZING',
                'host' => null,
                'public_ip' => null,
                'port' => null,
            ];
        });
    }

    /**
     * @return array{request_id: string, room_id: string, ready: bool, status: string, host: ?string, public_ip: ?string, port: ?int}|null
     */
    public function cekStatus(string $requestId, string $roomId): ?array
    {
        $response = $this->http()->get($this->url('/v1/status/'.urlencode($requestId)));

        if ($response->status() === 404 || $response->status() === 410 || $response->failed()) {
            return null;
        }

        $data = $response->json();
        $status = (string) ($data['current_status'] ?? $data['status'] ?? '');

        if (in_array($status, ['Status.TERMINATED', 'Status.STOPPED', 'Status.ERROR'], true)) {
            return null;
        }

        return $this->formatPayload($data, $requestId, $roomId, $status);
    }

    private function cariDeploymentAktifBerdasarkanTag(string $roomId): ?array
    {
        $response = $this->http()->get($this->url('/v1/deployments'));
        if ($response->failed()) {
            return null;
        }

        $targetTag = $this->tagRoom($roomId);
        foreach ((array) $response->json('data', []) as $item) {
            $tags = (array) ($item['tags'] ?? []);
            $status = (string) ($item['status'] ?? '');
            if (! in_array($targetTag, $tags, true)) {
                continue;
            }
            if (in_array($status, ['Status.TERMINATED', 'Status.STOPPED', 'Status.ERROR'], true)) {
                continue;
            }
            $reqId = (string) ($item['request_id'] ?? '');
            if ($reqId !== '') {
                return $this->formatPayload($item, $reqId, $roomId, $status);
            }
        }

        return null;
    }

    private function formatPayload(array $data, string $requestId, string $roomId, string $status): array
    {
        $portInfo = $data['ports']['gameport'] ?? null;
        if ($portInfo === null && ! empty($data['ports']) && is_array($data['ports'])) {
            $portInfo = reset($data['ports']) ?: null;
        }

        $externalPort = isset($portInfo['external']) ? (int) $portInfo['external'] : null;
        $ready = (bool) ($data['ready'] ?? ($status === 'Status.READY')) && $externalPort !== null && $externalPort > 0;

        return [
            'request_id' => $requestId,
            'room_id' => $roomId,
            'ready' => $ready,
            'status' => $status !== '' ? $status : ($ready ? 'Status.READY' : 'Status.INITIALIZING'),
            'host' => isset($data['fqdn']) ? (string) $data['fqdn'] : null,
            'public_ip' => isset($data['public_ip']) ? (string) $data['public_ip'] : null,
            'port' => $externalPort,
        ];
    }

    private function resolusiIpPublik(?string $ip): string
    {
        if (is_string($ip) && filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE) !== false) {
            return $ip;
        }

        return self::FALLBACK_PUBLIC_IP;
    }

    private function tagRoom(string $roomId): string
    {
        return 'room-'.preg_replace('/[^a-zA-Z0-9_-]/', '_', $roomId);
    }

    private function http()
    {
        return Http::timeout(15)
            ->acceptJson()
            ->withHeaders(['authorization' => $this->token()]);
    }

    private function url(string $path): string
    {
        $base = $this->baseUrl ?? (string) config('services.edgegap.url', 'https://api.edgegap.com');

        return rtrim($base, '/').'/'.ltrim($path, '/');
    }

    private function token(): string
    {
        $tok = trim((string) ($this->apiToken ?? config('services.edgegap.token', '')));
        if ($tok !== '' && ! str_starts_with(strtolower($tok), 'token ')) {
            return 'token '.$tok;
        }

        return $tok;
    }

    private function appName(): string
    {
        return trim((string) ($this->appName ?? config('services.edgegap.app_name', '')));
    }

    private function appVersion(): string
    {
        return trim((string) ($this->appVersion ?? config('services.edgegap.app_version', '')));
    }
}
