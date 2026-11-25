@extends('admin.layouts.app')

@section('title', __('admin.nav.exhibitions'))
@section('page_title', __('admin.nav.exhibitions'))

@section('content')
<div class="space-y-8">
    {{-- Search Bar & Dashboard Filters --}}
    <div class="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 w-full">
        <form method="GET" action="{{ route('admin.exhibitions') }}" data-live-search class="flex-1 min-w-0">
            @if ($tipe) <input type="hidden" name="tipe" value="{{ $tipe }}"> @endif
            @if ($sort && $sort !== 'newest') <input type="hidden" name="sort" value="{{ $sort }}"> @endif
            <div class="relative flex items-center w-full">
                <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
                <input type="text"
                       name="cari"
                       value="{{ request('cari') }}"
                       placeholder="{{ __('admin.common.search') }}"
                       class="w-full h-12 pl-13 pr-6 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
            </div>
        </form>

        <div class="flex flex-wrap items-center gap-2.5">
            @include('admin.layouts.filter-dropdown', [
                'param' => 'tipe',
                'value' => $tipe,
                'placeholder' => __('admin.exhibitions.all_sizes'),
                'options' => [
                    'kecil' => __('admin.exhibitions.size_kecil'),
                    'sedang' => __('admin.exhibitions.size_sedang'),
                    'besar' => __('admin.exhibitions.size_besar'),
                ],
            ])
            @include('admin.layouts.filter-dropdown', [
                'param' => 'sort',
                'value' => $sort === 'newest' ? '' : $sort,
                'placeholder' => __('admin.common.sort_newest'),
                'options' => [
                    'oldest' => __('admin.common.sort_oldest'),
                ],
            ])
        </div>
    </div>

    @include('admin.layouts.list-controls')

    <div data-live-region class="space-y-8">
    @if ($viewMode === 'list')
        {{-- Exhibition List Rows --}}
        <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
            <div class="min-w-[840px]">
                {{-- Header kolom konsisten & rata sempurna --}}
                <div class="grid grid-cols-[minmax(300px,1.8fr)_140px_90px_110px_124px] items-center gap-4 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                    <span>{{ __('admin.exhibitions.col_title') }}</span>
                    <span>{{ __('admin.exhibitions.col_owner') }}</span>
                    <span class="text-center">{{ __('admin.exhibitions.col_post') }}</span>
                    <span class="text-center">{{ __('admin.exhibitions.col_created') }}</span>
                    <span class="text-center">{{ __('admin.common.col_action') }}</span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                    @forelse ($pameran as $exhibit)
                        @php
                            $bannerFallback = \App\Support\AdminMedia::bannerFallback();
                            $bannerUrl = \App\Support\AdminMedia::imageUrl($exhibit->banner)
                                ?? \App\Support\AdminMedia::imageUrl($exhibit->banner_url ?? null)
                                ?? $bannerFallback;
                        @endphp
                        <div class="px-6 py-4 grid grid-cols-[minmax(300px,1.8fr)_140px_90px_110px_124px] items-center gap-4 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                            {{-- Landscape Banner Thumbnail & Title --}}
                            <div class="flex items-center gap-4 min-w-0">
                                <div class="w-28 h-[60px] flex-shrink-0 rounded-xl overflow-hidden bg-gray-100 dark:bg-zinc-800 border border-[#E5E7EB]/60 dark:border-[#2D2D30]">
                                    <img src="{{ $bannerUrl }}"
                                         alt="{{ $exhibit->judul }}"
                                         referrerpolicy="no-referrer"
                                         onerror="this.onerror=null;this.src='{{ $bannerFallback }}';"
                                         loading="lazy"
                                         decoding="async"
                                         class="w-full h-full object-cover hover:scale-105 transition-transform duration-300">
                                </div>
                                <span class="min-w-0 text-sm font-semibold text-[#0F172A] dark:text-white truncate">{{ $exhibit->judul }}</span>
                            </div>

                            {{-- Owner --}}
                            <span class="min-w-0 text-sm font-medium text-[#0F172A] dark:text-white truncate">{{ $exhibit->pengguna->nama_panggilan ?? '—' }}</span>

                            {{-- Post --}}
                            <span class="text-sm font-medium text-[#0F172A] dark:text-white text-center tabular-nums">{{ $exhibit->karya_count ?? $exhibit->ruangan()->count() }}</span>

                            {{-- Created At --}}
                            <span class="text-sm font-medium text-[#64748B] dark:text-[#A1A1AA] text-center tabular-nums whitespace-nowrap">{{ $exhibit->created_at ? $exhibit->created_at->format('d/m/y') : '—' }}</span>

                            {{-- Action: Delete Button --}}
                            <div class="flex items-center justify-center">
                                <button type="button"
                                        onclick="openDeleteExhibitionModal('{{ $exhibit->id_pameran }}', '{{ addslashes($exhibit->judul) }}')"
                                        class="h-9 min-w-[104px] px-4 rounded-full bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 font-semibold text-xs tracking-wide hover:bg-rose-600 hover:text-white hover:border-rose-600 transition-colors cursor-pointer inline-flex items-center justify-center gap-1.5">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                    </svg>
                                    <span>{{ __('admin.common.delete') }}</span>
                                </button>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A] text-sm">
                            {{ __('admin.exhibitions.empty') }}
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        {{-- Exhibition Card Grid --}}
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            @forelse ($pameran as $exhibit)
                @php
                    $bannerFallback = \App\Support\AdminMedia::bannerFallback();
                    $bannerUrl = \App\Support\AdminMedia::imageUrl($exhibit->banner)
                        ?? \App\Support\AdminMedia::imageUrl($exhibit->banner_url ?? null)
                        ?? $bannerFallback;
                @endphp
                <div class="flex flex-col rounded-2xl overflow-hidden bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                    <div class="h-36 w-full overflow-hidden bg-gray-100 dark:bg-zinc-800">
                        <img src="{{ $bannerUrl }}"
                             alt="{{ $exhibit->judul }}"
                             referrerpolicy="no-referrer"
                             onerror="this.onerror=null;this.src='{{ $bannerFallback }}';"
                             loading="lazy"
                             decoding="async"
                             class="w-full h-full object-cover hover:scale-105 transition-transform duration-300">
                    </div>
                    <div class="flex flex-col flex-1 p-5 gap-3.5">
                        <h3 class="text-base font-semibold text-[#0F172A] dark:text-white leading-snug line-clamp-2">{{ $exhibit->judul }}</h3>
                        <div class="grid grid-cols-3 gap-3 text-xs">
                            <div class="min-w-0">
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">owner</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $exhibit->pengguna->nama_panggilan ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">post</span>
                                <span class="font-medium text-[#0F172A] dark:text-white tabular-nums">{{ $exhibit->karya_count ?? $exhibit->ruangan()->count() }}</span>
                            </div>
                            <div>
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">created</span>
                                <span class="font-medium text-[#0F172A] dark:text-white tabular-nums whitespace-nowrap">{{ $exhibit->created_at ? $exhibit->created_at->format('d/m/y') : '—' }}</span>
                            </div>
                        </div>
                        <div class="pt-2 mt-auto">
                            <button type="button"
                                    onclick="openDeleteExhibitionModal('{{ $exhibit->id_pameran }}', '{{ addslashes($exhibit->judul) }}')"
                                    class="h-9 w-full rounded-full bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 font-semibold text-xs tracking-wide hover:bg-rose-600 hover:text-white hover:border-rose-600 transition-colors cursor-pointer inline-flex items-center justify-center gap-1.5">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                </svg>
                                <span>{{ __('admin.common.delete') }}</span>
                            </button>
                        </div>
                    </div>
                </div>
            @empty
                <div class="col-span-full py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                    No virtual exhibitions found.
                </div>
            @endforelse
        </div>
    @endif

    @if ($pameran instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $pameran->withQueryString()->links('admin.layouts.pagination') }}
    @endif
    </div>
