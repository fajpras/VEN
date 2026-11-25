@extends('admin.layouts.app')

@section('title', __('admin.nav.accounts'))
@section('page_title', __('admin.nav.accounts'))

@section('content')
<div class="space-y-8">
    {{-- Search Bar & Dashboard Filters --}}
    <div class="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 w-full">
        @include('admin.components.toolbar.search-bar', [
            'action' => route('admin.accounts'),
            'hidden' => [
                'status' => $status,
                'sort' => $sort !== 'newest' ? $sort : null,
            ],
        ])

        <div class="flex flex-wrap items-center gap-2.5">
            @include('admin.components.toolbar.filter-dropdown', [
                'param' => 'status',
                'value' => $status,
                'placeholder' => __('admin.accounts.filter_status'),
                'options' => [
                    'aktif' => __('admin.accounts.status_active'),
                    'ditangguhkan' => __('admin.accounts.status_suspended'),
                    'nonaktifkan' => __('admin.accounts.status_blocked'),
                ],
            ])
            @include('admin.components.toolbar.filter-dropdown', [
                'param' => 'sort',
                'value' => $sort === 'newest' ? '' : $sort,
                'placeholder' => __('admin.common.sort_newest'),
                'options' => [
                    'oldest' => __('admin.common.sort_oldest'),
                    'posts' => __('admin.accounts.sort_posts'),
                    'exhibitions' => __('admin.accounts.sort_exhibitions'),
                ],
            ])
        </div>
    </div>

    @include('admin.components.toolbar.list-controls')

    <div data-live-region class="space-y-8">
    @if ($viewMode === 'list')
        {{-- Account List --}}
        <div class="overflow-x-auto rounded-2xl border border-[#E5E7EB] dark:border-[#2D2D30] bg-white dark:bg-[#1E1E20]">
            <div class="min-w-[900px]">
                {{-- Header kolom konsisten & rata sempurna --}}
                <div class="grid grid-cols-[minmax(240px,1.6fr)_minmax(150px,1.2fr)_90px_110px_110px_136px] items-center gap-4 px-6 py-3.5 text-[11px] uppercase tracking-wider text-[#94A3B8] dark:text-[#71717A] font-semibold bg-[#F8FAFC] dark:bg-[#18181B] border-b border-[#E5E7EB] dark:border-[#2D2D30]">
                    <span>{{ __('admin.accounts.col_username') }}</span>
                    <span>{{ __('admin.accounts.col_name') }}</span>
                    <span class="text-center">{{ __('admin.accounts.col_post') }}</span>
                    <span class="text-center">{{ __('admin.accounts.col_exhibition') }}</span>
                    <span class="text-center">{{ __('admin.accounts.col_join_date') }}</span>
                    <span class="text-center">{{ __('admin.common.col_action') }}</span>
                </div>

                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A]">
                    @forelse ($pengguna as $user)
                        @php
                            $avatarFallback = \App\Support\AdminMedia::avatarFallback($user->nama);
                            $avatarUrl = \App\Support\AdminMedia::imageUrl($user->foto_profil) ?? $avatarFallback;
                        @endphp
                        <div class="px-6 py-4 grid grid-cols-[minmax(240px,1.6fr)_minmax(150px,1.2fr)_90px_110px_110px_136px] items-center gap-4 hover:bg-[#FAF5FF]/50 dark:hover:bg-zinc-800/40 transition-colors">
                            {{-- Avatar & Username --}}
                            <div class="flex items-center gap-3.5 min-w-0">
                                <img src="{{ $avatarUrl }}"
                                     alt="{{ $user->nama }}"
                                     referrerpolicy="no-referrer"
                                     onerror="this.onerror=null;this.src='{{ $avatarFallback }}';"
                                     loading="lazy"
                                     decoding="async"
                                     class="w-10 h-10 rounded-full object-cover border border-gray-100 dark:border-zinc-800 shadow-xs flex-shrink-0">
                                <span class="text-sm font-semibold text-[#0F172A] dark:text-white truncate">{{ $user->nama_panggilan }}</span>
                            </div>

                            {{-- Name --}}
                            <span class="min-w-0 text-sm font-medium text-[#0F172A] dark:text-white truncate">{{ $user->nama }}</span>

                            {{-- Post --}}
                            <span class="text-sm font-medium text-[#0F172A] dark:text-white text-center tabular-nums">{{ $user->karya_count ?? $user->karya()->count() }}</span>

                            {{-- Exhibition --}}
                            <span class="text-sm font-medium text-[#0F172A] dark:text-white text-center tabular-nums">{{ $user->pameran_count ?? $user->pameran()->count() }}</span>

                            {{-- Join Date --}}
                            <span class="text-sm font-medium text-[#64748B] dark:text-[#A1A1AA] text-center tabular-nums whitespace-nowrap">{{ $user->created_at ? $user->created_at->format('d/m/y') : '—' }}</span>

                            {{-- Action Button: Suspend --}}
                            <div class="flex items-center justify-center">
                                <button type="button"
                                        onclick="openSuspendModal('{{ $user->id_pengguna }}', '{{ addslashes($user->nama) }}', '{{ addslashes($user->nama_panggilan) }}')"
                                        class="h-9 min-w-[118px] px-4 rounded-full bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 font-semibold text-xs tracking-wide hover:bg-rose-600 hover:text-white hover:border-rose-600 transition-colors cursor-pointer inline-flex items-center justify-center gap-1.5">
                                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"></path>
                                    </svg>
                                    <span>{{ __('admin.common.suspend') }}</span>
                                </button>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A] text-sm">
                            {{ __('admin.accounts.empty') }}
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        {{-- Account Card Grid --}}
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            @forelse ($pengguna as $user)
                @php
                    $avatarFallback = \App\Support\AdminMedia::avatarFallback($user->nama);
                    $avatarUrl = \App\Support\AdminMedia::imageUrl($user->foto_profil) ?? $avatarFallback;
                @endphp
                <div class="flex flex-col items-center text-center rounded-2xl p-5 gap-3.5 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                    <img src="{{ $avatarUrl }}"
                         alt="{{ $user->nama }}"
                         referrerpolicy="no-referrer"
                         onerror="this.onerror=null;this.src='{{ $avatarFallback }}';"
                         loading="lazy"
                         decoding="async"
                         class="w-16 h-16 rounded-full object-cover border border-gray-100 dark:border-zinc-800 shadow-xs flex-shrink-0">
                    <div class="w-full min-w-0">
                        <h3 class="text-base font-semibold text-[#0F172A] dark:text-white leading-snug truncate">{{ $user->nama }}</h3>
                        <p class="text-xs text-[#94A3B8] dark:text-[#71717A] truncate">{{ '@' . $user->nama_panggilan }}</p>
                        <p class="text-xs text-[#94A3B8] dark:text-[#71717A] truncate mt-0.5">{{ $user->email ?? '' }}</p>
                    </div>
                    <div class="grid w-full grid-cols-3 gap-2 pt-2 border-t border-[#E5E7EB] dark:border-[#27272A]">
                        <div>
                            <span class="block text-sm font-semibold text-[#0F172A] dark:text-white tabular-nums">{{ $user->karya_count ?? $user->karya()->count() }}</span>
                            <span class="block text-[11px] leading-tight text-[#94A3B8] dark:text-[#71717A]">post</span>
                        </div>
                        <div>
                            <span class="block text-sm font-semibold text-[#0F172A] dark:text-white tabular-nums">{{ $user->pameran_count ?? $user->pameran()->count() }}</span>
                            <span class="block text-[11px] leading-tight text-[#94A3B8] dark:text-[#71717A]">exhibition</span>
                        </div>
                        <div>
                            <span class="block text-xs font-medium text-[#0F172A] dark:text-white tabular-nums whitespace-nowrap">{{ $user->created_at ? $user->created_at->format('d/m/y') : '—' }}</span>
                            <span class="block text-[11px] leading-tight text-[#94A3B8] dark:text-[#71717A]">join date</span>
                        </div>
                    </div>
                    <div class="w-full pt-1 mt-auto">
                        <button type="button"
                                onclick="openSuspendModal('{{ $user->id_pengguna }}', '{{ addslashes($user->nama) }}', '{{ addslashes($user->nama_panggilan) }}')"
                                class="h-9 w-full rounded-full bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 font-semibold text-xs tracking-wide hover:bg-rose-600 hover:text-white hover:border-rose-600 transition-colors cursor-pointer inline-flex items-center justify-center gap-1.5">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"></path>
                            </svg>
                            <span>{{ __('admin.common.suspend') }}</span>
                        </button>
                    </div>
                </div>
            @empty
                <div class="col-span-full py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                    No user accounts found.
                </div>
            @endforelse
        </div>
    @endif

    @if ($pengguna instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $pengguna->withQueryString()->links('admin.components.navigation.pagination') }}
    @endif
    </div>
