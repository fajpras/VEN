@extends('admin.layouts.app')

@section('title', __('admin.settings.title'))
@section('page_title', __('admin.settings.title'))

@section('content')
@php
    $avatarFallback = \App\Support\AdminMedia::avatarFallback($admin->nama);
    $avatarUrl = \App\Support\AdminMedia::imageUrl($admin->foto_profil) ?? $avatarFallback;
@endphp

<div class="mx-auto w-full max-w-5xl space-y-7">
    {{-- Ringkasan profil administrator --}}
    <section class="ven-card flex flex-col gap-5 p-6 sm:flex-row sm:items-center sm:p-7">
        <div class="relative shrink-0">
            <img src="{{ $avatarUrl }}"
                 alt="{{ $admin->nama }}"
                 referrerpolicy="no-referrer"
                 onerror="this.onerror=null;this.src='{{ $avatarFallback }}';"
                 class="h-[72px] w-[72px] rounded-full object-cover ring-4 ring-[#BA18F5]/10">
            <span class="absolute -bottom-0.5 -right-0.5 h-4 w-4 rounded-full border-[3px] border-surface bg-emerald-500"
                  aria-hidden="true"></span>
        </div>
        <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
                <h2 class="truncate text-xl font-semibold tracking-tight text-ink dark:text-white">{{ $admin->nama }}</h2>
                <span class="inline-flex items-center rounded-full border border-[#BA18F5]/20 bg-[#BA18F5]/8 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-[#A315D4] dark:text-[#D574FF]">
                    {{ __('admin.settings.profile_role') }}
                </span>
            </div>
            <p class="mt-1.5 truncate text-sm text-muted dark:text-zinc-400">
                {{ '@'.$admin->nama_panggilan }} <span class="mx-1.5 text-[#C4C4CD]">·</span> {{ $admin->email }}
            </p>
            <p class="mt-1 text-xs leading-relaxed text-muted/80 dark:text-zinc-500">
                {{ __('admin.settings.subtitle') }}
            </p>
        </div>
    </section>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {{-- Ubah alamat email --}}
        <section class="ven-card flex flex-col p-6 sm:p-7">
            <header class="flex items-start gap-3.5 border-b border-line pb-5 dark:border-[#2A2A2E]">
                <span class="ven-icon-tile">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 8.5l8.48 5.65a2.6 2.6 0 002.04 0L21 8.5M5.5 19h13a2.5 2.5 0 002.5-2.5v-9A2.5 2.5 0 0018.5 5h-13A2.5 2.5 0 003 7.5v9A2.5 2.5 0 005.5 19z"></path>
                    </svg>
                </span>
                <div class="min-w-0">
                    <h3 class="text-[15px] font-semibold tracking-tight text-ink dark:text-white">{{ __('admin.settings.email_section') }}</h3>
                    <p class="mt-1 text-xs leading-relaxed text-muted dark:text-zinc-400">{{ __('admin.settings.email_desc') }}</p>
                </div>
            </header>

            <form id="adminEmailForm"
                  method="POST"
                  action="{{ route('admin.settings.email') }}"
                  class="mt-5 flex flex-1 flex-col">
                @csrf
                @method('PUT')

                <div class="flex-1 space-y-5">
                    <div>
                        <label for="adminEmail" class="mb-2 block text-[13px] font-semibold text-ink/85 dark:text-zinc-300">
                            {{ __('admin.settings.new_email') }}
                        </label>
                        <input id="adminEmail"
                               type="email"
                               name="email"
                               required
                               autocomplete="email"
                               value="{{ old('email', $admin->email) }}"
                               class="ven-field {{ $errors->emailUpdate->has('email') ? 'is-invalid' : '' }}">
                        <p id="adminEmailClientError" class="mt-2 hidden text-xs font-medium text-rose-600 dark:text-rose-400">{{ __('admin.settings.err_invalid_email') }}</p>
                        @error('email', 'emailUpdate')
                            <p class="mt-2 text-xs font-medium text-rose-600 dark:text-rose-400">{{ $message }}</p>
                        @enderror
                    </div>

                    <div>
                        <label for="emailCurrentPass" class="mb-2 block text-[13px] font-semibold text-ink/85 dark:text-zinc-300">
                            {{ __('admin.settings.current_password') }}
                        </label>
                        <div class="relative">
                            <input id="emailCurrentPass"
                                   type="password"
                                   name="kata_sandi_saat_ini"
                                   required
                                   autocomplete="current-password"
                                   class="ven-field pr-12 {{ $errors->emailUpdate->has('kata_sandi_saat_ini') ? 'is-invalid' : '' }}">
                            <button type="button"
                                    data-toggle-password="emailCurrentPass"
                                    aria-label="{{ __('admin.settings.toggle_password') }}"
                                    class="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-[#9A9AA6] transition-colors hover:bg-[#BA18F5]/8 hover:text-[#BA18F5]">
                                <svg data-eye-open class="h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"></path>
                                    <circle cx="12" cy="12" r="3" stroke-width="1.8"></circle>
                                </svg>
                                <svg data-eye-closed class="hidden h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 3l18 18M10.6 6.1A9.8 9.8 0 0112 6c5 0 8.5 6 8.5 6a16 16 0 01-2.2 3M6.3 7.6A15.7 15.7 0 003.5 12S6.9 18 12 18a8.9 8.9 0 003.3-.6M9.9 9.9a3 3 0 004.2 4.2"></path>
                                </svg>
                            </button>
                        </div>
                        @error('kata_sandi_saat_ini', 'emailUpdate')
                            <p class="mt-2 text-xs font-medium text-rose-600 dark:text-rose-400">{{ $message }}</p>
                        @enderror
                    </div>
                </div>

                <div class="mt-7">
                    <button id="adminEmailSubmit" type="submit" class="ven-primary-btn">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                        </svg>
                        <span>{{ __('admin.settings.save_email') }}</span>
                    </button>
                </div>
            </form>
        </section>

        {{-- Ubah kata sandi --}}
        <section class="ven-card flex flex-col p-6 sm:p-7">
            <header class="flex items-start gap-3.5 border-b border-line pb-5 dark:border-[#2A2A2E]">
                <span class="ven-icon-tile">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="5" y="10" width="14" height="10" rx="2.5" stroke-width="1.8"></rect>
                        <path stroke-width="1.8" stroke-linecap="round" d="M8.5 10V7.5a3.5 3.5 0 017 0V10"></path>
                        <circle cx="12" cy="15" r="1.25" fill="currentColor" stroke="none"></circle>
                    </svg>
                </span>
                <div class="min-w-0">
                    <h3 class="text-[15px] font-semibold tracking-tight text-ink dark:text-white">{{ __('admin.settings.password_section') }}</h3>
                    <p class="mt-1 text-xs leading-relaxed text-muted dark:text-zinc-400">{{ __('admin.settings.password_desc') }}</p>
                </div>
            </header>

            <form id="adminPasswordForm"
                  method="POST"
                  action="{{ route('admin.settings.password') }}"
                  class="mt-5 flex flex-1 flex-col">
                @csrf
                @method('PUT')

                <div class="flex-1 space-y-5">
                    <div>
                        <label for="passCurrent" class="mb-2 block text-[13px] font-semibold text-ink/85 dark:text-zinc-300">
                            {{ __('admin.settings.current_password') }}
                        </label>
                        <div class="relative">
                            <input id="passCurrent"
                                   type="password"
                                   name="kata_sandi_saat_ini"
                                   required
                                   autocomplete="current-password"
                                   class="ven-field pr-12 {{ $errors->passwordUpdate->has('kata_sandi_saat_ini') ? 'is-invalid' : '' }}">
                            <button type="button"
                                    data-toggle-password="passCurrent"
                                    aria-label="{{ __('admin.settings.toggle_password') }}"
                                    class="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-[#9A9AA6] transition-colors hover:bg-[#BA18F5]/8 hover:text-[#BA18F5]">
                                <svg data-eye-open class="h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"></path>
                                    <circle cx="12" cy="12" r="3" stroke-width="1.8"></circle>
                                </svg>
                                <svg data-eye-closed class="hidden h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 3l18 18M10.6 6.1A9.8 9.8 0 0112 6c5 0 8.5 6 8.5 6a16 16 0 01-2.2 3M6.3 7.6A15.7 15.7 0 003.5 12S6.9 18 12 18a8.9 8.9 0 003.3-.6M9.9 9.9a3 3 0 004.2 4.2"></path>
                                </svg>
                            </button>
                        </div>
                        @error('kata_sandi_saat_ini', 'passwordUpdate')
                            <p class="mt-2 text-xs font-medium text-rose-600 dark:text-rose-400">{{ $message }}</p>
                        @enderror
                    </div>

                    <div>
                        <label for="passNew" class="mb-2 block text-[13px] font-semibold text-ink/85 dark:text-zinc-300">
                            {{ __('admin.settings.new_password') }}
                        </label>
                        <div class="relative">
                            <input id="passNew"
                                   type="password"
                                   name="kata_sandi"
                                   required
                                   minlength="8"
                                   autocomplete="new-password"
                                   class="ven-field pr-12 {{ $errors->passwordUpdate->has('kata_sandi') ? 'is-invalid' : '' }}">
                            <button type="button"
                                    data-toggle-password="passNew"
                                    aria-label="{{ __('admin.settings.toggle_password') }}"
                                    class="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-[#9A9AA6] transition-colors hover:bg-[#BA18F5]/8 hover:text-[#BA18F5]">
                                <svg data-eye-open class="h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"></path>
                                    <circle cx="12" cy="12" r="3" stroke-width="1.8"></circle>
                                </svg>
                                <svg data-eye-closed class="hidden h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 3l18 18M10.6 6.1A9.8 9.8 0 0112 6c5 0 8.5 6 8.5 6a16 16 0 01-2.2 3M6.3 7.6A15.7 15.7 0 003.5 12S6.9 18 12 18a8.9 8.9 0 003.3-.6M9.9 9.9a3 3 0 004.2 4.2"></path>
                                </svg>
                            </button>
                        </div>
                        <p id="passSameError" class="mt-2 hidden text-xs font-medium text-rose-600 dark:text-rose-400">{{ __('admin.settings.err_same_password') }}</p>
                        @error('kata_sandi', 'passwordUpdate')
                            <p class="mt-2 text-xs font-medium text-rose-600 dark:text-rose-400">{{ $message }}</p>
                        @enderror

                        <ul id="passRulesList" class="mt-3 space-y-2">
                            <li data-rule="min" class="ven-rule">
                                <span data-rule-icon class="grid h-[18px] w-[18px] shrink-0 place-items-center rounded-full border border-current text-[10px] font-bold leading-none"></span>
                                <span>{{ __('admin.settings.rule_min') }}</span>
                            </li>
                            <li data-rule="upper" class="ven-rule">
                                <span data-rule-icon class="grid h-[18px] w-[18px] shrink-0 place-items-center rounded-full border border-current text-[10px] font-bold leading-none"></span>
                                <span>{{ __('admin.settings.rule_upper') }}</span>
                            </li>
                            <li data-rule="lower" class="ven-rule">
                                <span data-rule-icon class="grid h-[18px] w-[18px] shrink-0 place-items-center rounded-full border border-current text-[10px] font-bold leading-none"></span>
                                <span>{{ __('admin.settings.rule_lower') }}</span>
                            </li>
                            <li data-rule="digit" class="ven-rule">
                                <span data-rule-icon class="grid h-[18px] w-[18px] shrink-0 place-items-center rounded-full border border-current text-[10px] font-bold leading-none"></span>
                                <span>{{ __('admin.settings.rule_digit') }}</span>
                            </li>
                            <li data-rule="symbol" class="ven-rule">
                                <span data-rule-icon class="grid h-[18px] w-[18px] shrink-0 place-items-center rounded-full border border-current text-[10px] font-bold leading-none"></span>
                                <span>{{ __('admin.settings.rule_symbol') }}</span>
                            </li>
                        </ul>
                    </div>

                    <div>
                        <label for="passConfirm" class="mb-2 block text-[13px] font-semibold text-ink/85 dark:text-zinc-300">
                            {{ __('admin.settings.confirm_password') }}
                        </label>
                        <div class="relative">
                            <input id="passConfirm"
                                   type="password"
                                   name="kata_sandi_confirmation"
                                   required
                                   minlength="8"
                                   autocomplete="new-password"
                                   class="ven-field pr-12 {{ $errors->passwordUpdate->has('kata_sandi') ? 'is-invalid' : '' }}">
                            <button type="button"
                                    data-toggle-password="passConfirm"
                                    aria-label="{{ __('admin.settings.toggle_password') }}"
                                    class="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-[#9A9AA6] transition-colors hover:bg-[#BA18F5]/8 hover:text-[#BA18F5]">
                                <svg data-eye-open class="h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"></path>
                                    <circle cx="12" cy="12" r="3" stroke-width="1.8"></circle>
                                </svg>
                                <svg data-eye-closed class="hidden h-[18px] w-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 3l18 18M10.6 6.1A9.8 9.8 0 0112 6c5 0 8.5 6 8.5 6a16 16 0 01-2.2 3M6.3 7.6A15.7 15.7 0 003.5 12S6.9 18 12 18a8.9 8.9 0 003.3-.6M9.9 9.9a3 3 0 004.2 4.2"></path>
                                </svg>
                            </button>
                        </div>
                        <p id="passConfirmError" class="mt-2 hidden text-xs font-medium text-rose-600 dark:text-rose-400">{{ __('admin.settings.err_confirm_mismatch') }}</p>
                    </div>
                </div>

                <div class="mt-7">
                    <button id="adminPasswordSubmit" type="submit" disabled class="ven-primary-btn">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <rect x="5" y="10" width="14" height="10" rx="2.5" stroke-width="1.8"></rect>
                            <path stroke-width="1.8" stroke-linecap="round" d="M8.5 10V7.5a3.5 3.5 0 017 0V10"></path>
                        </svg>
                        <span>{{ __('admin.settings.save_password') }}</span>
                    </button>
                </div>
            </form>
        </section>
    </div>
