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
