{{--
    Komponen Toggle Mode Tampilan (Card | List) reusable.
    Props:
      - $viewMode: 'card' | 'list'
--}}
@php
    $query = request()->query();
    unset($query['page']);

    $buildUrl = function (array $merge) use ($query) {
        return request()->url() . '?' . http_build_query(array_merge($query, $merge));
    };
@endphp

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
