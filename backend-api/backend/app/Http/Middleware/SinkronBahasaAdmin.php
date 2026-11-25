<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Menyelaraskan locale Panel Admin Web dari session 'admin_locale' (id / en).
 */
class SinkronBahasaAdmin
{
    public function handle(Request $request, Closure $next): Response
    {
        $locale = (string) $request->session()->get('admin_locale', 'en');
        if (! in_array($locale, ['en', 'id'], true)) {
            $locale = 'en';
        }

        app()->setLocale($locale);

        return $next($request);
    }
}