</div>

@push('scripts')
<script>
    const field = (id) => document.getElementById(id);
    const setInvalid = (input, invalid) => input?.classList.toggle('is-invalid', invalid);

    // Tampilkan / sembunyikan kata sandi.
    document.querySelectorAll('[data-toggle-password]').forEach((button) => {
        button.addEventListener('click', () => {
            const input = field(button.dataset.togglePassword);
            if (!input) return;

            const revealed = input.type === 'password';
            input.type = revealed ? 'text' : 'password';
            button.querySelector('[data-eye-open]')?.classList.toggle('hidden', revealed);
            button.querySelector('[data-eye-closed]')?.classList.toggle('hidden', !revealed);
        });
    });

    // Validasi email secara langsung.
    const emailInput = field('adminEmail');
    const emailPassword = field('emailCurrentPass');
    const emailError = field('adminEmailClientError');
    const emailSubmit = field('adminEmailSubmit');
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    function validateEmailForm() {
        const value = (emailInput?.value ?? '').trim();
        const invalid = value.length > 0 && !emailPattern.test(value);

        setInvalid(emailInput, invalid);
        emailError?.classList.toggle('hidden', !invalid);

        if (emailSubmit) {
            emailSubmit.disabled = !emailPattern.test(value) || (emailPassword?.value.length ?? 0) === 0;
        }
    }

    emailInput?.addEventListener('input', validateEmailForm);
    emailPassword?.addEventListener('input', validateEmailForm);
    validateEmailForm();

    // Validasi kata sandi secara langsung (5 aturan kekuatan).
    const currentPass = field('passCurrent');
    const newPass = field('passNew');
    const confirmPass = field('passConfirm');
    const sameError = field('passSameError');
    const confirmError = field('passConfirmError');
    const passwordSubmit = field('adminPasswordSubmit');

    function validatePasswordForm() {
        const current = currentPass?.value ?? '';
        const next = newPass?.value ?? '';
        const confirmation = confirmPass?.value ?? '';

        const checks = {
            min: next.length >= 8,
            upper: /[A-Z]/.test(next),
            lower: /[a-z]/.test(next),
            digit: /[0-9]/.test(next),
            symbol: /[^A-Za-z0-9]/.test(next),
        };

        let allStrong = true;
        Object.entries(checks).forEach(([rule, met]) => {
            if (!met) allStrong = false;

            const item = document.querySelector(`#passRulesList [data-rule="${rule}"]`);
            item?.classList.toggle('is-met', met);
            const icon = item?.querySelector('[data-rule-icon]');
            if (icon) icon.textContent = met ? '✓' : '';
        });

        const sameAsCurrent = current.length > 0 && next.length > 0 && next === current;
        const confirmationMismatch = confirmation.length > 0 && next !== confirmation;

        setInvalid(newPass, sameAsCurrent || (next.length > 0 && !allStrong));
        setInvalid(confirmPass, confirmationMismatch);
        sameError?.classList.toggle('hidden', !sameAsCurrent);
        confirmError?.classList.toggle('hidden', !confirmationMismatch);

        if (passwordSubmit) {
            passwordSubmit.disabled = !(current.length > 0 && allStrong && !sameAsCurrent && confirmation.length > 0 && next === confirmation);
        }
    }

    currentPass?.addEventListener('input', validatePasswordForm);
    newPass?.addEventListener('input', validatePasswordForm);
    confirmPass?.addEventListener('input', validatePasswordForm);
    validatePasswordForm();
</script>
@endpush
@endsection
