@php
    $currentLocale = app()->getLocale();
    $isSettings = request()->routeIs('admin.settings*');
    $isArchive = request()->routeIs('admin.archive*');
@endphp

<header class="h-20 w-full flex items-center justify-between px-10 bg-canvas/80 dark:bg-[#121212]/70 backdrop-blur-md sticky top-0 z-20 transition-colors">
    <h1 class="text-xl font-semibold text-[#0F172A] dark:text-white tracking-tight">
        @yield('page_title', 'Dashboard')
    </h1>

    <div class="flex items-center gap-3">
        {{-- Language Switcher: ID | EN --}}
        <div class="inline-flex items-center p-1 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] border border-transparent select-none" role="group" aria-label="Language">
            <a href="{{ route('admin.locale', ['locale' => 'id']) }}"
               class="h-7 px-3 rounded-full text-xs font-semibold flex items-center justify-center transition-colors {{ $currentLocale === 'id' ? 'bg-[#BA18F5] text-white shadow-2xs' : 'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
                ID
            </a>
            <a href="{{ route('admin.locale', ['locale' => 'en']) }}"
               class="h-7 px-3 rounded-full text-xs font-semibold flex items-center justify-center transition-colors {{ $currentLocale !== 'id' ? 'bg-[#BA18F5] text-white shadow-2xs' : 'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
                EN
            </a>
        </div>

        {{-- Archive / Restore Shortcut --}}
        <a href="{{ route('admin.archive') }}"
           aria-label="{{ __('admin.nav.archive') }}"
           title="{{ __('admin.nav.archive') }}"
           class="h-9 px-3.5 rounded-full text-xs font-semibold inline-flex items-center gap-1.5 transition-colors border {{ $isArchive ? 'bg-[#BA18F5] text-white border-[#BA18F5]' : 'bg-white dark:bg-[#1E1E20] text-[#64748B] dark:text-[#A1A1AA] border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/60 hover:text-[#0F172A] dark:hover:text-white' }}">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4"></path>
            </svg>
            <span class="hidden sm:inline">{{ __('admin.nav.archive') }}</span>
        </a>

        {{-- Admin Settings Shortcut --}}
        <a href="{{ route('admin.settings') }}"
           aria-label="{{ __('admin.nav.settings') }}"
           title="{{ __('admin.nav.settings') }}"
           class="p-2.5 rounded-full transition-colors {{ $isSettings ? 'bg-[#BA18F5]/15 text-[#BA18F5]' : 'hover:bg-gray-100 dark:hover:bg-zinc-800 text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
            </svg>
        </a>

        {{-- Theme switcher --}}
        <button type="button"
                onclick="toggleTheme()"
                aria-label="{{ __('admin.nav.toggle_theme') }}"
                class="p-2.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-[#0F172A] dark:text-white transition-colors cursor-pointer"
                title="{{ __('admin.nav.toggle_theme') }}">
            {{-- Sun Icon (shown in dark mode) --}}
            <svg class="w-5 h-5 hidden dark:block text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path>
            </svg>
            {{-- Moon Icon (shown in light mode) --}}
            <svg class="w-5 h-5 block dark:hidden text-black fill-black" viewBox="0 0 24 24">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
            </svg>
        </button>

        {{-- Logout button --}}
        <form method="POST" action="{{ route('admin.logout') }}" class="inline">
            @csrf
            <button type="submit"
                    aria-label="{{ __('admin.nav.sign_out') }}"
                    class="p-2.5 rounded-full hover:bg-rose-50 dark:hover:bg-rose-950/30 text-gray-500 hover:text-rose-600 dark:text-zinc-400 dark:hover:text-rose-400 transition-colors cursor-pointer"
                    title="{{ __('admin.nav.sign_out') }}">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                </svg>
            </button>
        </form>
    </div>
</header>
