{{--
    Komponen Modal Konfirmasi Hapus (gaya GitHub: ketik "delete").
    Props:
      - $modalId: id elemen modal (default 'deleteModal')
      - $title: judul modal (mis. 'Delete Artwork', 'Delete Exhibition')
      - $entityLabel: nama entitas di teks konfirmasi (mis. 'artwork', 'exhibition')
      - $actionPrefix: prefix URL DELETE (mis. '/admin/posts', '/admin/exhibitions')
      - $openFn: nama fungsi JS untuk membuka modal (mis. 'openDeleteModal')
      - $closeFn: nama fungsi JS untuk menutup modal (mis. 'closeDeleteModal')
--}}
@php
    $modalId = $modalId ?? 'deleteModal';
    $openFn = $openFn ?? 'openDeleteModal';
    $closeFn = $closeFn ?? 'closeDeleteModal';
    $formId = $modalId . 'Form';
    $titleTargetId = $modalId . 'ItemTitle';
    $inputId = $modalId . 'ConfirmInput';
    $submitId = $modalId . 'SubmitBtn';
@endphp

<div id="{{ $modalId }}" class="fixed inset-0 z-50 hidden items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-xs" onclick="{{ $closeFn }}()"></div>
    <div class="relative w-full max-w-md bg-white dark:bg-[#1E1E20] rounded-2xl p-6 sm:p-8 shadow-xl border border-[#E5E7EB] dark:border-[#2D2D30] z-10">
        <div class="flex items-center justify-between pb-4 border-b border-[#E5E7EB] dark:border-[#2D2D30] mb-5">
            <h2 class="text-lg font-semibold text-[#0F172A] dark:text-white">{{ $title }}</h2>
            <button type="button" onclick="{{ $closeFn }}()" class="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800 text-gray-500 hover:text-black dark:text-zinc-400 dark:hover:text-white transition-colors cursor-pointer">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>

        <form id="{{ $formId }}" method="POST" action="" class="space-y-4">
            @csrf
            @method('DELETE')
            <p class="text-sm text-[#0F172A] dark:text-[#D4D4D8] leading-relaxed">
                Are you sure you want to delete {{ $entityLabel }} <strong id="{{ $titleTargetId }}" class="text-black dark:text-white"></strong>? {{ __('admin.archive.delete_note') }}
            </p>

            <div>
                <label for="{{ $inputId }}" class="block text-xs font-semibold text-[#64748B] dark:text-[#A1A1AA] mb-1.5">
                    {{ __('admin.archive.type_delete_label') }}
                </label>
                <input id="{{ $inputId }}"
                       type="text"
                       autocomplete="off"
                       placeholder="delete"
                       class="w-full h-11 px-4 rounded-xl bg-[#F3F4F6] dark:bg-[#27272A] text-[#0F172A] dark:text-white text-sm font-mono outline-none border border-transparent focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20 transition-all">
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#E5E7EB] dark:border-[#2D2D30]">
                <button type="button" onclick="{{ $closeFn }}()" class="h-10 px-5 rounded-full text-gray-600 dark:text-zinc-400 hover:bg-gray-100 dark:hover:bg-zinc-800 font-medium text-sm transition-colors cursor-pointer">
                    {{ __('admin.common.cancel') }}
                </button>
                <button id="{{ $submitId }}" type="submit" disabled class="h-10 px-6 rounded-full bg-rose-600 disabled:opacity-40 disabled:cursor-not-allowed text-white font-semibold text-sm hover:bg-rose-700 transition-all cursor-pointer inline-flex items-center gap-2">
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
    (function () {
        const modal = document.getElementById(@json($modalId));
        const form = document.getElementById(@json($formId));
        const titleEl = document.getElementById(@json($titleTargetId));
        const input = document.getElementById(@json($inputId));
        const submitBtn = document.getElementById(@json($submitId));
        const prefix = @json(rtrim($actionPrefix, '/'));

        function syncButton() {
            if (input && submitBtn) {
                submitBtn.disabled = input.value.trim().toLowerCase() !== 'delete';
            }
        }

        input?.addEventListener('input', syncButton);

        window[@json($openFn)] = function (itemId, itemTitle) {
            if (!modal || !form) return;
            if (titleEl) titleEl.textContent = `"${itemTitle}"`;
            form.action = `${prefix}/${itemId}`;
            if (input) input.value = '';
            syncButton();
            modal.classList.remove('hidden');
            modal.classList.add('flex');
            setTimeout(() => input?.focus(), 50);
        };

        window[@json($closeFn)] = function () {
            if (!modal) return;
            modal.classList.add('hidden');
            modal.classList.remove('flex');
        };
    })();
</script>
@endpush
