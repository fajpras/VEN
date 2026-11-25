{{--
    Modern reusable filter dropdown using [data-dropdown] controller in resources/js/app.js.
    Props:
      - $param: query string key (e.g. 'status', 'sort', 'aksi')
      - $value: current active value (string|null)
      - $placeholder: label when empty (e.g. 'All Status')
      - $options: associative array ['val' => 'Label', ...]
      - $minWidth: optional width class (default 'min-w-[160px]')
--}}
@php
    $query = request()->query();
    unset($query['page']);
    $activeValue = (string) ($value ?? '');
    $activeLabel = $activeValue !== '' && isset($options[$activeValue]) ? $options[$activeValue] : $placeholder;
    $minWidth = $minWidth ?? 'min-w-[155px]';

    $buildFilterUrl = function (?string $newVal) use ($query, $param) {
        $next = $query;
        if ($newVal === null || $newVal === '') {
            unset($next[$param]);
        } else {
            $next[$param] = $newVal;
        }
        return request()->url() . (empty($next) ? '' : '?' . http_build_query($next));
    };
@endphp

<div class="relative flex-shrink-0" data-dropdown>
    <button type="button"
            data-dropdown-trigger
            aria-expanded="false"
            aria-haspopup="listbox"
            class="h-12 {{ $minWidth }} pl-4 pr-3.5 rounded-full bg-white dark:bg-[#1E1E20] text-sm font-medium flex items-center justify-between gap-2.5 border {{ $activeValue !== '' ? 'border-[#BA18F5]/60 text-[#BA18F5] dark:text-[#D55BFF]' : 'border-[#E5E7EB] dark:border-[#2D2D30] text-[#0F172A] dark:text-white' }} hover:border-[#BA18F5]/60 focus:outline-none focus:ring-2 focus:ring-[#BA18F5]/30 shadow-2xs transition-all cursor-pointer select-none">
        <span class="truncate">{{ $activeLabel }}</span>
        <svg data-dropdown-chevron class="w-4 h-4 text-[#94A3B8] dark:text-[#71717A] transition-transform duration-200 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
        </svg>
    </button>

    <div data-dropdown-menu
         role="listbox"
         class="hidden absolute right-0 top-full mt-2 min-w-[190px] max-h-72 overflow-y-auto bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] rounded-2xl shadow-xl shadow-black/10 dark:shadow-black/40 p-1.5 z-30">
        <a href="{{ $buildFilterUrl(null) }}"
           role="option"
           aria-selected="{{ $activeValue === '' ? 'true' : 'false' }}"
           class="w-full px-3.5 py-2 rounded-xl text-sm flex items-center justify-between gap-2 transition-colors {{ $activeValue === '' ? 'bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] font-semibold' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
            <span class="truncate">{{ $placeholder }}</span>
            @if ($activeValue === '')
                <svg class="w-4 h-4 flex-shrink-0 text-[#BA18F5] dark:text-[#D55BFF]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                </svg>
            @endif
        </a>

        @foreach ($options as $optKey => $optLabel)
            @php $isSelected = $activeValue === (string) $optKey; @endphp
            <a href="{{ $buildFilterUrl((string) $optKey) }}"
               role="option"
               aria-selected="{{ $isSelected ? 'true' : 'false' }}"
               class="w-full px-3.5 py-2 rounded-xl text-sm flex items-center justify-between gap-2 transition-colors {{ $isSelected ? 'bg-[#BA18F5]/10 text-[#BA18F5] dark:text-[#D55BFF] font-semibold' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
                <span class="truncate">{{ $optLabel }}</span>
                @if ($isSelected)
                    <svg class="w-4 h-4 flex-shrink-0 text-[#BA18F5] dark:text-[#D55BFF]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                    </svg>
                @endif
            </a>
        @endforeach
    </div>
</div>
