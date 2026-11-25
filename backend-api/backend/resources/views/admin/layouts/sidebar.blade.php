<aside class="w-20 min-h-screen bg-white dark:bg-[#121212] border-r border-[#E5E7EB] dark:border-[#27272A] flex flex-col items-center py-6 fixed left-0 top-0 z-30 transition-colors">
    {{-- Brand Logo --}}
    <div class="mb-10 flex items-center justify-center">
        <a href="{{ route('admin.accounts') }}" aria-label="VEN Home">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 61 24" fill="none" class="w-10 h-10 hover:scale-105 transition-transform" aria-hidden="true">
                <path fill="#BA18F5" d="M44.4 6.8h5.74V4.86H41.6v13.82h8.54V16.7H44.4v-3.58h5.74v-2.19H44.4V6.81Z" />
                <path fill="#BA18F5" d="M40.52 18.67H37.9L32.51 4.89h3.17l2.22 6.37v-6.4h2.62v13.8Z" />
                <path fill="#BA18F5" d="M53.76 4.85l1.75 8.78L58 4.85h2.33l-3.98 13.82h-1.58l-1.02-4.35v4.35h-2.54V4.85h2.54Z" />
                <path fill="#BA18F5" d="M6.37 15.15C3.5 14.36 0 11 0 11l4.62-4.7S6 9.05 8.34 10.34c3.4 1.87 10.04-0.75 12.73-1.96 0.7-0.31 1.54-0.18 2.08 0.36l1.86 1.85c0.23 0.23 0.23 0.6 0 0.84L13.2 23.26c-0.36 0.35-0.93 0.35-1.27 0l-3.6-3.67 3.92-3.78c0.21-0.2 0.04-0.57-0.25-0.53-1.36 0.15-3.9 0.34-5.64-0.13Z" />
                <path fill="#BA18F5" d="M8.97 3.71c0 2.05 1.66 3.72 3.71 3.72S16.4 5.76 16.4 3.7C16.4 1.66 14.73 0 12.7 0c-2.06 0-3.72 1.66-3.72 3.71Z" />
            </svg>
        </a>
    </div>

    {{-- Navigation items --}}
    <nav class="flex flex-col gap-6 w-full items-center">
        {{-- 1. Account --}}
        @php $isAccount = request()->routeIs('admin.accounts*'); @endphp
        <a href="{{ route('admin.accounts') }}"
           title="Account"
           @if ($isAccount) aria-current="page" @endif
           class="w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-200 {{ $isAccount ? 'bg-[#BA18F5] text-white' : 'text-[#64748B] dark:text-[#A1A1AA] hover:bg-gray-100 dark:hover:bg-zinc-800 hover:text-black dark:hover:text-white' }}">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 30" fill="currentColor" class="w-6 h-6" aria-hidden="true">
                <path fill-rule="evenodd" d="M0 15C0 6.73 6.73 0 15 0s15 6.73 15 15-6.73 15-15 15S0 23.27 0 15Zm8.45 10l-0.21 0.79c2.03 1.28 4.37 1.96 6.77 1.96 2.4 0 4.74-0.68 6.76-1.96l-0.2-0.8c-0.2-0.71-0.63-1.34-1.22-1.8-0.58-0.45-1.3-0.7-2.05-0.7h-6.6c-1.52 0-2.86 1.04-3.25 2.5Zm9.85-4.76c2.5 0 4.7 1.67 5.4 4.06 1.27-1.18 2.3-2.62 2.99-4.22 0.7-1.6 1.06-3.33 1.06-5.08 0-7.03-5.72-12.75-12.75-12.75S2.25 7.97 2.26 14.99c0 1.75 0.36 3.48 1.06 5.08 0.7 1.6 1.72 3.04 3 4.23 0.34-1.17 1.04-2.2 2.02-2.93 0.97-0.73 2.15-1.12 3.37-1.13h6.59ZM8.25 11.25c0-3.72 3.03-6.75 6.75-6.75s6.75 3.03 6.75 6.75S18.72 18 15 18s-6.75-3.03-6.75-6.75Zm2.25 0c0 2.48 2.02 4.5 4.5 4.5s4.5-2.02 4.5-4.5-2.02-4.5-4.5-4.5-4.5 2.02-4.5 4.5Z" />
            </svg>
        </a>

        {{-- 2. Post --}}
        @php $isPost = request()->routeIs('admin.posts*'); @endphp
        <a href="{{ route('admin.posts') }}"
           title="Post"
           @if ($isPost) aria-current="page" @endif
           class="w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-200 {{ $isPost ? 'bg-[#BA18F5] text-white' : 'text-[#64748B] dark:text-[#A1A1AA] hover:bg-gray-100 dark:hover:bg-zinc-800 hover:text-black dark:hover:text-white' }}">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 25 25" fill="currentColor" class="w-6 h-6" aria-hidden="true">
                <path d="M24.53 2.73C24.53 1.23 23.3 0 21.8 0H2.73C1.23 0 0 1.23 0 2.73V21.8c0 1.5 1.23 2.73 2.73 2.73H21.8c1.5 0 2.73-1.23 2.73-2.73V2.73ZM4.09 19.08l4.09-5.45 1.36 1.36 4.09-5.45 6.81 9.54H4.09Z" />
            </svg>
        </a>

        {{-- 3. Exhibition --}}
        @php $isExhibition = request()->routeIs('admin.exhibitions*'); @endphp
        <a href="{{ route('admin.exhibitions') }}"
           title="Exhibition"
           @if ($isExhibition) aria-current="page" @endif
           class="w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-200 {{ $isExhibition ? 'bg-[#BA18F5] text-white' : 'text-[#64748B] dark:text-[#A1A1AA] hover:bg-gray-100 dark:hover:bg-zinc-800 hover:text-black dark:hover:text-white' }}">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 25 24" fill="currentColor" class="w-6 h-6" aria-hidden="true">
                <path d="M10.82 0.27c0.91-0.36 1.93-0.36 2.84 0l9.19 3.68c0.48 0.2 0.9 0.52 1.19 0.96 0.29 0.43 0.44 0.93 0.44 1.45v10.68c0 0.52-0.15 1.03-0.44 1.46-0.3 0.43-0.7 0.76-1.2 0.95l-9.18 3.68c-0.9 0.36-1.93 0.36-2.84 0l-9.18-3.68c-0.49-0.19-0.9-0.52-1.2-0.95C0.17 18.07 0 17.56 0 17.04V6.36C0 5.84 0.16 5.33 0.45 4.9c0.29-0.43 0.7-0.76 1.19-0.95l9.18-3.68Zm-4.54 6.3c-0.19-0.09-0.4-0.1-0.59-0.02-0.19 0.07-0.34 0.22-0.42 0.4-0.08 0.2-0.09 0.4-0.01 0.6 0.07 0.18 0.21 0.33 0.4 0.41l5.82 2.56v5.62c0 0.2 0.08 0.4 0.22 0.54s0.34 0.22 0.54 0.22 0.4-0.08 0.54-0.22c0.15-0.14 0.23-0.34 0.23-0.54v-5.62l5.81-2.56c0.1-0.04 0.18-0.1 0.25-0.17s0.12-0.16 0.16-0.25c0.04-0.1 0.05-0.2 0.05-0.3 0-0.1-0.02-0.2-0.06-0.28-0.04-0.1-0.1-0.18-0.17-0.25-0.08-0.07-0.16-0.12-0.26-0.16C18.7 6.51 18.6 6.5 18.5 6.5c-0.1 0-0.2 0.02-0.3 0.06l-5.96 2.62-5.96-2.62Z" />
            </svg>
        </a>

        {{-- 4. Activity log --}}
        @php $isLog = request()->routeIs('admin.logs*'); @endphp
        <a href="{{ route('admin.logs') }}"
           title="Activity log"
           @if ($isLog) aria-current="page" @endif
           class="w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-200 {{ $isLog ? 'bg-[#BA18F5] text-white' : 'text-[#64748B] dark:text-[#A1A1AA] hover:bg-gray-100 dark:hover:bg-zinc-800 hover:text-black dark:hover:text-white' }}">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 30" fill="currentColor" class="w-6 h-6" aria-hidden="true">
                <path d="M28.86 9.16C28.48 8.28 28 7.42 27.47 6.61c-0.54-0.8-1.15-1.55-1.83-2.22-0.67-0.68-1.42-1.3-2.22-1.83-0.81-0.54-1.67-1.01-2.55-1.39C19.97 0.78 19 0.5 18.05 0.3c-1.97-0.4-4.09-0.4-6.05 0-0.97 0.2-1.91 0.5-2.83 0.87C8.3 1.55 7.43 2.02 6.62 2.56 5.82 3.1 5.07 3.7 4.4 4.39 4.14 4.64 3.92 4.9 3.68 5.17L0 1.49v9h9L5.82 7.3c0.23-0.27 0.47-0.54 0.72-0.78 0.54-0.54 1.14-1.04 1.77-1.47 0.65-0.44 1.34-0.81 2.04-1.11 0.72-0.3 1.49-0.54 2.26-0.7 1.57-0.32 3.27-0.32 4.85 0 0.76 0.16 1.53 0.38 2.25 0.7 0.7 0.3 1.4 0.67 2.04 1.1 0.63 0.44 1.23 0.94 1.77 1.48s1.04 1.14 1.47 1.77c0.44 0.65 0.81 1.34 1.11 2.04 0.3 0.72 0.54 1.49 0.7 2.25 0.17 0.8 0.25 1.61 0.25 2.42 0 1.6-0.32 3.2-0.95 4.67-0.3 0.7-0.67 1.4-1.1 2.04-0.44 0.63-0.94 1.23-1.48 1.77s-1.14 1.04-1.77 1.48c-0.65 0.43-1.34 0.8-2.04 1.1-0.72 0.3-1.49 0.55-2.25 0.71-1.58 0.32-3.28 0.32-4.85 0-0.78-0.16-1.53-0.4-2.26-0.7-0.7-0.3-1.4-0.68-2.04-1.11-0.63-0.44-1.23-0.94-1.77-1.48s-1.04-1.14-1.47-1.77c-0.44-0.64-0.81-1.33-1.11-2.04-0.3-0.72-0.54-1.49-0.7-2.25C3.07 16.62 3 15.82 3 15H0c0 1.02 0.1 2.03 0.3 3.02 0.2 0.96 0.5 1.9 0.87 2.82 0.38 0.89 0.84 1.74 1.38 2.55 0.54 0.8 1.16 1.55 1.83 2.23 0.68 0.67 1.43 1.29 2.23 1.83s1.66 1 2.55 1.38c0.9 0.39 1.86 0.67 2.82 0.87 1 0.2 2 0.3 3.02 0.3s2.04-0.1 3.02-0.3c0.96-0.2 1.9-0.5 2.82-0.87 0.88-0.38 1.74-0.84 2.55-1.38 0.8-0.54 1.55-1.16 2.22-1.83 0.68-0.68 1.3-1.43 1.83-2.23 0.54-0.8 1.01-1.66 1.39-2.55 0.39-0.9 0.67-1.86 0.87-2.82 0.2-1 0.3-2.01 0.3-3.02 0-1-0.1-2.02-0.3-3.02-0.2-0.96-0.5-1.9-0.87-2.82h0.03Z" />
                <path d="M13.52 7.5v9h9.01v-3h-6v-6h-3Z" />
            </svg>
        </a>
    </nav>
</aside>
