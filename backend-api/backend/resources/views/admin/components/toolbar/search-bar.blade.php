{{--
    Komponen Search Bar realtime reusable (data-live-search).
    Props:
      - $action: URL tujuan form (mis. route('admin.accounts'))
      - $placeholder: teks placeholder (opsional, default __('admin.common.search'))
      - $hidden: associative array input hidden ['key' => 'val'] (nilai kosong otomatis diabaikan)
      - $id: id form opsional
--}}
@php
    $placeholder = $placeholder ?? __('admin.common.search');
    $hidden = $hidden ?? [];
@endphp

<form @if (! empty($id)) id="{{ $id }}" @endif
      method="GET"
      action="{{ $action }}"
      data-live-search
      class="flex-1 min-w-0">
    @foreach ($hidden as $hiddenName => $hiddenValue)
        @if ($hiddenValue !== null && $hiddenValue !== '')
            <input type="hidden" name="{{ $hiddenName }}" value="{{ $hiddenValue }}">
        @endif
    @endforeach
    <div class="relative flex items-center w-full">
        <svg class="w-5 h-5 absolute left-5 text-[#94A3B8] dark:text-[#71717A] pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
        <input type="text"
               name="cari"
               value="{{ request('cari') }}"
               placeholder="{{ $placeholder }}"
               class="w-full h-12 pl-13 pr-6 rounded-full bg-white dark:bg-[#27272A] text-[#0F172A] dark:text-white border border-[#E5E7EB] dark:border-[#27272A] placeholder-[#94A3B8] dark:placeholder-[#71717A] text-base font-normal outline-none focus:ring-2 focus:ring-[#BA18F5] transition-all">
    </div>
</form>
