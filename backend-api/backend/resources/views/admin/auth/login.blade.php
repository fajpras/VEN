<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <meta name="description" content="Sign in to the VEN Admin Panel to manage exhibitions, posts, accounts, and activity logs.">
    <title>Sign In - VEN Admin Panel</title>
    <link rel="icon" type="image/svg+xml" href="{{ asset('assets/admin/logo-ven.svg') }}">
    <link rel="preload" as="image" href="{{ asset('assets/admin/ven-glow.webp') }}">
    @vite(['resources/css/app.css'])
    {{ Vite::fonts() }}
</head>
<body class="relative min-h-screen w-full flex items-center justify-center overflow-hidden bg-white select-none">
    {{-- Pre-rendered concentric gradient background (17 KB WebP; avoids costly CSS blur painting) --}}
    <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div class="absolute w-[1400px] h-[1400px] bg-center bg-no-repeat bg-contain" style="background-image: url('{{ asset('assets/admin/ven-glow.webp') }}');"></div>
    </div>

    {{-- Floating 3D Cubes (inline SVG for zero network requests & fast LCP) --}}
    {{-- 1. Large Isometric Block (Bottom-Left) --}}
    <div class="absolute -bottom-8 -left-12 w-[340px] sm:w-[460px] md:w-[540px] pointer-events-none z-10 animate-float-slow" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 592 317" fill="none" class="w-full h-auto drop-shadow-xl opacity-95" focusable="false">
            <path fill="#FFFFFF" d="M0 267.4l372.12-163.28 196.64 418.25-348.8 141.69L0 267.4Z" />
            <path fill="#FFFFFF" d="M150.73 124.6L440.3 0l-68.2 104.12L0 267.4l150.73-142.8Z" />
            <path fill="#EBEBEB" d="M372.12 104.12L440.32 0 591.7 411.8l-22.95 110.57-196.64-418.25Z" />
            <path stroke="#EBEBEB" stroke-width="3" d="M566.73 521.57l-346.1 140.6L2.1 268.12l369.27-162.04 195.35 415.5Zm23.42-109.66L568.2 517.66 373.83 104.24l66.1-100.92L590.15 411.9ZM371.1 102.92L7.28 262.57l144.29-136.7L436.32 3.36l-65.22 99.57Z" />
        </svg>
    </div>

    {{-- 2. Large Isometric Block (Bottom-Right, Mirrored) --}}
    <div class="absolute -bottom-8 -right-12 w-[340px] sm:w-[460px] md:w-[540px] pointer-events-none z-10 scale-x-[-1] animate-float-slow" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 592 317" fill="none" class="w-full h-auto drop-shadow-xl opacity-95" focusable="false">
            <path fill="#FFFFFF" d="M0 267.4l372.12-163.28 196.64 418.25-348.8 141.69L0 267.4Z" />
            <path fill="#FFFFFF" d="M150.73 124.6L440.3 0l-68.2 104.12L0 267.4l150.73-142.8Z" />
            <path fill="#EBEBEB" d="M372.12 104.12L440.32 0 591.7 411.8l-22.95 110.57-196.64-418.25Z" />
            <path stroke="#EBEBEB" stroke-width="3" d="M566.73 521.57l-346.1 140.6L2.1 268.12l369.27-162.04 195.35 415.5Zm23.42-109.66L568.2 517.66 373.83 104.24l66.1-100.92L590.15 411.9ZM371.1 102.92L7.28 262.57l144.29-136.7L436.32 3.36l-65.22 99.57Z" />
        </svg>
    </div>

    {{-- 3. Small Isometric Block (Mid-Left Floating) --}}
    <div class="absolute top-[42%] left-[8%] sm:left-[14%] w-[110px] sm:w-[150px] md:w-[170px] pointer-events-none z-10 -rotate-12 animate-float-fast" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 201 194" fill="none" class="w-full h-auto drop-shadow-xl" focusable="false">
            <path fill="#FFFFFF" d="M81.28 3.5l92.04 113.25-103.42 71L4.75 86.1 81.28 3.5Z" />
            <path fill="#FFFFFF" d="M135.87 44.85l59.8 70.78-22.34 1.12L81.27 3.5l54.59 41.35Z" />
            <path fill="#EBEBEB" d="M173.32 116.75l22.36-1.12-82.4 58.84-43.38 13.27 103.42-71Z" />
            <path stroke="#EBEBEB" stroke-width="3" d="M70.33 185.63L6.64 86.27 81.21 5.78l89.93 110.65-100.81 69.2Zm42.27-12.53l-33.8 10.35 95.02-65.22 16.82-0.85-78.04 55.72Zm61.4-57.89l-83.7-103 44.53 33.74 57.75 68.33-18.57 0.93Z" />
        </svg>
    </div>

    {{-- 4. Small Isometric Block (Mid-Right Floating) --}}
    <div class="absolute top-[45%] right-[8%] sm:right-[14%] w-[110px] sm:w-[150px] md:w-[170px] pointer-events-none z-10 rotate-12 animate-float-fast" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 201 194" fill="none" class="w-full h-auto drop-shadow-xl" focusable="false">
            <path fill="#FFFFFF" d="M81.28 3.5l92.04 113.25-103.42 71L4.75 86.1 81.28 3.5Z" />
            <path fill="#FFFFFF" d="M135.87 44.85l59.8 70.78-22.34 1.12L81.27 3.5l54.59 41.35Z" />
            <path fill="#EBEBEB" d="M173.32 116.75l22.36-1.12-82.4 58.84-43.38 13.27 103.42-71Z" />
            <path stroke="#EBEBEB" stroke-width="3" d="M70.33 185.63L6.64 86.27 81.21 5.78l89.93 110.65-100.81 69.2Zm42.27-12.53l-33.8 10.35 95.02-65.22 16.82-0.85-78.04 55.72Zm61.4-57.89l-83.7-103 44.53 33.74 57.75 68.33-18.57 0.93Z" />
        </svg>
    </div>

    {{-- Center Login Card --}}
    <main class="relative z-20 w-full max-w-[420px] mx-4 bg-white rounded-2xl p-8 sm:p-10 shadow-[0_12px_40px_rgba(0,0,0,0.12)] border border-gray-100">
        <h1 class="sr-only">Sign In - VEN Admin Panel</h1>
        <form method="POST" action="{{ route('admin.login.submit') }}" class="space-y-5">
            @csrf

            @php $hasLoginError = isset($errors) && $errors->any(); @endphp

            @if ($hasLoginError)
                <div class="p-3 text-sm text-rose-600 bg-rose-50 border border-rose-200 rounded-xl">
                    {{ $errors->first() }}
                </div>
            @endif

            {{-- Email Input --}}
            <div>
                <label for="email" class="sr-only">Email</label>
                <input type="email"
                       id="email"
                       name="email"
                       value="{{ old('email') }}"
                       placeholder="Email"
                       autocomplete="email"
                       required
                       class="w-full h-12 px-4 rounded-xl bg-[#EAEAEA] text-[#0F172A] placeholder-[#8E8E93] text-sm font-medium border {{ $hasLoginError ? 'border-rose-500 ring-2 ring-rose-500/20 bg-white' : 'border-transparent focus:border-[#BA18F5] focus:bg-white' }} outline-none transition-all">
            </div>

            {{-- Password Input with Visibility Toggle --}}
            <div class="relative">
                <label for="passwordInput" class="sr-only">Password</label>
                <input type="password"
                       id="passwordInput"
                       name="password"
                       placeholder="Password"
                       autocomplete="current-password"
                       required
                       class="w-full h-12 pl-4 pr-12 rounded-xl bg-[#EAEAEA] text-[#0F172A] placeholder-[#8E8E93] text-sm font-medium border {{ $hasLoginError ? 'border-rose-500 ring-2 ring-rose-500/20 bg-white' : 'border-transparent focus:border-[#BA18F5] focus:bg-white' }} outline-none transition-all">
                <button type="button"
                        id="togglePasswordButton"
                        onclick="togglePasswordVisibility()"
                        aria-label="Show password"
                        aria-pressed="false"
                        class="absolute right-3 top-1/2 -translate-y-1/2 p-2 text-gray-500 hover:text-black transition-colors cursor-pointer">
                    <svg id="eyeIcon" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                </button>
            </div>

            {{-- Forgot Password Link --}}
            <div class="text-right">
                <a href="#"
                   onclick="alert('Please contact the super administrator to reset your password.'); return false;"
                   class="text-xs text-[#0F172A] hover:text-[#BA18F5] font-medium transition-colors">
                    Forgot Password?
                </a>
            </div>

            {{-- Submit Button --}}
            <button type="submit"
                    class="w-full h-12 rounded-xl bg-gradient-to-r from-[#BA18F5] to-[#D257FF] text-white font-semibold text-base hover:opacity-95 transition-opacity flex items-center justify-center cursor-pointer">
                Sign In
            </button>
        </form>
    </main>

    <script>
        function togglePasswordVisibility() {
            const input = document.getElementById('passwordInput');
            const button = document.getElementById('togglePasswordButton');
            const isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(!isHidden));
            button.setAttribute('aria-label', isHidden ? 'Hide password' : 'Show password');
        }
    </script>
</body>
</html>
