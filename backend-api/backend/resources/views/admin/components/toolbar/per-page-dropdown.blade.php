{{--
    Komponen Per-Page Dropdown pill reusable.
    Props:
      - $perPage: jumlah item per halaman aktif
      - $perPageOptions: array opsi jumlah per halaman
      - $height: kelas tinggi opsional (default 'h-12' agar sejajar satu baris dengan search/filter)
--}}
@php
    $height = $height ?? 'h-12';
    $query = request()->query();
    unset($query['page']);

    $buildUrl = function (array $merge) use ($query) {
        return request()->url() . '?' . http_build_query(array_merge($query, $merge));
    };
@endphp

<div class="relative flex-shrink-0" data-dropdown>
    <button type="button"
            data-dropdown-trigger
            aria-expanded="false"
            aria-haspopup="listbox"
            aria-label="Items per page"
            class="{{ $height }} pl-4 pr-3.5 rounded-full bg-white dark:bg-[#1E1E20] text-[#0F172A] dark:text-white text-sm font-medium flex items-center justify-between gap-2 border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/60 focus:outline-none focus:ring-2 focus:ring-[#BA18F5]/40 focus:border-[#BA18F5] shadow-2xs transition-all cursor-pointer select-none whitespace-nowrap">
        <span class="text-xs font-medium text-[#64748B] dark:text-[#A1A1AA]">{{ __('admin.common.show') }}</span>
        <span class="font-semibold tabular-nums">{{ (int) $perPage }}</span>
        <svg data-dropdown-chevron class="w-4 h-4 text-[#94A3B8] dark:text-[#71717A] transition-transform duration-200 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
        </svg>
    </button>

    <div data-dropdown-menu
         role="listbox"
         class="hidden absolute right-0 top-full mt-2 w-40 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] rounded-2xl shadow-xl shadow-black/10 dark:shadow-black/40 p-1.5 z-30">
        @foreach ($perPageOptions as $option)
            @php $isSelected = (int) $perPage === (int) $option; @endphp
            <a href="{{ $buildUrl(['per_page' => $option]) }}"
               role="option"
               aria-selected="{{ $isSelected ? 'true' : 'false' }}"
               class="w-full px-3.5 py-2 rounded-xl text-sm flex items-center justify-between transition-colors {{ $isSelected ? 'bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] font-semibold' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
                <span class="tabular-nums">{{ $option }} <span class="text-xs font-normal text-[#94A3B8] dark:text-[#71717A]">/ {{ __('admin.common.per_page') }}</span></span>
                @if ($isSelected)
                    <svg class="w-4 h-4 text-[#BA18F5] dark:text-[#D55BFF] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                    </svg>
                @endif
            </a>
        @endforeach
    </div>
</div>
