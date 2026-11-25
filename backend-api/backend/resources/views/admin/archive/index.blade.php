@extends('admin.layouts.app')

@section('title', __('admin.archive.title'))
@section('page_title', __('admin.archive.title'))

@section('content')
<div class="space-y-8">
    {{-- Subtitle + Search & Tabs --}}
    <div class="space-y-4">
        <p class="text-sm text-[#64748B] dark:text-[#A1A1AA]">
            {{ __('admin.archive.subtitle') }}
        </p>

        <div class="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 w-full">
            @include('admin.components.toolbar.search-bar', [
                'action' => route('admin.archive'),
                'placeholder' => __('admin.archive.search'),
                'hidden' => ['tab' => $tab],
            ])

            <div class="flex flex-wrap items-center gap-2.5 flex-shrink-0">
                {{-- Tab Switcher: Exhibitions | Posts | Accounts --}}
                <div class="flex items-center gap-1 p-1 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] flex-shrink-0 h-12 select-none w-fit">
                    @foreach (['exhibitions' => __('admin.archive.tab_exhibitions'), 'posts' => __('admin.archive.tab_posts'), 'accounts' => __('admin.archive.tab_accounts')] as $key => $label)
                        <a href="{{ route('admin.archive', ['tab' => $key, 'per_page' => $perPage]) }}"
                           class="h-10 px-4 rounded-full text-sm font-semibold flex items-center gap-2 transition-all {{ $tab === $key ? 'bg-[#BA18F5] text-white shadow-md shadow-purple-500/25' : 'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
                            <span>{{ $label }}</span>
                            <span class="px-1.5 py-0.5 rounded-full text-[11px] tabular-nums {{ $tab === $key ? 'bg-white/20 text-white' : 'bg-black/5 dark:bg-white/10' }}">
                                {{ $counts[$key] ?? 0 }}
                            </span>
                        </a>
                    @endforeach
                </div>

                @include('admin.components.toolbar.per-page-dropdown')
            </div>
        </div>
    </div>

    {{-- Archived Items Table --}}
    <div data-live-region class="space-y-8">
    <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
        <div class="min-w-[820px]">
            <div class="grid grid-cols-[minmax(260px,1.8fr)_160px_170px_136px] items-center gap-4 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                <span>{{ $tab === 'accounts' ? __('admin.accounts.col_username') : __('admin.exhibitions.col_title') }}</span>
                <span>{{ $tab === 'accounts' ? __('admin.accounts.col_name') : __('admin.exhibitions.col_owner') }}</span>
                <span class="text-center">{{ $tab === 'accounts' ? __('admin.logs.col_status') : __('admin.common.col_deleted_at') }}</span>
                <span class="text-center">{{ __('admin.common.col_action') }}</span>
            </div>

            <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                @forelse ($items as $item)
                    @php
                        $itemId = match ($tab) {
                            'exhibitions' => $item->id_pameran,
                            'posts' => $item->id_karya,
                            'accounts' => $item->id_pengguna,
                        };
                        $primaryTitle = $tab === 'accounts' ? $item->nama_panggilan : $item->judul;
                        $secondaryText = match ($tab) {
                            'accounts' => $item->nama,
                            default => $item->pengguna->nama_panggilan ?? '—',
                        };
                        $thumbFallback = $tab === 'accounts'
                            ? \App\Support\AdminMedia::avatarFallback($item->nama)
                            : \App\Support\AdminMedia::bannerFallback();
                        $thumbUrl = match ($tab) {
                            'exhibitions' => \App\Support\AdminMedia::imageUrl($item->banner) ?? $thumbFallback,
                            'posts' => $item->file_url ?? $thumbFallback,
                            'accounts' => \App\Support\AdminMedia::imageUrl($item->foto_profil) ?? $thumbFallback,
                        };
                        $latestSanction = $tab === 'accounts' ? $item->sanksi->first() : null;
                    @endphp
                    <div class="px-6 py-4 grid grid-cols-[minmax(260px,1.8fr)_160px_170px_136px] items-center gap-4 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                        <div class="flex items-center gap-3.5 min-w-0">
                            <img src="{{ $thumbUrl }}"
                                 alt="{{ $primaryTitle }}"
                                 referrerpolicy="no-referrer"
                                 onerror="this.onerror=null;this.src='{{ $thumbFallback }}';"
                                 class="{{ $tab === 'accounts' ? 'w-10 h-10 rounded-full' : 'w-16 h-10 rounded-lg' }} object-cover border border-[#E5E7EB] dark:border-[#2D2D30] flex-shrink-0">
                            <div class="min-w-0">
                                <span class="block text-sm font-semibold text-[#0F172A] dark:text-white truncate">{{ $primaryTitle }}</span>
                                @if ($tab === 'accounts' && $latestSanction?->alasan)
                                    <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">{{ $latestSanction->alasan }}</span>
                                @endif
                            </div>
                        </div>

                        <span class="text-sm font-medium text-[#64748B] dark:text-[#A1A1AA] truncate">{{ $secondaryText }}</span>

                        @if ($tab === 'accounts')
                            <div class="text-center space-y-0.5">
                                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold capitalize {{ $item->status === 'nonaktifkan' ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300' : 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300' }}">
                                    {{ $item->status }}
                                </span>
                                @if ($latestSanction?->berakhir)
                                    <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] tabular-nums whitespace-nowrap">
                                        {{ \Illuminate\Support\Carbon::parse($latestSanction->berakhir)->format('d/m/y H:i') }}
                                    </span>
                                @endif
                            </div>
                        @else
                            <span class="text-xs font-medium text-[#94A3B8] dark:text-[#71717A] text-center tabular-nums whitespace-nowrap">
                                {{ $item->deleted_at ? $item->deleted_at->format('d/m/y H:i') : '—' }}
                            </span>
                        @endif

                        <div class="flex items-center justify-center">
                            <button type="button"
                                    onclick="openRestoreModal('{{ route('admin.archive.restore', ['type' => $tab, 'id' => $itemId]) }}', '{{ addslashes($primaryTitle) }}')"
                                    class="h-9 min-w-[114px] px-4 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25 font-semibold text-xs tracking-wide hover:bg-emerald-600 hover:text-white hover:border-emerald-600 transition-colors cursor-pointer inline-flex items-center justify-center gap-1.5">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"></path>
                                </svg>
                                <span>{{ __('admin.common.restore') }}</span>
                            </button>
                        </div>
                    </div>
                @empty
                    <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A] text-sm">
                        {{ __('admin.archive.empty') }}
                    </div>
                @endforelse
            </div>
        </div>
    </div>

    @if ($items instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $items->withQueryString()->links('admin.components.navigation.pagination') }}
    @endif
    </div>
