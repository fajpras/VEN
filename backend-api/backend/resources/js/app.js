const PROGRESS_BAR_ID = 'ven-nav-progress';

let progressTimer;

function getProgressBar() {
    let bar = document.getElementById(PROGRESS_BAR_ID);

    if (!bar) {
        bar = document.createElement('div');
        bar.id = PROGRESS_BAR_ID;
        bar.className = 'ven-nav-progress';
        bar.setAttribute('aria-hidden', 'true');
        document.body.appendChild(bar);
    }

    return bar;
}

function startProgress() {
    const bar = getProgressBar();

    clearTimeout(progressTimer);

    bar.style.transition = 'none';
    bar.style.transform = 'scaleX(0)';
    bar.style.opacity = '1';
    void bar.offsetWidth;

    bar.style.transition = 'transform 900ms cubic-bezier(0.1, 0.7, 0.3, 1), opacity 200ms ease-out';
    bar.style.transform = 'scaleX(0.85)';

    progressTimer = window.setTimeout(() => {
        bar.style.opacity = '0';
    }, 6000);
}

function finishProgress() {
    const bar = document.getElementById(PROGRESS_BAR_ID);

    if (!bar) {
        return;
    }

    clearTimeout(progressTimer);
    bar.style.transition = 'transform 200ms ease-out, opacity 250ms ease-out';
    bar.style.transform = 'scaleX(1)';
    bar.style.opacity = '0';
}

function resetProgress() {
    const bar = document.getElementById(PROGRESS_BAR_ID);

    if (!bar) {
        return;
    }

    clearTimeout(progressTimer);
    bar.style.transition = 'none';
    bar.style.transform = 'scaleX(0)';
    bar.style.opacity = '0';
}

document.addEventListener('click', (event) => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
    }

    const link = event.target.closest('a[href]');

    if (!link || link.hasAttribute('download') || (link.target && link.target !== '_self')) {
        return;
    }

    const url = new URL(link.href, window.location.href);
    const isSamePage = url.origin === window.location.origin
        && url.pathname === window.location.pathname
        && url.search === window.location.search;

    if (url.origin !== window.location.origin || (isSamePage && url.hash)) {
        return;
    }

    startProgress();
});

document.addEventListener('submit', (event) => {
    if (!event.defaultPrevented) {
        startProgress();
    }
}, true);

window.addEventListener('pagehide', finishProgress);
window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        resetProgress();
    }
});

// Modern custom dropdown controller ([data-dropdown])
function closeAllDropdowns(except = null) {
    document.querySelectorAll('[data-dropdown]').forEach((container) => {
        if (container === except) return;
        const menu = container.querySelector('[data-dropdown-menu]');
        const trigger = container.querySelector('[data-dropdown-trigger]');
        const chevron = container.querySelector('[data-dropdown-chevron]');
        if (menu && !menu.classList.contains('hidden')) {
            menu.classList.add('hidden');
            trigger?.setAttribute('aria-expanded', 'false');
            chevron?.classList.remove('rotate-180');
        }
    });
}

document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-dropdown-trigger]');
    if (trigger) {
        const container = trigger.closest('[data-dropdown]');
        const menu = container?.querySelector('[data-dropdown-menu]');
        const chevron = container?.querySelector('[data-dropdown-chevron]');
        if (!menu) return;

        const willOpen = menu.classList.contains('hidden');
        closeAllDropdowns(container);
        menu.classList.toggle('hidden', !willOpen);
        trigger.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
        chevron?.classList.toggle('rotate-180', willOpen);
        return;
    }

    if (!event.target.closest('[data-dropdown]')) {
        closeAllDropdowns();
    }
});

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
        closeAllDropdowns();
    }
});

// Realtime debounced search for admin forms marked with [data-live-search]
let searchDebounceTimer;
let activeSearchController = null;

document.querySelectorAll('form[data-live-search]').forEach((form) => {
    const input = form.querySelector('input[name="cari"]');
    if (!input) return;

    input.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = window.setTimeout(async () => {
            const params = new URLSearchParams(new FormData(form));
            // Keep active view/per_page from current URL
            const currentParams = new URLSearchParams(window.location.search);
            ['view', 'per_page'].forEach((k) => {
                if (currentParams.has(k) && !params.has(k)) {
                    params.set(k, currentParams.get(k));
                }
            });
            params.delete('page');
            if (!params.get('cari')?.trim()) {
                params.delete('cari');
            }

            const targetUrl = form.action + (params.toString() ? `?${params.toString()}` : '');

            if (activeSearchController) {
                activeSearchController.abort();
            }
            activeSearchController = new AbortController();

            try {
                const res = await fetch(targetUrl, {
                    signal: activeSearchController.signal,
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                });
                if (!res.ok) return;

                const html = await res.text();
                const doc = new DOMParser().parseFromString(html, 'text/html');
                const nextRegion = doc.querySelector('[data-live-region]');
                const currRegion = document.querySelector('[data-live-region]');

                if (nextRegion && currRegion) {
                    currRegion.innerHTML = nextRegion.innerHTML;
                    window.history.replaceState({}, '', targetUrl);
                }
            } catch (err) {
                if (err.name !== 'AbortError') {
                    // Fallback gracefully on network error
                }
            }
        }, 280);
    });
});