</div>

{{-- Delete Confirmation Modal (GitHub-style type "delete") --}}
<div id="deleteExhibitionModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="closeDeleteExhibitionModal()"></div>
    <div class="relative w-full max-w-md bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-5">
            <h2 class="text-lg font-semibold text-[#0F172A] dark:text-white">Delete Exhibition</h2>
            <button type="button" onclick="closeDeleteExhibitionModal()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <form id="deleteExhibitionForm" method="POST" action="" class="space-y-4">
            @csrf
            @method('DELETE')
            <p class="text-sm text-[#0F172A] dark:text-[#D4D4D8] leading-relaxed">
                Are you sure you want to delete exhibition <strong id="deleteExhibitTitle" class="text-black dark:text-white"></strong>? {{ __('admin.archive.delete_note') }}
            </p>

            <div>
                <label for="deleteExhibitConfirmInput" class="block text-xs font-semibold text-[#64748B] dark:text-[#A1A1AA] mb-1.5">
                    {{ __('admin.archive.type_delete_label') }}
                </label>
                <input id="deleteExhibitConfirmInput"
                       type="text"
                       autocomplete="off"
                       placeholder="delete"
                       oninput="syncDeleteExhibitButton()"
                       class="w-full h-11 px-4 rounded-xl bg-[#F3F4F6] dark:bg-[#27272A] text-[#0F172A] dark:text-white text-sm font-mono outline-none border border-transparent focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20 transition-all">
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="closeDeleteExhibitionModal()" class="h-10 px-5 rounded-full text-gray-600 dark:text-zinc-400 hover:bg-gray-100 dark:hover:bg-zinc-800 font-medium text-sm transition-colors cursor-pointer">
                    {{ __('admin.common.cancel') }}
                </button>
                <button id="deleteExhibitSubmitBtn" type="submit" disabled class="h-10 px-6 rounded-full bg-rose-600 disabled:opacity-40 disabled:cursor-not-allowed text-white font-semibold text-sm hover:bg-rose-700 transition-all cursor-pointer inline-flex items-center gap-2">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                    </svg>
                    <span>{{ __('admin.common.delete') }}</span>
                </button>
            </div>
        </form>
    </div>
</div>

@push('scripts')
<script>
    function syncDeleteExhibitButton() {
        const input = document.getElementById('deleteExhibitConfirmInput');
        const btn = document.getElementById('deleteExhibitSubmitBtn');
        if (input && btn) {
            btn.disabled = input.value.trim().toLowerCase() !== 'delete';
        }
    }

    function openDeleteExhibitionModal(id, title) {
        const modal = document.getElementById('deleteExhibitionModal');
        const form = document.getElementById('deleteExhibitionForm');
        const input = document.getElementById('deleteExhibitConfirmInput');
        document.getElementById('deleteExhibitTitle').textContent = `"${title}"`;
        form.action = `/admin/exhibitions/${id}`;
        if (input) input.value = '';
        syncDeleteExhibitButton();
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        setTimeout(() => input?.focus(), 50);
    }

    function closeDeleteExhibitionModal() {
        const modal = document.getElementById('deleteExhibitionModal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
</script>
@endpush
@endsection
