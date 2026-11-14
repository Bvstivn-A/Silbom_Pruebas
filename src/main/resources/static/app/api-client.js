/* Cliente compartido por panel, PWA y HTML Android. Nunca reintenta escrituras. */
(() => {
  'use strict';

  function serverOrigin(raw = window.location.origin) {
    if (!raw || raw === 'null') return '';
    const url = new URL(raw);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password
        || url.search || url.hash || !['/', '/api/v1', '/api/v1/'].includes(url.pathname)) {
      throw new Error('Usa el origen HTTPS del servidor, sin credenciales ni parámetros.');
    }
    return url.origin;
  }

  function baseUrl(origin = window.location.origin) {
    const selected = serverOrigin(origin);
    if (!selected) throw new Error('Configura el servidor HTTPS antes de conectar.');
    return selected === window.location.origin ? '/api/v1' : `${selected}/api/v1`;
  }

  function allowsCredentials(origin) {
    const url = new URL(serverOrigin(origin));
    return url.protocol === 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
  }

  async function request(endpoint, { origin = window.location.origin, timeoutMs = 15000, ...options } = {}) {
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');
    headers.set('X-Requested-With', 'XMLHttpRequest');
    if (headers.has('Authorization') && !allowsCredentials(origin)) {
      throw new Error('Inicia sesión mediante HTTPS. HTTP solo está permitido en localhost.');
    }
    if (options.body != null && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(`${baseUrl(origin)}${endpoint}`, {
        ...options, headers, signal: controller.signal, credentials: 'omit',
        cache: 'no-store', redirect: 'error'
      });
      const text = await response.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch (err) {
        if (response.ok) throw new Error('Respuesta del servidor inválida; operación sin confirmar.');
      }
      if (response.ok && response.status !== 204 && data === null) {
        throw new Error('Respuesta incompleta; operación sin confirmar.');
      }
      return { ok: response.ok, status: response.status, data,
        retryAfter: Number(response.headers.get('Retry-After')) || 10 };
    } catch (err) {
      err.isCommunicationError = true;
      err.isNetworkError = err instanceof TypeError || err.name === 'AbortError';
      throw err;
    } finally {
      clearTimeout(timer);
    }
  }

  function operationId() {
    // getRandomValues funciona también al revisar el catálogo por HTTP local.
    return '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, c =>
      (Number(c) ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> Number(c) / 4).toString(16));
  }

  window.SilbomHttp = { serverOrigin, baseUrl, allowsCredentials, request, operationId };
})();