</div>

{{-- Suspend Modal Dialog --}}
<div id="suspendModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="closeSuspendModal()"></div>
    <div class="relative w-full max-w-lg bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-6">
            <h2 class="text-lg font-semibold text-[#0F172A] dark:text-white">Suspend User Account</h2>
            <button type="button" onclick="closeSuspendModal()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <form id="suspendForm" method="POST" action="" class="space-y-5">
            @csrf
            <div class="p-4 bg-gray-50 dark:bg-zinc-800/50 rounded-2xl">
                <p id="modalUserName" class="font-semibold text-base text-[#0F172A] dark:text-white"></p>
                <p id="modalUserHandle" class="text-xs text-[#94A3B8] dark:text-[#71717A]"></p>
            </div>

            <div>
                <label class="block text-xs font-semibold uppercase text-gray-500 dark:text-zinc-400 mb-2">Sanction Type</label>
                <select name="jenis_sanksi" class="w-full h-11 px-4 rounded-xl bg-gray-100 dark:bg-zinc-800 text-[#0F172A] dark:text-white text-sm outline-none border border-transparent focus:border-[#BA18F5]">
                    <option value="ditangguhkan">Suspension (Temporary)</option>
                    <option value="diblokir">Permanent Ban (Blocked)</option>
                </select>
            </div>

            <div>
                <label class="block text-xs font-semibold uppercase text-gray-500 dark:text-zinc-400 mb-2">Duration</label>
                <select name="duration" class="w-full h-11 px-4 rounded-xl bg-gray-100 dark:bg-zinc-800 text-[#0F172A] dark:text-white text-sm outline-none border border-transparent focus:border-[#BA18F5]">
                    <option value="24h">24 Hours</option>
                    <option value="7d">7 Days</option>
                    <option value="30d">30 Days</option>
                    <option value="permanent">Permanent</option>
                </select>
            </div>

            <div>
                <label class="block text-xs font-semibold uppercase text-gray-500 dark:text-zinc-400 mb-2">Reason for Suspension *</label>
                <textarea name="alasan" rows="3" required placeholder="Specify policy violation reason..." class="w-full p-4 rounded-xl bg-gray-100 dark:bg-zinc-800 text-[#0F172A] dark:text-white placeholder-gray-400 text-sm outline-none border border-transparent focus:border-[#BA18F5]"></textarea>
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="closeSuspendModal()" class="h-10 px-5 rounded-full text-gray-600 dark:text-zinc-400 hover:bg-gray-100 dark:hover:bg-zinc-800 font-medium text-sm transition-colors cursor-pointer">
                    Cancel
                </button>
                <button type="submit" class="h-10 px-6 rounded-full bg-rose-600 text-white font-semibold text-sm hover:bg-rose-700 transition-colors cursor-pointer">
                    Confirm Suspend
                </button>
            </div>
        </form>
    </div>
</div>

@push('scripts')
<script>
    function openSuspendModal(userId, name, handle) {
        const modal = document.getElementById('suspendModal');
        const form = document.getElementById('suspendForm');
        document.getElementById('modalUserName').textContent = name;
        document.getElementById('modalUserHandle').textContent = '@' + handle;
        form.action = `/admin/accounts/${userId}/suspend`;
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    function closeSuspendModal() {
        const modal = document.getElementById('suspendModal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
</script>
@endpush
@endsection
