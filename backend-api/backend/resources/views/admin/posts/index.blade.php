@extends('admin.layouts.app')

@section('title', __('admin.nav.posts'))
@section('page_title', __('admin.nav.posts'))

@section('content')
<div class="space-y-8">
    {{-- Search & Filter Header --}}
    <div class="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 w-full">
        @include('admin.components.toolbar.search-bar', [
            'id' => 'postFilterForm',
            'action' => route('admin.posts'),
            'hidden' => [
                'kategori' => request('kategori'),
                'jenis' => $jenis,
                'sort' => $sort !== 'newest' ? $sort : null,
            ],
        ])

        <div class="flex flex-wrap items-center gap-2.5">
            @php $categoryOptions = array_combine($kategoriList, $kategoriList); @endphp
            @include('admin.components.toolbar.filter-dropdown', [
                'param' => 'kategori',
                'value' => request('kategori'),
                'placeholder' => __('admin.posts.all_categories'),
                'options' => $categoryOptions,
                'minWidth' => 'min-w-[180px]',
            ])
            @include('admin.components.toolbar.filter-dropdown', [
                'param' => 'jenis',
                'value' => $jenis,
                'placeholder' => __('admin.posts.all_types'),
                'options' => [
                    'poster' => __('admin.posts.type_poster'),
                    'video' => __('admin.posts.type_video'),
                ],
            ])
            @include('admin.components.toolbar.filter-dropdown', [
                'param' => 'sort',
                'value' => $sort === 'newest' ? '' : $sort,
                'placeholder' => __('admin.common.sort_newest'),
                'options' => [
                    'oldest' => __('admin.common.sort_oldest'),
                    'title' => __('admin.posts.sort_title'),
                ],
            ])
        </div>
    </div>

    @include('admin.components.toolbar.list-controls')

    <div data-live-region class="space-y-8">
    @if ($viewMode === 'list')
        {{-- List Rows --}}
        <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
            <div class="min-w-[840px]">
                {{-- Header kolom konsisten & rata sempurna --}}
                <div class="grid grid-cols-[minmax(260px,1.8fr)_140px_140px_110px_124px] items-center gap-4 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                    <span>{{ __('admin.posts.col_title') }}</span>
                    <span>{{ __('admin.posts.col_author') }}</span>
                    <span>{{ __('admin.posts.col_category') }}</span>
                    <span class="text-center">{{ __('admin.posts.col_created') }}</span>
                    <span class="text-center">{{ __('admin.common.col_action') }}</span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                    @forelse ($karya as $post)
                        @php $postFallback = \App\Support\AdminMedia::bannerFallback(); @endphp
                        <div class="px-6 py-4 grid grid-cols-[minmax(260px,1.8fr)_140px_140px_110px_124px] items-center gap-4 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                            {{-- Thumbnail & Title --}}
                            <div class="flex items-center gap-4 min-w-0">
                                <div class="w-12 h-12 flex-shrink-0 rounded-xl overflow-hidden bg-gray-100 dark:bg-zinc-800 border border-[#E5E7EB]/60 dark:border-[#2D2D30]">
                                    <img src="{{ $post->file_url ?? $post->thumbnail_url ?? $postFallback }}"
                                         alt="{{ $post->judul }}"
                                         onerror="this.onerror=null;this.src='{{ $postFallback }}';"
                                         loading="lazy"
                                         decoding="async"
                                         class="w-full h-full object-cover">
                                </div>
                                <span class="min-w-0 text-sm font-semibold text-[#0F172A] dark:text-white truncate">{{ $post->judul }}</span>
                            </div>

                            {{-- Author --}}
                            <span class="min-w-0 text-sm font-medium text-[#0F172A] dark:text-white truncate">{{ $post->pengguna->nama_panggilan ?? '—' }}</span>

                            {{-- Category --}}
                            <span class="min-w-0 text-sm font-medium text-[#64748B] dark:text-[#A1A1AA] truncate">{{ $post->kategori->kategori ?? $post->kategori->nama_kategori ?? '—' }}</span>

                            {{-- Created --}}
                            <span class="text-sm font-medium text-[#64748B] dark:text-[#A1A1AA] text-center tabular-nums whitespace-nowrap">{{ ! empty($post->created_at) ? $post->created_at->format('d/m/y') : '—' }}</span>

                            {{-- Action --}}
                            <div class="flex items-center justify-center">
                                <button type="button"
                                        onclick="openDeleteModal('{{ $post->id_karya }}', '{{ addslashes($post->judul) }}')"
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
                            {{ __('admin.posts.empty') }}
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        {{-- Artwork Card Grid --}}
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            @forelse ($karya as $post)
                <div class="flex flex-col rounded-2xl overflow-hidden bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                    @php $postFallback = \App\Support\AdminMedia::bannerFallback(); @endphp
                    <div class="h-36 w-full overflow-hidden bg-gray-100 dark:bg-zinc-800">
                        <img src="{{ $post->file_url ?? $post->thumbnail_url ?? $postFallback }}"
                             alt="{{ $post->judul }}"
                             onerror="this.onerror=null;this.src='{{ $postFallback }}';"
                             loading="lazy"
                             decoding="async"
                             class="w-full h-full object-cover hover:scale-105 transition-transform duration-300">
                    </div>
                    <div class="flex flex-col flex-1 p-5 gap-3.5">
                        <h3 class="text-base font-semibold text-[#0F172A] dark:text-white leading-snug line-clamp-2">{{ $post->judul }}</h3>
                        <div class="grid grid-cols-3 gap-3 text-xs">
                            <div class="min-w-0">
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">author</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $post->pengguna->nama_panggilan ?? '—' }}</span>
                            </div>
                            <div class="min-w-0">
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">category</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $post->kategori->kategori ?? $post->kategori->nama_kategori ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[11px] text-[#94A3B8] dark:text-[#71717A] mb-0.5">created</span>
                                <span class="font-medium text-[#0F172A] dark:text-white tabular-nums whitespace-nowrap">{{ ! empty($post->created_at) ? $post->created_at->format('d/m/y') : '—' }}</span>
                            </div>
                        </div>
                        <div class="pt-2 mt-auto">
                            <button type="button"
                                    onclick="openDeleteModal('{{ $post->id_karya }}', '{{ addslashes($post->judul) }}')"
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
                    No artwork posts found.
                </div>
            @endforelse
        </div>
    @endif

    @if ($karya instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $karya->withQueryString()->links('admin.components.navigation.pagination') }}
    @endif
    </div>
</div>

@include('admin.components.modals.confirm-delete', [
    'modalId' => 'deleteModal',
    'title' => 'Delete Artwork',
    'entityLabel' => 'artwork',
    'actionPrefix' => '/admin/posts',
    'openFn' => 'openDeleteModal',
    'closeFn' => 'closeDeleteModal',
])
@endsection
