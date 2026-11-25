<?php

namespace App\Exceptions;

use App\Http\Middleware\SinkronBahasaApi;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Http\Exceptions\HttpResponseException;
use Illuminate\Http\Exceptions\PostTooLargeException;
use Illuminate\Http\Exceptions\ThrottleRequestsException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Routing\Exceptions\InvalidSignatureException;
use Illuminate\Session\TokenMismatchException;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\Exception\MethodNotAllowedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Throwable;

/**
 * Menyeragamkan seluruh respons galat untuk endpoint /api/* menjadi JSON.
 *
 * Tujuannya agar aplikasi klien (Android/Unity/Postman) tidak pernah menerima
 * halaman HTML, redirect ke halaman login web, atau stack trace ketika sebuah
 * endpoint tidak dapat diakses. Semua galat dikembalikan dengan bentuk yang
 * sama seperti respons sukses, yaitu { data, message }.
 */
final class PenanganKesalahanApi
{
    public const PESAN_SESI_BERAKHIR = 'Sesi berakhir atau token tidak valid. Silakan masuk kembali.';

    /** Pesan bawaan framework yang tidak perlu ditampilkan ke pengguna. */
    private const PESAN_BAWAAN = [
        'This action is unauthorized.',
        'Unauthenticated.',
        'Malformed URL.',
        'The page has expired due to inactivity.',
    ];

    /**
     * Ubah exception menjadi respons untuk request /api/*.
     *
     * - Browser yang membuka endpoint langsung dialihkan ke halaman 404 agar
     *   tidak melihat JSON mentah.
     * - Klien API (Android/Unity/Postman) tetap menerima JSON.
     *
     * Mengembalikan null bila request bukan dari API, sehingga penanganan
     * bawaan Laravel (halaman web) tetap dipakai.
     */
    public static function render(Throwable $e, Request $request): ?Response
    {
        if (! $request->is('api', 'api/*')) {
            return null;
        }

        // Biarkan Laravel mengembalikan respons yang sudah dibentuk sendiri.
        if ($e instanceof HttpResponseException) {
            return null;
        }

        if (self::dariBrowser($request)) {
            return redirect('/404');
        }

        SinkronBahasaApi::terapkanDariRequest($request);

        return self::responsJson($e);
    }

    /**
     * Deteksi navigasi halaman dari browser, bukan panggilan API.
     *
     * Browser mengirim Sec-Fetch-Mode: navigate saat membuka alamat langsung,
     * lengkap dengan Accept text/html dan User-Agent Mozilla. Klien API
     * (Android/Unity/Postman/curl) tidak memenuhi kombinasi tersebut, sehingga
     * tetap menerima JSON.
     */
    private static function dariBrowser(Request $request): bool
    {
        if (strtolower((string) $request->header('Sec-Fetch-Mode')) === 'navigate') {
            return true;
        }

        $accept = strtolower((string) $request->header('Accept'));
        $agen = strtolower((string) $request->header('User-Agent'));

        return str_contains($accept, 'text/html') && str_contains($agen, 'mozilla');
    }

    private static function responsJson(Throwable $e): JsonResponse
    {
        return match (true) {
            $e instanceof AuthenticationException => self::buat(401, __('api.error.401')),
            $e instanceof ValidationException => self::buat(
                status: 422,
                pesan: $e->getMessage(),
                data: $e->errors(),
                // Dipertahankan agar klien lama yang membaca "errors" tetap bekerja.
                galat: $e->errors(),
            ),
            $e instanceof InvalidSignatureException => self::buat(403, __('api.error.invalid_signature')),
            $e instanceof PostTooLargeException => self::buat(413, __('api.error.413')),
            $e instanceof ThrottleRequestsException => self::buat(
                429,
                __('api.error.429'),
                null,
                $e->getHeaders(),
            ),
            $e instanceof NotFoundHttpException => self::buat(404, __('api.error.404')),
            $e instanceof MethodNotAllowedHttpException => self::buat(
                405,
                __('api.error.405'),
                null,
                $e->getHeaders(),
            ),
            $e instanceof TokenMismatchException => self::buat(419, __('api.error.419')),
            $e instanceof HttpExceptionInterface => self::buatHttp($e),
            // Jangan pernah bocorkan detail internal (query SQL, path, dsb.) ke klien.
            default => self::buat(500, __('api.error.500')),
        };
    }

    /**
     * Bentuk respons galat yang konsisten dengan envelope API VEN.
     */
    public static function buat(
        int $status,
        string $pesan,
        mixed $data = null,
        array $headers = [],
        array $galat = [],
    ): JsonResponse {
        $isi = ['data' => $data, 'message' => $pesan];

        if ($galat !== []) {
            $isi['errors'] = $galat;
        }

        return response()->json(
            $isi,
            $status,
            $headers,
            JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES,
        );
    }

    private static function buatHttp(HttpExceptionInterface $e): JsonResponse
    {
        $status = $e->getStatusCode();
        $pesan = trim($e->getMessage());

        // Pesan galat 4xx berasal dari kode aplikasi (mis. abort(403, '...')),
        // sehingga aman ditampilkan. Galat 5xx selalu memakai pesan umum agar
        // detail internal tidak bocor.
        if ($status >= 500 || $pesan === '' || in_array($pesan, self::PESAN_BAWAAN, true)) {
            $pesan = self::pesanStatus($status);
        }

        return self::buat($status, $pesan, null, $e->getHeaders());
    }

    private static function pesanStatus(int $status): string
    {
        return match ($status) {
            400 => __('api.error.400'),
            401 => __('api.error.401'),
            403 => __('api.error.403'),
            404 => __('api.error.404'),
            405 => __('api.error.405'),
            408 => __('api.error.408'),
            413 => __('api.error.413'),
            415 => __('api.error.415'),
            419 => __('api.error.419'),
            422 => __('api.error.422'),
            429 => __('api.error.429'),
            502, 503, 504 => __('api.error.503'),
            default => $status >= 500
                ? __('api.error.500')
                : (Response::$statusTexts[$status] ?? __('api.error.400')),
        };
    }
}
