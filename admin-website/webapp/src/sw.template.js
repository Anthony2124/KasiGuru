/*
 * KasiGuru service worker. Emitted at build time by the plugin in vite.config.ts, which fills in
 * the precache list and the build version.
 *
 * - The app shell and every built asset are precached, so the app opens offline once it has been
 *   opened online once (and an installed iPhone app starts without a network).
 * - Navigations are network-first with the cached shell as the fallback.
 * - The content snapshot is network-first, so a redeploy with new words reaches learners promptly.
 * - Art, fonts and icons are cache-first: they change only with a release.
 * - Firestore and Google sign-in are never intercepted.
 */
const VERSION = '__VERSION__';
const SHELL = `kasiguru-shell-${VERSION}`;
const RUNTIME = 'kasiguru-runtime-v1';
const PRECACHE = __PRECACHE__;

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(SHELL)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith('kasiguru-shell-') && k !== SHELL).map((k) => caches.delete(k))))
      // Earlier workers cached Firebase's auth handler here as the shell; drop that copy so the
      // precached shell is the offline fallback again.
      .then(() => caches.open(RUNTIME))
      .then((cache) => cache.delete('/index.html'))
      .then(() => self.clients.claim())
  );
});

async function networkFirst(request, fallbackUrl) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(RUNTIME);
      cache.put(fallbackUrl || request, response.clone());
    }
    return response;
  } catch (e) {
    const cached = (await caches.match(fallbackUrl || request)) || (await caches.match(request));
    if (cached) return cached;
    throw e;
  }
}

async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  const response = await fetch(request);
  if (response.ok) {
    const cache = await caches.open(RUNTIME);
    cache.put(request, response.clone());
  }
  return response;
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/__word-audio/')) return;
  // Firebase's sign-in pages, proxied to this origin (vercel.json). Left alone, the navigation
  // branch below would store the auth handler as the offline shell.
  if (url.pathname.startsWith('/__/')) return;

  if (request.mode === 'navigate') {
    event.respondWith(networkFirst(request, '/index.html'));
    return;
  }
  if (url.pathname.startsWith('/content/')) {
    event.respondWith(networkFirst(request));
    return;
  }
  if (/^\/(assets|fonts|img|icons)\//.test(url.pathname)) {
    event.respondWith(cacheFirst(request));
  }
});