</div>

{{-- Restore Confirmation Modal --}}
<div id="restoreModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="closeRestoreModal()"></div>
    <div class="relative w-full max-w-md bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-5">
            <h2 class="text-lg font-semibold text-[#0F172A] dark:text-white">{{ __('admin.archive.restore_modal_title') }}</h2>
            <button type="button" onclick="closeRestoreModal()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <form id="restoreForm" method="POST" action="" class="space-y-5">
            @csrf
            <p class="text-sm text-[#0F172A] dark:text-[#D4D4D8] leading-relaxed">
                {{ __('admin.archive.restore_modal_body') }}
                <strong id="restoreItemTitle" class="block mt-1.5 text-black dark:text-white"></strong>
            </p>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="closeRestoreModal()" class="h-10 px-5 rounded-full text-gray-600 dark:text-zinc-400 hover:bg-gray-100 dark:hover:bg-zinc-800 font-medium text-sm transition-colors cursor-pointer">
                    {{ __('admin.common.cancel') }}
                </button>
                <button type="submit" class="h-10 px-6 rounded-full bg-emerald-600 text-white font-semibold text-sm hover:bg-emerald-700 transition-colors cursor-pointer inline-flex items-center gap-2">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"></path>
                    </svg>
                    <span>{{ __('admin.common.restore') }}</span>
                </button>
            </div>
        </form>
    </div>
</div>

@push('scripts')
<script>
    function openRestoreModal(actionUrl, title) {
        const modal = document.getElementById('restoreModal');
        const form = document.getElementById('restoreForm');
        document.getElementById('restoreItemTitle').textContent = `"${title}"`;
        form.action = actionUrl;
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    function closeRestoreModal() {
        const modal = document.getElementById('restoreModal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
</script>
@endpush
@endsection
