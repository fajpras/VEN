{{--
    Baris kontrol daftar: menggabungkan view-toggle (Card/List) dan per-page-dropdown.
    Props:
      - $viewMode, $perPage, $perPageOptions
      - $showViewToggle (default true)
--}}
@php
    $showViewToggle = $showViewToggle ?? true;
@endphp

<div class="flex flex-col sm:flex-row sm:items-center {{ $showViewToggle ? 'sm:justify-between' : 'sm:justify-end' }} gap-4">
    @if ($showViewToggle)
        @include('admin.components.toolbar.view-toggle')
    @endif

    @include('admin.components.toolbar.per-page-dropdown', ['height' => 'h-11'])
</div>
