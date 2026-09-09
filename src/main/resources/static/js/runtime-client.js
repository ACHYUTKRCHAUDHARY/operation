(() => {
  const nativeFetch = window.fetch.bind(window);
  const defaultApiBase = window.location.hostname.endsWith('.vercel.app')
    ? 'https://yardflow-zzow.onrender.com'
    : '';
  const apiBase = String(window.YARDFLOW_API_BASE || defaultApiBase).replace(/\/$/, '');
  const tokenKey = 'yardflow_access_token';

  window.yardFlowApiBase = apiBase;
  window.yardFlowClearSession = () => sessionStorage.removeItem(tokenKey);

  window.fetch = async (input, init = {}) => {
    const rawUrl = typeof input === 'string' ? input : input.url;
    const isBackendPath = rawUrl.startsWith('/api/') || rawUrl.startsWith('/actuator/');
    const url = isBackendPath && apiBase ? `${apiBase}${rawUrl}` : rawUrl;
    const headers = new Headers(init.headers || (typeof input !== 'string' ? input.headers : undefined));
    const token = sessionStorage.getItem(tokenKey);
    if (token && isBackendPath && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`);

    const response = await nativeFetch(url, {
      ...init,
      headers,
      credentials: apiBase && isBackendPath ? 'omit' : (init.credentials || 'same-origin')
    });

    if (rawUrl === '/api/auth/login' && response.ok) {
      try {
        const payload = await response.clone().json();
        if (payload?.token) sessionStorage.setItem(tokenKey, payload.token);
      } catch (_) {}
    }
    if (rawUrl === '/api/auth/logout') sessionStorage.removeItem(tokenKey);
    return response;
  };
})();