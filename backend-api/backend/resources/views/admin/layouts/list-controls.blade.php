{{--
    Kontrol tampilan daftar: toggle Card/List + dropdown modern jumlah item per halaman.
    Semua query string yang aktif (cari, kategori, dsb.) dipertahankan;
    parameter "page" dibuang agar kembali ke halaman pertama saat opsi diubah.

    Variables yang dibutuhkan: $viewMode, $perPage, $perPageOptions
--}}
@php
    $showViewToggle = $showViewToggle ?? true;
    $query = request()->query();
    unset($query['page']);

    $buildUrl = function (array $merge) use ($query) {
        return request()->url() . '?' . http_build_query(array_merge($query, $merge));
    };
@endphp

<div class="flex flex-col sm:flex-row sm:items-center {{ $showViewToggle ? 'sm:justify-between' : 'sm:justify-end' }} gap-4">
    @if ($showViewToggle)
        {{-- Toggle Card / List --}}
        <div class="inline-flex items-center gap-1 p-1 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] w-fit" role="group" aria-label="Mode tampilan">
            <a href="{{ $buildUrl(['view' => 'card']) }}"
               @class([
                   'h-9 px-5 rounded-full flex items-center gap-2 text-sm font-medium transition-colors',
                   'bg-white dark:bg-zinc-700 text-[#BA18F5] shadow-xs' => $viewMode === 'card',
                   'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' => $viewMode !== 'card',
               ])
               aria-pressed="{{ $viewMode === 'card' ? 'true' : 'false' }}">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1H5a1 1 0 01-1-1V5zm9 0a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1h-5a1 1 0 01-1-1V5zM4 14a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1H5a1 1 0 01-1-1v-5zm9 0a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1h-5a1 1 0 01-1-1v-5z" />
                </svg>
                Card
            </a>
            <a href="{{ $buildUrl(['view' => 'list']) }}"
               @class([
                   'h-9 px-5 rounded-full flex items-center gap-2 text-sm font-medium transition-colors',
                   'bg-white dark:bg-zinc-700 text-[#BA18F5] shadow-xs' => $viewMode === 'list',
                   'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' => $viewMode !== 'list',
               ])
               aria-pressed="{{ $viewMode === 'list' ? 'true' : 'false' }}">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
                List
            </a>
        </div>
    @endif

    {{-- Modern Custom Dropdown: Jumlah item per halaman --}}
    <div class="flex items-center gap-2.5 text-sm text-[#64748B] dark:text-[#A1A1AA]">
        <span class="font-medium">{{ __('admin.common.show') }}</span>
        <div class="relative" data-dropdown>
            <button type="button"
                    data-dropdown-trigger
                    aria-expanded="false"
                    aria-haspopup="listbox"
                    aria-label="Items per page"
                    class="h-10 min-w-[78px] pl-4 pr-3.5 rounded-full bg-white dark:bg-[#1E1E20] text-[#0F172A] dark:text-white text-sm font-semibold flex items-center justify-between gap-2.5 border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/60 focus:outline-none focus:ring-2 focus:ring-[#BA18F5]/40 focus:border-[#BA18F5] shadow-2xs transition-all cursor-pointer select-none">
                <span class="tabular-nums">{{ (int) $perPage }}</span>
                <svg data-dropdown-chevron class="w-4 h-4 text-[#94A3B8] dark:text-[#71717A] transition-transform duration-200 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                </svg>
            </button>

            <div data-dropdown-menu
                 role="listbox"
                 class="hidden absolute right-0 top-full mt-2 w-32 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] rounded-2xl shadow-xl shadow-black/10 dark:shadow-black/40 p-1.5 z-30">
                @foreach ($perPageOptions as $option)
                    @php $isSelected = (int) $perPage === (int) $option; @endphp
                    <a href="{{ $buildUrl(['per_page' => $option]) }}"
                       role="option"
                       aria-selected="{{ $isSelected ? 'true' : 'false' }}"
                       class="w-full px-3.5 py-2 rounded-xl text-sm flex items-center justify-between transition-colors {{ $isSelected ? 'bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] font-semibold' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
                        <span class="tabular-nums">{{ $option }}</span>
                        @if ($isSelected)
                            <svg class="w-4 h-4 text-[#BA18F5] dark:text-[#D55BFF]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                            </svg>
                        @endif
                    </a>
                @endforeach
            </div>
        </div>
        <span class="font-medium">{{ __('admin.common.per_page') }}</span>
    </div>
</div>
