<?php

namespace App\Http\Controllers\Admin\Web;

use App\Http\Controllers\Controller;
use App\Models\Pengguna;
use App\Services\LogSistem;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Illuminate\Validation\Rule;
use Illuminate\Validation\Rules\Password;
use Illuminate\View\View;

class AdminSettingsController extends Controller
{
    public function switchLocale(Request $request, string $locale): RedirectResponse
    {
        $normalized = in_array(strtolower($locale), ['id', 'en'], true) ? strtolower($locale) : 'en';
        $request->session()->put('admin_locale', $normalized);

        return redirect()->back(fallback: route('admin.accounts'));
    }

    public function index(Request $request): View
    {
        /** @var Pengguna $admin */
        $admin = $request->user('web');

        return view('admin.settings.index', compact('admin'));
    }

    public function updateEmail(Request $request): RedirectResponse
    {
        /** @var Pengguna $admin */
        $admin = $request->user('web');

        $validated = $request->validateWithBag('emailUpdate', [
            'email' => [
                'required',
                'string',
                'email',
                'max:254',
                Rule::unique('pengguna', 'email')->ignore($admin->id_pengguna, 'id_pengguna'),
            ],
            'kata_sandi_saat_ini' => ['required', 'string', 'max:72'],
        ]);

        if (! Hash::check($validated['kata_sandi_saat_ini'], (string) $admin->kata_sandi)) {
            return redirect()
                ->route('admin.settings')
                ->withErrors(['kata_sandi_saat_ini' => __('admin.settings.wrong_password')], 'emailUpdate')
                ->withInput($request->only('email'));
        }

        $emailLama = $admin->email;
        $admin->forceFill(['email' => Str::lower(trim($validated['email']))])->save();

        LogSistem::aktivitas(
            entitas: $admin->nama,
            jenis: 'profil',
            pesan: "Admin mengubah email dari {$emailLama} ke {$admin->email}",
            status: 'berhasil',
            idPengguna: $admin->id_pengguna,
            aksi: 'ubah',
        );

        return redirect()
            ->route('admin.settings')
            ->with('success', __('admin.settings.email_updated'));
    }

    public function updatePassword(Request $request): RedirectResponse
    {
        /** @var Pengguna $admin */
        $admin = $request->user('web');

        $validated = $request->validateWithBag('passwordUpdate', [
            'kata_sandi_saat_ini' => ['required', 'string', 'max:72'],
            'kata_sandi' => [
                'required',
                'string',
                'max:72',
                'different:kata_sandi_saat_ini',
                'confirmed',
                Password::min(8)->mixedCase()->numbers()->symbols(),
            ],
        ]);

        if (! Hash::check($validated['kata_sandi_saat_ini'], (string) $admin->kata_sandi)) {
            return redirect()
                ->route('admin.settings')
                ->withErrors(['kata_sandi_saat_ini' => __('admin.settings.wrong_password')], 'passwordUpdate');
        }

        $admin->forceFill(['kata_sandi' => $validated['kata_sandi']])->saveQuietly();

        LogSistem::aktivitas(
            entitas: $admin->nama,
            jenis: 'profil',
            pesan: 'Admin memperbarui kata sandi akun panel web',
            status: 'berhasil',
            idPengguna: $admin->id_pengguna,
            aksi: 'ubah',
        );

        LogSistem::keamanan(
            event: 'reset_kata_sandi',
            pesan: 'Admin memperbarui kata sandi melalui panel pengaturan',
            statusKode: 200,
            idPengguna: $admin->id_pengguna,
        );

        return redirect()
            ->route('admin.settings')
            ->with('success', __('admin.settings.password_updated'));
    }
}
