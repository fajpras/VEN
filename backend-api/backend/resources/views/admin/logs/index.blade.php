@extends('admin.layouts.app')

@section('title', $tab === 'keamanan' ? __('admin.nav.security_logs') : __('admin.nav.logs'))
@section('page_title', $tab === 'keamanan' ? __('admin.nav.security_logs') : __('admin.nav.logs'))

@section('content')
<div class="space-y-8">
    {{-- Search Bar & Dashboard Filters (sejajar, konsisten dengan halaman lain) --}}
    <div class="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 w-full">
        <form method="GET" action="{{ route('admin.logs') }}" data-live-search class="flex-1 min-w-0">
            <input type="hidden" name="tab" value="{{ $tab }}">
            @if ($aksi) <input type="hidden" name="aksi" value="{{ $aksi }}"> @endif
            @if ($status) <input type="hidden" name="status" value="{{ $status }}"> @endif
            @if ($method) <input type="hidden" name="method" value="{{ $method }}"> @endif
            @if ($statusGroup) <input type="hidden" name="status_group" value="{{ $statusGroup }}"> @endif
            <div class="relative flex items-center w-full">
                <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
                <input type="text"
                       name="cari"
                       value="{{ request('cari') }}"
                       placeholder="{{ $tab === 'keamanan' ? __('admin.logs.search_security') : __('admin.logs.search_activity') }}"
                       class="w-full h-12 pl-13 pr-6 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
            </div>
        </form>

        <div class="flex flex-wrap items-center gap-2.5">
            @if ($tab === 'keamanan')
                @include('admin.layouts.filter-dropdown', [
                    'param' => 'status_group',
                    'value' => $statusGroup,
                    'placeholder' => __('admin.logs.all_status'),
                    'options' => [
                        '2xx' => __('admin.logs.status_2xx'),
                        '4xx' => __('admin.logs.status_4xx'),
                        '5xx' => __('admin.logs.status_5xx'),
                    ],
                ])
                @include('admin.layouts.filter-dropdown', [
                    'param' => 'method',
                    'value' => $method,
                    'placeholder' => __('admin.logs.all_methods'),
                    'options' => [
                        'GET' => 'GET',
                        'POST' => 'POST',
                        'PUT' => 'PUT',
                        'PATCH' => 'PATCH',
                        'DELETE' => 'DELETE',
                    ],
                    'minWidth' => 'min-w-[140px]',
                ])
            @else
                @include('admin.layouts.filter-dropdown', [
                    'param' => 'aksi',
                    'value' => $aksi,
                    'placeholder' => __('admin.logs.all_actions'),
                    'options' => [
                        'tambah' => 'Tambah',
                        'ubah' => 'Ubah',
                        'hapus' => 'Hapus',
                        'registrasi' => 'Registrasi',
                        'pulih' => 'Pulih',
                        'lihat' => 'Lihat',
                    ],
                ])
                @include('admin.layouts.filter-dropdown', [
                    'param' => 'status',
                    'value' => $status,
                    'placeholder' => __('admin.logs.all_status'),
                    'options' => [
                        'berhasil' => 'Berhasil',
                        'gagal' => 'Gagal',
                    ],
                    'minWidth' => 'min-w-[140px]',
                ])
            @endif
        </div>
    </div>

    @include('admin.layouts.list-controls', ['showViewToggle' => false])

    <div data-live-region class="space-y-8">
    @if ($tab === 'keamanan')
        {{-- ==================== TAB LOG KEAMANAN (PESAN SEBAGAI ICON TOOLTIP COMPACT) ==================== --}}
        <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
            <div class="min-w-[960px]">
                <div class="grid grid-cols-[160px_80px_minmax(180px,1.2fr)_72px_76px_145px_175px_104px] items-center gap-3.5 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                    <span>{{ __('admin.logs.col_event') }}</span>
                    <span class="text-center">{{ __('admin.logs.col_method') }}</span>
                    <span>{{ __('admin.logs.col_endpoint') }}</span>
                    <span class="text-center">{{ __('admin.logs.col_message') }}</span>
                    <span class="text-center">{{ __('admin.logs.col_status') }}</span>
                    <span>{{ __('admin.logs.col_actor') }}</span>
                    <span>{{ __('admin.logs.col_client') }}</span>
                    <span class="text-right">{{ __('admin.logs.col_time') }}</span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                    @forelse ($logs as $log)
                        @php
                            $statusClass = match (true) {
                                $log->status_kode >= 200 && $log->status_kode < 300 => 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800/50',
                                $log->status_kode >= 400 && $log->status_kode < 500 => 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300 border-amber-200 dark:border-amber-800/50',
                                default => 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 border-rose-200 dark:border-rose-800/50',
                            };
                            $methodClass = match (strtoupper((string) $log->method)) {
                                'GET' => 'bg-sky-50 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300 border-sky-200 dark:border-sky-800/40',
                                'POST' => 'bg-purple-50 text-[#BA18F5] dark:bg-purple-950/40 dark:text-purple-300 border-purple-200 dark:border-purple-800/40',
                                'PUT', 'PATCH' => 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300 border-amber-200 dark:border-amber-800/40',
                                'DELETE' => 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 border-rose-200 dark:border-rose-800/40',
                                default => 'bg-gray-100 text-gray-600 dark:bg-zinc-800 dark:text-zinc-300 border-gray-200 dark:border-zinc-700',
                            };
                            $clientLabel = \App\Support\AdminMedia::parseUserAgent($log->user_agent);
                            $actorName = $log->pengguna->nama ?? 'Guest / Anonim';
                            $actorHandle = $log->pengguna->nama_panggilan ?? null;
                            $hasMessage = filled($log->pesan);
                        @endphp
                        <div class="px-6 py-4 grid grid-cols-[160px_80px_minmax(180px,1.2fr)_72px_76px_145px_175px_104px] items-center gap-3.5 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                            {{-- Event --}}
                            <div class="min-w-0">
                                <span class="inline-flex max-w-full items-center px-2.5 py-1 rounded-full bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] font-mono text-[11px] font-semibold truncate" title="{{ $log->event }}">
                                    {{ $log->event }}
                                </span>
                            </div>

                            {{-- Method --}}
                            <div class="flex justify-center">
                                <span class="inline-flex px-2.5 py-0.5 rounded-md font-mono text-[10px] font-bold border {{ $methodClass }}">
                                    {{ strtoupper((string) ($log->method ?: 'REQ')) }}
                                </span>
                            </div>

                            {{-- Endpoint --}}
                            <div class="min-w-0">
                                <span class="block font-mono text-xs font-medium text-[#0F172A] dark:text-white truncate" title="{{ $log->endpoint }}">
                                    {{ $log->endpoint ?: '—' }}
                                </span>
                            </div>

                            {{-- Pesan (Compact Icon + Hover Tooltip) --}}
                            <div class="flex justify-center">
                                @if ($hasMessage)
                                    <div class="group relative inline-flex">
                                        <button type="button"
                                                aria-label="{{ $log->pesan }}"
                                                class="w-8 h-8 rounded-full bg-[#F1F5F9] dark:bg-zinc-800 text-[#64748B] dark:text-[#A1A1AA] hover:bg-[#BA18F5]/15 hover:text-[#BA18F5] dark:hover:text-[#D55BFF] flex items-center justify-center transition-colors cursor-pointer">
                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path>
                                            </svg>
                                        </button>
                                        <div role="tooltip"
                                             class="pointer-events-none opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 translate-y-1 group-hover:translate-y-0 group-focus-within:translate-y-0 transition-all duration-150 absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-64 p-3 rounded-xl bg-[#0F172A] dark:bg-zinc-800 text-white text-xs leading-relaxed shadow-xl border border-white/10 z-40">
                                            {{ $log->pesan }}
                                        </div>
                                    </div>
                                @else
                                    <span class="text-xs text-[#94A3B8] dark:text-[#71717A]">—</span>
                                @endif
                            </div>

                            {{-- HTTP Status --}}
                            <div class="flex justify-center">
                                <span class="inline-flex items-center px-2.5 py-0.5 text-xs font-semibold rounded-full border tabular-nums {{ $statusClass }}">
                                    {{ $log->status_kode }}
                                </span>
                            </div>

                            {{-- Actor --}}
                            <div class="min-w-0">
                                <p class="text-xs font-semibold text-[#0F172A] dark:text-white truncate">
                                    {{ $actorName }}
                                </p>
                                @if ($actorHandle)
                                    <p class="text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">
                                        {{ '@' . $actorHandle }}
                                    </p>
                                @endif
                            </div>

                            {{-- Client / User Agent --}}
                            <div class="min-w-0 space-y-0.5">
                                <p class="text-xs font-medium text-[#0F172A] dark:text-white truncate" title="{{ $log->user_agent ?: 'Tidak tercatat' }}">
                                    {{ $clientLabel }}
                                </p>
                                <p class="font-mono text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">
                                    {{ $log->ip_address ?: '—' }}
                                </p>
                            </div>

                            {{-- Timestamp --}}
                            <div class="text-right">
                                <span class="block text-xs font-medium text-[#0F172A] dark:text-white tabular-nums whitespace-nowrap">
                                    {{ $log->waktu ? $log->waktu->format('d/m/y') : ($log->created_at ? $log->created_at->format('d/m/y') : '—') }}
                                </span>
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] tabular-nums whitespace-nowrap">
                                    {{ $log->waktu ? $log->waktu->format('H:i:s') : ($log->created_at ? $log->created_at->format('H:i:s') : '') }}
                                </span>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A] text-sm">
                            {{ __('admin.logs.empty_security') }}
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        {{-- ==================== TAB LOG AKTIVITAS (LIST ONLY) ==================== --}}
        <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
            <div class="min-w-[880px]">
                <div class="grid grid-cols-[minmax(280px,1.8fr)_120px_minmax(150px,0.9fr)_110px_150px] items-center gap-4 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                    <span>{{ __('admin.logs.col_activity') }}</span>
                    <span class="text-center">{{ __('admin.logs.col_action') }}</span>
                    <span>{{ __('admin.logs.col_actor') }}</span>
                    <span class="text-center">{{ __('admin.logs.col_status') }}</span>
                    <span class="text-right">{{ __('admin.logs.col_ip_time') }}</span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                    @forelse ($logs as $log)
                        @php
                            $aksiVal = strtolower((string) ($log->aksi ?? 'aktivitas'));
                            $aksiClass = match ($aksiVal) {
                                'tambah' => 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800/50',
                                'ubah' => 'bg-sky-50 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300 border-sky-200 dark:border-sky-800/50',
                                'hapus' => 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 border-rose-200 dark:border-rose-800/50',
                                'registrasi' => 'bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] border-[#BA18F5]/25',
                                'pulih' => 'bg-teal-50 text-teal-700 dark:bg-teal-950/40 dark:text-teal-300 border-teal-200 dark:border-teal-800/50',
                                default => 'bg-gray-100 text-gray-700 dark:bg-zinc-800 dark:text-zinc-300 border-gray-200 dark:border-zinc-700',
                            };
                            $isSuccess = strtolower((string) ($log->status ?? 'berhasil')) === 'berhasil';
                            $actorName = $log->pengguna->nama ?? ($log->entitas ?: 'Sistem');
                            $actorHandle = $log->pengguna->nama_panggilan ?? null;
                        @endphp
                        <div class="px-6 py-4 grid grid-cols-[minmax(280px,1.8fr)_120px_minmax(150px,0.9fr)_110px_150px] items-start gap-4 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                            {{-- Activity Message & Category --}}
                            <div class="min-w-0 space-y-1">
                                <p class="text-sm font-medium text-[#0F172A] dark:text-white leading-snug">
                                    {{ $log->pesan ?? $log->deskripsi ?? 'Aktivitas sistem tercatat' }}
                                </p>
                                <div class="flex flex-wrap items-center gap-2 text-[11px] text-[#94A3B8] dark:text-[#71717A]">
                                    @if ($log->jenis)
                                        <span class="inline-flex items-center px-2 py-0.5 rounded-md bg-[#F1F5F9] dark:bg-zinc-800 text-[#64748B] dark:text-[#A1A1AA] font-medium capitalize">
                                            {{ $log->jenis }}
                                        </span>
                                    @endif
                                    @if ($log->entitas && $log->entitas !== $actorName)
                                        <span class="truncate">{{ $log->entitas }}</span>
                                    @endif
                                </div>
                            </div>

                            {{-- Action Pill --}}
                            <div class="pt-0.5 flex justify-center">
                                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold capitalize border {{ $aksiClass }}">
                                    {{ $log->aksi ?? 'sistem' }}
                                </span>
                            </div>

                            {{-- Actor --}}
                            <div class="min-w-0">
                                <p class="text-xs font-semibold text-[#0F172A] dark:text-white truncate">
                                    {{ $actorName }}
                                </p>
                                @if ($actorHandle)
                                    <p class="text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">
                                        {{ '@' . $actorHandle }}
                                    </p>
                                @endif
                            </div>

                            {{-- Status --}}
                            <div class="pt-0.5 flex justify-center">
                                <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold capitalize {{ $isSuccess ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300' : 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300' }}">
                                    <span class="w-1.5 h-1.5 rounded-full {{ $isSuccess ? 'bg-emerald-500' : 'bg-rose-500' }}"></span>
                                    {{ $log->status ?? 'berhasil' }}
                                </span>
                            </div>

                            {{-- IP & Timestamp --}}
                            <div class="text-right min-w-0 space-y-0.5">
                                <p class="text-xs font-medium text-[#0F172A] dark:text-white tabular-nums whitespace-nowrap">
                                    {{ $log->waktu ? $log->waktu->format('d/m/y H:i') : ($log->created_at ? $log->created_at->format('d/m/y H:i') : '—') }}
                                </p>
                                <p class="font-mono text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">
                                    {{ $log->ip_address ?: '—' }}
                                </p>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A] text-sm">
                            {{ __('admin.logs.empty_activity') }}
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @endif

    @if ($logs instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $logs->withQueryString()->links('admin.layouts.pagination') }}
    @endif
    </div>
</div>
@endsection
