@extends('admin.layouts.app')

@section('title', 'Account')
@section('page_title', 'Account')

@section('content')
<div class="space-y-8">
    {{-- Search Bar (Note: Filter dropdown "Active" is REMOVED per directive) --}}
    <form method="GET" action="{{ route('admin.accounts') }}" class="w-full">
        <div class="relative flex items-center max-w-4xl">
            <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
            <input type="text"
                   name="cari"
                   value="{{ request('cari') }}"
                   placeholder="Search"
                   class="w-full h-12 pl-13 pr-6 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
        </div>
    </form>

    @php
        $query = request()->query();
        unset($query['page']);

        $buildUrl = function (array $merge) use ($query) {
            return request()->url() . '?' . http_build_query(array_merge($query, $merge));
        };
    @endphp

    @if ($viewMode === 'list')
        {{-- Account List View with Headers Aligned to the Right of Card/List Toggle --}}
        <div class="overflow-x-auto">
            <div class="min-w-[850px] space-y-3">
                {{-- Header Row: Toggle [ Card | List ] on left, column headers to the right, Show per page on right end --}}
                <div class="grid grid-cols-12 items-center gap-4 px-2 py-1 text-xs text-[#94A3B8] dark:text-[#71717A] font-medium select-none">
                    {{-- Toggle Card / List --}}
                    <div class="col-span-2">
                        <div class="inline-flex items-center gap-1 p-1 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] w-fit" role="group" aria-label="Mode tampilan">
                            <a href="{{ $buildUrl(['view' => 'card']) }}"
                               class="h-8 px-3.5 rounded-full flex items-center gap-1.5 text-xs font-medium text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white transition-colors"
                               aria-pressed="false">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1H5a1 1 0 01-1-1V5zm9 0a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1h-5a1 1 0 01-1-1V5zM4 14a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1H5a1 1 0 01-1-1v-5zm9 0a1 1 0 011-1h5a1 1 0 011 1v5a1 1 0 01-1 1h-5a1 1 0 01-1-1v-5z" />
                                </svg>
                                Card
                            </a>
                            <a href="{{ $buildUrl(['view' => 'list']) }}"
                               class="h-8 px-3.5 rounded-full flex items-center gap-1.5 text-xs font-medium bg-white dark:bg-zinc-700 text-[#BA18F5] shadow-xs transition-colors"
                               aria-pressed="true">
                                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                                </svg>
                                List
                            </a>
                        </div>
                    </div>

                    {{-- Headers to the right of Card/List toggle --}}
                    <div class="col-span-2">
                        <span>username</span>
                    </div>
                    <div class="col-span-3">
                        <span>name</span>
                    </div>
                    <div class="col-span-1">
                        <span>post</span>
                    </div>
                    <div class="col-span-1">
                        <span>exhibition</span>
                    </div>
                    <div class="col-span-1">
                        <span>join date</span>
                    </div>
                    <div class="col-span-2 flex items-center justify-end">
                        <div class="flex items-center gap-1.5 text-xs text-[#64748B] dark:text-[#A1A1AA]">
                            <label for="perPageSelectList" class="font-medium">Show</label>
                            <select id="perPageSelectList"
                                    onchange="window.location.href = this.value"
                                    class="h-7 px-2 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white text-xs font-semibold outline-none focus:ring-2 focus:ring-[#BA18F5] cursor-pointer border border-transparent">
                                @foreach ($perPageOptions as $option)
                                    <option value="{{ $buildUrl(['per_page' => $option]) }}" {{ (int) $perPage === (int) $option ? 'selected' : '' }}>
                                        {{ $option }}
                                    </option>
                                @endforeach
                            </select>
                            <span class="font-medium">per page</span>
                        </div>
                    </div>
                </div>

                {{-- Account Data Rows --}}
                <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A] border-t border-b border-[#E5E7EB] dark:border-[#27272A]">
                    @forelse ($pengguna as $user)
                        <div class="py-3 grid grid-cols-12 items-center gap-4 hover:bg-gray-50/50 dark:hover:bg-zinc-900/30 transition-colors px-2 rounded-xl">
                            {{-- Avatar (under Card/List toggle) --}}
                            <div class="col-span-2 flex items-center pl-1">
                                <img src="{{ $user->foto_profil ? asset('storage/' . $user->foto_profil) : 'https://ui-avatars.com/api/?name=' . urlencode($user->nama) . '&background=BA18F5&color=fff' }}"
                                     alt="{{ $user->nama }}"
                                     class="w-10 h-10 rounded-full object-cover border border-gray-100 dark:border-zinc-800 shadow-xs flex-shrink-0">
                            </div>

                            {{-- Username --}}
                            <div class="col-span-2 min-w-0">
                                <span class="text-sm font-semibold text-[#0F172A] dark:text-white truncate block">{{ $user->nama_panggilan }}</span>
                            </div>

                            {{-- Name --}}
                            <div class="col-span-3 min-w-0">
                                <span class="text-sm font-medium text-[#0F172A] dark:text-white truncate block">{{ $user->nama }}</span>
                            </div>

                            {{-- Post --}}
                            <div class="col-span-1 min-w-0">
                                <span class="text-sm font-medium text-[#0F172A] dark:text-white">{{ $user->karya_count ?? $user->karya()->count() }}</span>
                            </div>

                            {{-- Exhibition --}}
                            <div class="col-span-1 min-w-0">
                                <span class="text-sm font-medium text-[#0F172A] dark:text-white">{{ $user->pameran_count ?? $user->pameran()->count() }}</span>
                            </div>

                            {{-- Join Date --}}
                            <div class="col-span-1 min-w-0">
                                <span class="text-sm font-medium text-[#0F172A] dark:text-white whitespace-nowrap">{{ $user->created_at ? $user->created_at->format('d/m/y') : '—' }}</span>
                            </div>

                            {{-- Action Button: Suspend (Red Pill) --}}
                            <div class="col-span-2 flex items-center justify-end">
                                <button type="button"
                                        onclick="openSuspendModal('{{ $user->id_pengguna }}', '{{ addslashes($user->nama) }}', '{{ addslashes($user->nama_panggilan) }}')"
                                        class="h-9 px-6 rounded-full bg-[#FF0000] text-white font-semibold text-xs tracking-wide hover:bg-red-600 transition-colors cursor-pointer">
                                    Suspend
                                </button>
                            </div>
                        </div>
                    @empty
                        <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                            No user accounts found.
                        </div>
                    @endforelse
                </div>
            </div>
        </div>
    @else
        @include('admin.layouts.list-controls')

        {{-- Account Card Grid --}}
        <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
            @forelse ($pengguna as $user)
                <div class="flex flex-col items-center text-center rounded-2xl p-6 gap-3 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                    <img src="{{ $user->foto_profil ? asset('storage/' . $user->foto_profil) : 'https://ui-avatars.com/api/?name=' . urlencode($user->nama) . '&background=BA18F5&color=fff' }}"
                         alt="{{ $user->nama }}"
                         class="w-16 h-16 rounded-full object-cover border border-gray-100 dark:border-zinc-800 shadow-xs flex-shrink-0">
                    <div>
                        <h3 class="text-base font-semibold text-[#0F172A] dark:text-white leading-snug">{{ $user->nama }}</h3>
                        <p class="text-sm text-[#94A3B8] dark:text-[#71717A]">{{ '@' . $user->nama_panggilan }}</p>
                        <p class="text-xs text-[#94A3B8] dark:text-[#71717A] truncate max-w-[220px]">{{ $user->email ?? '' }}</p>
                    </div>
                    <div class="flex items-center justify-center gap-6 text-sm py-2">
                        <div>
                            <span class="block text-base font-semibold text-[#0F172A] dark:text-white">{{ $user->karya_count ?? $user->karya()->count() }}</span>
                            <span class="block text-xs text-[#94A3B8] dark:text-[#71717A]">post</span>
                        </div>
                        <div>
                            <span class="block text-base font-semibold text-[#0F172A] dark:text-white">{{ $user->pameran_count ?? $user->pameran()->count() }}</span>
                            <span class="block text-xs text-[#94A3B8] dark:text-[#71717A]">exhibition</span>
                        </div>
                        <div>
                            <span class="block text-sm font-medium text-[#0F172A] dark:text-white">{{ $user->created_at ? $user->created_at->format('d/m/y') : '—' }}</span>
                            <span class="block text-xs text-[#94A3B8] dark:text-[#71717A]">join date</span>
                        </div>
                    </div>
                    <div class="w-full pt-2 mt-auto">
                        <button type="button"
                                onclick="openSuspendModal('{{ $user->id_pengguna }}', '{{ addslashes($user->nama) }}', '{{ addslashes($user->nama_panggilan) }}')"
                                class="h-9 w-full rounded-full bg-[#FF0000] text-white font-semibold text-xs tracking-wide hover:bg-red-600 transition-colors cursor-pointer">
                            Suspend
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
        {{ $pengguna->withQueryString()->links('admin.layouts.pagination') }}
    @endif
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
                <button type="submit" class="h-10 px-6 rounded-full bg-[#FF0000] text-white font-semibold text-sm hover:bg-red-600 transition-colors cursor-pointer">
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
