const CACHE_NAME = 'silbom-tech-shell-v2';
const APP_ENTRY = '/app/index.html';
const APP_SHELL_URLS = [
  APP_ENTRY, '/app/api-client.js', '/app/manifest.webmanifest',
  '/app/icons/icon-192.png', '/app/icons/icon-512.png', '/app/icons/apple-touch-icon.png'
];

self.addEventListener('install', event => {
  // Una actualización espera a que se cierren las vistas; no interrumpe una operación pendiente.
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL_URLS)));
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys
    .filter(key => key.startsWith('silbom-tech-shell-') && key !== CACHE_NAME)
    .map(key => caches.delete(key)))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', event => {
  const req = event.request;
  const url = new URL(req.url);
  // Ninguna API, credencial ni escritura se guarda en Cache Storage ni se reintenta.
  if (req.method !== 'GET' || req.headers.has('Authorization')
      || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;
  const isEntry = req.mode === 'navigate' && ['/app/', APP_ENTRY].includes(url.pathname);
  if (!isEntry && !APP_SHELL_URLS.includes(url.pathname)) return;
  const cacheKey = isEntry ? APP_ENTRY : url.pathname;
  event.respondWith(fetch(req).then(async res => {
    if (res.ok) {
      const cache = await caches.open(CACHE_NAME);
      await cache.put(cacheKey, res.clone());
    }
    return res;
  }).catch(() => caches.match(cacheKey)));
});
