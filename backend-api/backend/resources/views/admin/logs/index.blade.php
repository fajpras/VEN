@extends('admin.layouts.app')

@section('title', $tab === 'keamanan' ? 'Security log' : 'Activity log')
@section('page_title', 'Activity log')

@section('content')
<div class="space-y-8">
    {{-- Search Bar & Tab Switcher Header --}}
    <form method="GET" action="{{ route('admin.logs') }}" class="flex flex-col sm:flex-row items-center gap-4 w-full">
        <input type="hidden" name="tab" value="{{ $tab }}">
        <div class="relative flex items-center flex-1 w-full">
            <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
            <input type="text"
                   name="cari"
                   value="{{ request('cari') }}"
                   placeholder="{{ $tab === 'keamanan' ? 'Search event, endpoint, method, IP, message...' : 'Search activity, entity, action, message...' }}"
                   class="w-full h-12 pl-13 pr-6 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] text-[#0F172A] dark:text-white placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
        </div>

        {{-- Tab Switcher: Log Aktivitas vs Log Keamanan --}}
        <div class="flex items-center gap-1.5 p-1 rounded-full bg-[#EBEBEB] dark:bg-[#27272A] flex-shrink-0 h-12 select-none">
            <a href="{{ route('admin.logs', array_merge(request()->except(['page']), ['tab' => 'aktivitas'])) }}"
               class="h-10 px-5 rounded-full text-sm font-semibold flex items-center justify-center transition-all {{ $tab === 'aktivitas' ? 'bg-[#BA18F5] text-white shadow-md shadow-purple-500/25' : 'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
                Log Aktivitas
            </a>
            <a href="{{ route('admin.logs', array_merge(request()->except(['page']), ['tab' => 'keamanan'])) }}"
               class="h-10 px-5 rounded-full text-sm font-semibold flex items-center justify-center transition-all {{ $tab === 'keamanan' ? 'bg-[#BA18F5] text-white shadow-md shadow-purple-500/25' : 'text-[#64748B] dark:text-[#A1A1AA] hover:text-[#0F172A] dark:hover:text-white' }}">
                Log Keamanan
            </a>
        </div>
    </form>

    @include('admin.layouts.list-controls')

    @if ($tab === 'keamanan')
        {{-- ==================== TAB LOG KEAMANAN ==================== --}}
        @if ($viewMode === 'list')
            <div class="overflow-x-auto">
                <div class="min-w-[760px]">
                    <div class="grid grid-cols-[160px_1fr_90px_130px_100px_70px] items-center gap-4 px-2 pb-2 text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">
                        <span>event</span>
                        <span>endpoint</span>
                        <span>status</span>
                        <span>ip address</span>
                        <span>date</span>
                        <span></span>
                    </div>

                    <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A] border-t border-b border-[#E5E7EB] dark:border-[#27272A]">
                        @forelse ($logs as $log)
                            @php
                                $statusClass = match (true) {
                                    $log->status_kode >= 200 && $log->status_kode < 300 => 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800/40',
                                    $log->status_kode >= 400 && $log->status_kode < 500 => 'bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400 border-amber-200 dark:border-amber-800/40',
                                    default => 'bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-400 border-rose-200 dark:border-rose-800/40',
                                };
                            @endphp
                            <div class="py-3 grid grid-cols-[160px_1fr_90px_130px_100px_70px] items-center gap-4 hover:bg-gray-50/50 dark:hover:bg-zinc-900/30 transition-colors px-2 rounded-xl">
                                <span class="font-mono text-xs font-semibold text-[#BA18F5] truncate">
                                    {{ $log->event }}
                                </span>

                                <div class="min-w-0">
                                    <p class="font-mono text-xs text-[#0F172A] dark:text-white truncate">
                                        <span class="font-semibold text-gray-500 dark:text-zinc-400">{{ $log->method }}</span>
                                        {{ $log->endpoint }}
                                    </p>
                                    @if ($log->pesan)
                                        <p class="text-[11px] text-[#94A3B8] dark:text-[#71717A] truncate">{{ $log->pesan }}</p>
                                    @endif
                                </div>

                                <div>
                                    <span class="inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full border {{ $statusClass }}">
                                        {{ $log->status_kode }}
                                    </span>
                                </div>

                                <span class="font-mono text-xs text-[#64748B] dark:text-[#A1A1AA] truncate">
                                    {{ $log->ip_address }}
                                </span>

                                <span class="text-xs font-medium text-[#94A3B8] dark:text-[#71717A] whitespace-nowrap">
                                    {{ $log->waktu ? $log->waktu->format('d/m/y H:i') : ($log->created_at ? $log->created_at->format('d/m/y H:i') : '—') }}
                                </span>

                                <div class="flex items-center justify-end">
                                    <button type="button"
                                            onclick="openDetailModal({{ json_encode($log) }}, 'keamanan')"
                                            class="text-sm font-medium text-[#BA18F5] hover:text-[#9333EA] transition-colors cursor-pointer">
                                        Detail
                                    </button>
                                </div>
                            </div>
                        @empty
                            <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                                No security logs recorded.
                            </div>
                        @endforelse
                    </div>
                </div>
            </div>
        @else
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                @forelse ($logs as $log)
                    @php
                        $statusClass = match (true) {
                            $log->status_kode >= 200 && $log->status_kode < 300 => 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800/40',
                            $log->status_kode >= 400 && $log->status_kode < 500 => 'bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400 border-amber-200 dark:border-amber-800/40',
                            default => 'bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-400 border-rose-200 dark:border-rose-800/40',
                        };
                    @endphp
                    <div class="flex flex-col rounded-2xl p-4 gap-2 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                        <div class="flex items-center justify-between gap-2">
                            <span class="font-mono text-xs font-semibold text-[#BA18F5] truncate">{{ $log->event }}</span>
                            <span class="px-2 py-0.5 text-[10px] font-semibold rounded-full border {{ $statusClass }}">{{ $log->status_kode }}</span>
                        </div>
                        <div class="p-2 bg-gray-50 dark:bg-zinc-800/40 rounded-xl font-mono text-xs text-[#0F172A] dark:text-white truncate">
                            <span class="font-semibold text-gray-500 dark:text-zinc-400">{{ $log->method }}</span>
                            {{ $log->endpoint }}
                        </div>
                        <div class="grid grid-cols-2 gap-1.5 text-xs mt-1">
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">ip</span>
                                <span class="font-mono font-medium text-[#0F172A] dark:text-white truncate block">{{ $log->ip_address }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">time</span>
                                <span class="font-medium text-[#0F172A] dark:text-white whitespace-nowrap">{{ $log->waktu ? $log->waktu->format('d/m/y H:i') : '—' }}</span>
                            </div>
                        </div>
                        <div class="pt-2 mt-auto">
                            <button type="button"
                                    onclick="openDetailModal({{ json_encode($log) }}, 'keamanan')"
                                    class="h-8 w-full rounded-full bg-[#BA18F5] text-white font-semibold text-[11px] hover:opacity-90 transition-opacity cursor-pointer">
                                Detail
                            </button>
                        </div>
                    </div>
                @empty
                    <div class="col-span-full py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                        No security logs recorded.
                    </div>
                @endforelse
            </div>
        @endif
    @else
        {{-- ==================== TAB LOG AKTIVITAS ==================== --}}
        @if ($viewMode === 'list')
            <div class="overflow-x-auto">
                <div class="min-w-[600px]">
                    <div class="grid grid-cols-[1fr_100px_80px] items-center gap-4 px-2 pb-2 text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">
                        <span>activity</span>
                        <span>date</span>
                        <span></span>
                    </div>

                    <div class="divide-y divide-[#E5E7EB] dark:divide-[#27272A] border-t border-b border-[#E5E7EB] dark:border-[#27272A]">
                        @forelse ($logs as $log)
                            <div class="py-3 grid grid-cols-[1fr_100px_80px] items-center gap-4 hover:bg-gray-50/50 dark:hover:bg-zinc-900/30 transition-colors px-2 rounded-xl">
                                <p class="min-w-0 text-sm font-normal text-[#0F172A] dark:text-white leading-relaxed">
                                    {{ $log->pesan ?? $log->deskripsi ?? 'Admin action performed on system' }}
                                </p>

                                <span class="text-xs font-medium text-[#94A3B8] dark:text-[#71717A] whitespace-nowrap">
                                    {{ $log->waktu ? $log->waktu->format('d/m/y') : ($log->created_at ? $log->created_at->format('d/m/y') : '—') }}
                                </span>

                                <div class="flex items-center justify-end">
                                    <button type="button"
                                            onclick="openDetailModal({{ json_encode($log) }}, 'aktivitas')"
                                            class="text-sm font-medium text-[#BA18F5] hover:text-[#9333EA] transition-colors cursor-pointer">
                                        Detail
                                    </button>
                                </div>
                            </div>
                        @empty
                            <div class="py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                                No activity logs recorded.
                            </div>
                        @endforelse
                    </div>
                </div>
            </div>
        @else
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                @forelse ($logs as $log)
                    <div class="flex flex-col rounded-2xl p-4 gap-2 bg-white dark:bg-[#1E1E20] border border-[#E5E7EB] dark:border-[#2D2D30] hover:border-[#BA18F5]/50 transition-colors">
                        <div class="p-2.5 bg-purple-50 dark:bg-purple-950/20 rounded-xl border border-purple-100 dark:border-purple-900/40">
                            <p class="text-xs font-medium text-[#0F172A] dark:text-white leading-relaxed line-clamp-3">
                                {{ $log->pesan ?? $log->deskripsi ?? 'Admin action performed on system' }}
                            </p>
                        </div>
                        <div class="grid grid-cols-2 gap-1.5 text-xs">
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">administrator</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $log->pengguna->nama ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">timestamp</span>
                                <span class="font-medium text-[#0F172A] dark:text-white whitespace-nowrap">{{ $log->waktu ? $log->waktu->format('d/m/y') : ($log->created_at ? $log->created_at->format('d/m/y') : '—') }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">entity</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $log->entitas ?? $log->tipe_entitas ?? '—' }}</span>
                            </div>
                            <div>
                                <span class="block text-[10px] leading-tight text-[#94A3B8] dark:text-[#71717A]">action</span>
                                <span class="font-medium text-[#0F172A] dark:text-white truncate block">{{ $log->aksi ?? '—' }}</span>
                            </div>
                        </div>
                        <div class="pt-1 mt-auto">
                            <button type="button"
                                    onclick="openDetailModal({{ json_encode($log) }}, 'aktivitas')"
                                    class="h-8 w-full rounded-full bg-[#BA18F5] text-white font-semibold text-[11px] hover:opacity-90 transition-opacity cursor-pointer">
                                Detail
                            </button>
                        </div>
                    </div>
                @empty
                    <div class="col-span-full py-16 text-center text-[#94A3B8] dark:text-[#71717A]">
                        No activity logs recorded.
                    </div>
                @endforelse
            </div>
        @endif
    @endif

    @if ($logs instanceof \Illuminate\Pagination\LengthAwarePaginator)
        {{ $logs->withQueryString()->links('admin.layouts.pagination') }}
    @endif
</div>

{{-- Detail Audit Trail Modal --}}
<div id="detailModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="closeDetailModal()"></div>
    <div class="relative w-full max-w-lg bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-6">
            <h2 id="modalTitle" class="text-lg font-semibold text-[#0F172A] dark:text-white">Activity Log Details</h2>
            <button type="button" onclick="closeDetailModal()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <div class="space-y-4">
            <div id="modalPrimaryBox" class="p-4 bg-purple-50 dark:bg-purple-950/20 rounded-2xl border border-purple-100 dark:border-purple-900/40">
                <p id="modalPrimaryLabel" class="text-xs font-semibold uppercase text-[#BA18F5] mb-1">Action Description</p>
                <p id="modalDesc" class="text-sm font-semibold text-[#0F172A] dark:text-white"></p>
            </div>

            <div class="grid grid-cols-2 gap-4 text-sm">
                <div class="p-3 bg-gray-50 dark:bg-zinc-800/40 rounded-xl">
                    <span id="modalActorLabel" class="block text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">Administrator</span>
                    <span id="modalAdmin" class="font-medium text-[#0F172A] dark:text-white">—</span>
                </div>

                <div class="p-3 bg-gray-50 dark:bg-zinc-800/40 rounded-xl">
                    <span class="block text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">Timestamp</span>
                    <span id="modalTime" class="font-medium text-[#0F172A] dark:text-white"></span>
                </div>

                <div class="p-3 bg-gray-50 dark:bg-zinc-800/40 rounded-xl">
                    <span class="block text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">IP Address</span>
                    <span id="modalIp" class="font-mono text-xs text-[#0F172A] dark:text-white"></span>
                </div>

                <div class="p-3 bg-gray-50 dark:bg-zinc-800/40 rounded-xl">
                    <span id="modalSecondLabel" class="block text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">Entity Target</span>
                    <span id="modalEntity" class="font-medium text-[#0F172A] dark:text-white"></span>
                </div>
            </div>

            <div class="p-3 bg-gray-50 dark:bg-zinc-800/40 rounded-xl">
                <span class="block text-xs text-[#94A3B8] dark:text-[#71717A] font-medium">Client User Agent</span>
                <p id="modalAgent" class="font-mono text-xs text-gray-600 dark:text-zinc-400 break-all mt-1"></p>
            </div>

            <div class="flex justify-end pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="closeDetailModal()" class="h-10 px-6 rounded-full bg-[#BA18F5] text-white font-medium text-sm hover:opacity-90 transition-opacity cursor-pointer">
                    Close
                </button>
            </div>
        </div>
    </div>
</div>

@push('scripts')
<script>
    function openDetailModal(log, type) {
        const modal = document.getElementById('detailModal');
        const modalTitle = document.getElementById('modalTitle');
        const primaryLabel = document.getElementById('modalPrimaryLabel');
        const actorLabel = document.getElementById('modalActorLabel');
        const secondLabel = document.getElementById('modalSecondLabel');

        if (type === 'keamanan') {
            modalTitle.textContent = 'Security Log Details';
            primaryLabel.textContent = 'Event & HTTP Status (' + (log.status_kode || '—') + ')';
            document.getElementById('modalDesc').textContent = (log.event || 'Security Event') + ' — ' + (log.method || '') + ' ' + (log.endpoint || '');
            actorLabel.textContent = 'Account / Actor';
            document.getElementById('modalAdmin').textContent = log.pengguna ? (log.pengguna.nama + ' (' + (log.pengguna.email || '') + ')') : (log.id_pengguna || 'Anonymous / Guest');
            secondLabel.textContent = 'Event Detail';
            document.getElementById('modalEntity').textContent = log.pesan || '—';
        } else {
            modalTitle.textContent = 'Activity Log Details';
            primaryLabel.textContent = 'Action Description';
            document.getElementById('modalDesc').textContent = log.pesan || log.deskripsi || 'Administrative action';
            actorLabel.textContent = 'Administrator';
            document.getElementById('modalAdmin').textContent = log.pengguna ? log.pengguna.nama : '—';
            secondLabel.textContent = 'Entity Target';
            document.getElementById('modalEntity').textContent = (log.entitas || log.tipe_entitas || 'General') + (log.id_entitas ? ' #' + log.id_entitas : (log.aksi ? ' (' + log.aksi + ')' : ''));
        }

        document.getElementById('modalTime').textContent = log.waktu ? new Date(log.waktu).toLocaleString() : (log.created_at ? new Date(log.created_at).toLocaleString() : '—');
        document.getElementById('modalIp').textContent = log.ip_address || '—';
        document.getElementById('modalAgent').textContent = log.user_agent || '—';

        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    function closeDetailModal() {
        const modal = document.getElementById('detailModal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
</script>
@endpush
@endsection
