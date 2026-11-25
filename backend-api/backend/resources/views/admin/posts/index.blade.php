@extends('admin.layouts.app')

@section('title', 'Post')
@section('page_title', 'Post')

@section('content')
<div class="space-y-8">
    {{-- Search & Filter Header --}}
    <form id="postFilterForm" method="GET" action="{{ route('admin.posts') }}" class="flex flex-col sm:flex-row items-center gap-4 w-full">
        <div class="relative flex items-center flex-1 w-full">
            <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
            <input type="text"
                   name="cari"
                   value="{{ request('cari') }}"
                   placeholder="Search"
                   class="w-full h-12 pl-13 pr-6 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
        </div>

        {{-- Custom Category Dropdown --}}
        <div class="relative w-full sm:w-[260px] flex-shrink-0" id="categoryDropdownContainer">
            {{-- Hidden input for category value --}}
            <input type="hidden" name="kategori" id="selectedCategoryInput" value="{{ request('kategori') }}">

            {{-- Dropdown Trigger Button --}}
            <button type="button"
                    id="categoryDropdownTrigger"
                    onclick="toggleCategoryDropdown()"
                    aria-expanded="false"
                    aria-haspopup="true"
                    class="w-full h-12 pl-6 pr-5 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white font-medium text-sm flex items-center justify-between gap-3 border border-transparent focus:ring-2 focus:ring-[#BA18F5] outline-none cursor-pointer transition-all hover:bg-gray-200/80 dark:hover:bg-zinc-700/60 select-none">
                <span id="categoryButtonText" class="truncate font-medium">
                    {{ request('kategori') ?: 'All Categories' }}
                </span>
                <svg id="categoryChevron" class="w-4 h-4 text-gray-500 dark:text-zinc-400 transition-transform duration-200 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                </svg>
            </button>

            {{-- Custom Dropdown Popover Menu (Aligned identically to button width) --}}
            <div id="categoryDropdownMenu"
                 class="absolute left-0 right-0 top-full mt-2 w-full bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] rounded-2xl shadow-xl shadow-black/10 dark:shadow-black/40 p-1.5 z-30 hidden transition-all">
                {{-- Option: All Categories --}}
                <button type="button"
                        onclick="selectCategory('')"
                        class="w-full px-4 py-2.5 rounded-xl text-left text-sm flex items-center justify-between transition-colors cursor-pointer {{ !request('kategori') ? 'text-[#BA18F5] font-semibold bg-purple-50 dark:bg-purple-950/30' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
                    <span>All Categories</span>
                    @if (!request('kategori'))
                        <svg class="w-4 h-4 text-[#BA18F5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                        </svg>
                    @endif
                </button>

                {{-- Category List --}}
                @foreach ($kategoriList as $cat)
                    @php $isSelected = (request('kategori') === $cat); @endphp
                    <button type="button"
                            onclick="selectCategory('{{ addslashes($cat) }}')"
                            class="w-full px-4 py-2.5 rounded-xl text-left text-sm flex items-center justify-between transition-colors cursor-pointer {{ $isSelected ? 'text-[#BA18F5] font-semibold bg-purple-50 dark:bg-purple-950/30' : 'text-[#0F172A] dark:text-white font-medium hover:bg-gray-100 dark:hover:bg-zinc-800' }}">
                        <span>{{ $cat }}</span>
                        @if ($isSelected)
                            <svg class="w-4 h-4 text-[#BA18F5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
                            </svg>
                        @endif
                    </button>
                @endforeach
            </div>
        </div>
    </form>

    @include('admin.layouts.list-controls')

    @if ($viewMode === 'list')
        {{-- List Rows --}}
        <div class="overflow-x-auto">
            <div class="min-w-[800px]">
                {{-- Header kolom (satu baris untuk semua post) --}}
                <div class="grid grid-cols-[56px_minmax(180px,1fr)_130px_130px_90px_minmax(90px,auto)] items-center gap-4 px-2 pb-2 text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">
                    <span></span>
                    <span>title</span>
                    <span>author</span>
                    <span>category</span>
                    <span>created at</span>
                    <span></span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A] border-t border-b border-[#E5E7EB] dark:border-[#27272A]">
                    @forelse ($karya as $post)
                        <div class="py-3 grid grid-cols-[56px_minmax(180px,1fr)_130px_130px_90px_minmax(90px,auto)] items-center gap-4 hover:bg-gray-50/50 dark:hover:bg-zinc-900/30 transition-colors px-2 rounded-xl">
                            {{-- Thumbnail --}}
                            <div class="w-14 h-14 flex-shrink-0 rounded-xl overflow-hidden bg-gray-100 dark:bg-zinc-800">
                                <img src="{{ $post->file_url ?? $post->thumbnail_url ?? 'data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22%20viewBox=%220%200%203%204%22%3E%3Crect%20width=%223%22%20height=%224%22%20fill=%22%23E5E7EB%22/%3E%3C/svg%3E' }}"
                                     alt="{{ $post->judul }}"
                                     loading="lazy"
                                     decoding="async"
                                     class="w-full h-full object-cover">
                            </div>

                            {{-- Title --}}
                            <span class="min-w-0 text-sm font-semibold text-[#0F172A] dark:text-white truncate">{{ $post->judul }}</span>

                            {{-- Author --}}
                            <span class="min-w-0 text-sm font-medium text-[#0F172A] dark:text-white truncate">{{ $post->pengguna->nama_panggilan ?? '—' }}</span>

                            {{-- Category --}}
                            <span class="min-w-0 text-sm font-medium text-[#0F172A] dark:text-white truncate">{{ $post->kategori->kategori ?? $post->kategori->nama_kategori ?? '—' }}</span>

                            {{-- Created --}}
                            <span class="text-sm font-medium text-[#0F172A] dark:text-white whitespace-nowrap">{{ ! empty($post->created_at) ? $post->created_at->format('d/m/y') : '—' }}</span>

                            {{-- Action --}}
                            <div class="flex items-center justify-end">
                                <button type="button"
                                        onclick="openDeleteModal('{{ $post->id_karya }}', '{{ addslashes($post->judul) }}')"
                                        class="h-9 px-6 rounded-full bg-[#FF0000] text-white font-semibold text-xs tracking-wide hover:bg-red-600 transition-colors cursor-pointer">
                                    Delete
                                </button>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                            No artwork posts found.
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        {{-- Artwork Card Grid --}}
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
            @forelse ($karya as $post)
                <div class="flex flex-col rounded-2xl overflow-hidden bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                    <div class="h-24 w-full overflow-hidden bg-gray-100 dark:bg-zinc-800">
                        <img src="{{ $post->file_url ?? $post->thumbnail_url ?? 'data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22%20viewBox=%220%200%203%204%22%3E%3Crect%20width=%223%22%20height=%224%22%20fill=%22%23E5E7EB%22/%3E%3C/svg%3E' }}"
                             alt="{{ $post->judul }}"
                             loading="lazy"
                             decoding="async"
                             class="w-full h-full object-cover hover:scale-105 transition-transform duration-300">
                    </div>
                    <div class="flex flex-col flex-1 p-4 gap-2">
                        <h3 class="text-sm font-semibold text-[#0F172A] dark:text-white leading-snug line-clamp-2">{{ $post->judul }}</h3>
                        <div class="grid grid-cols-3 gap-1 text-xs">
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">author</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $post->pengguna->nama_panggilan ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">category</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $post->kategori->kategori ?? $post->kategori->nama_kategori ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">created at</span>
                                <span class="font-medium text-[#0F172A] dark:text-white whitespace-nowrap">{{ ! empty($post->created_at) ? $post->created_at->format('d/m/y') : '—' }}</span>
                            </div>
                        </div>
                        <div class="pt-1 mt-auto">
                            <button type="button"
                                    onclick="openDeleteModal('{{ $post->id_karya }}', '{{ addslashes($post->judul) }}')"
                                    class="h-8 w-full rounded-full bg-[#FF0000] text-white font-semibold text-[11px] tracking-wide hover:bg-red-600 transition-colors cursor-pointer">
                                Delete
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
        {{ $karya->withQueryString()->links('admin.layouts.pagination') }}
    @endif
</div>

{{-- Delete Confirmation Modal --}}
<div id="deleteModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="closeDeleteModal()"></div>
    <div class="relative w-full max-w-md bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-6">
            <h2 class="text-lg font-semibold text-[#0F172A] dark:text-white">Delete Artwork</h2>
            <button type="button" onclick="closeDeleteModal()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <form id="deleteForm" method="POST" action="" class="space-y-5">
            @csrf
            @method('DELETE')
            <p class="text-sm text-[#0F172A] dark:text-[#D4D4D8]">
                Are you sure you want to delete artwork <strong id="deletePostTitle" class="text-black dark:text-white"></strong>? This will remove the post from public gallery feeds.
            </p>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="closeDeleteModal()" class="h-10 px-5 rounded-full text-gray-600 dark:text-zinc-400 hover:bg-gray-100 dark:hover:bg-zinc-800 font-medium text-sm transition-colors cursor-pointer">
                    Cancel
                </button>
                <button type="submit" class="h-10 px-6 rounded-full bg-[#FF0000] text-white font-semibold text-sm hover:bg-red-600 transition-colors cursor-pointer">
                    Delete Post
                </button>
            </div>
        </form>
    </div>
</div>

@push('scripts')
<script>
    function toggleCategoryDropdown() {
        const menu = document.getElementById('categoryDropdownMenu');
        const chevron = document.getElementById('categoryChevron');
        const trigger = document.getElementById('categoryDropdownTrigger');
        const isHidden = menu.classList.contains('hidden');

        if (isHidden) {
            menu.classList.remove('hidden');
            chevron.classList.add('rotate-180');
            trigger.setAttribute('aria-expanded', 'true');
        } else {
            menu.classList.add('hidden');
            chevron.classList.remove('rotate-180');
            trigger.setAttribute('aria-expanded', 'false');
        }
    }

    function selectCategory(category) {
        document.getElementById('selectedCategoryInput').value = category;
        document.getElementById('categoryButtonText').textContent = category || 'All Categories';
        document.getElementById('categoryDropdownMenu').classList.add('hidden');
        document.getElementById('categoryChevron').classList.remove('rotate-180');
        document.getElementById('categoryDropdownTrigger').setAttribute('aria-expanded', 'false');

        // Automatically submit the filter form
        document.getElementById('postFilterForm').submit();
    }

    // Close category dropdown when clicking outside
    document.addEventListener('click', function(e) {
        const container = document.getElementById('categoryDropdownContainer');
        if (container && !container.contains(e.target)) {
            const menu = document.getElementById('categoryDropdownMenu');
            const chevron = document.getElementById('categoryChevron');
            const trigger = document.getElementById('categoryDropdownTrigger');
            if (menu && !menu.classList.contains('hidden')) {
                menu.classList.add('hidden');
                chevron.classList.remove('rotate-180');
                trigger.setAttribute('aria-expanded', 'false');
            }
        }
    });

    // Close category dropdown on ESC key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const menu = document.getElementById('categoryDropdownMenu');
            const chevron = document.getElementById('categoryChevron');
            const trigger = document.getElementById('categoryDropdownTrigger');
            if (menu && !menu.classList.contains('hidden')) {
                menu.classList.add('hidden');
                chevron.classList.remove('rotate-180');
                trigger.setAttribute('aria-expanded', 'false');
            }
        }
    });

    function openDeleteModal(postId, title) {
        const modal = document.getElementById('deleteModal');
        const form = document.getElementById('deleteForm');
        document.getElementById('deletePostTitle').textContent = `"${title}"`;
        form.action = `/admin/posts/${postId}`;
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    function closeDeleteModal() {
        const modal = document.getElementById('deleteModal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
</script>
@endpush
@endsection
