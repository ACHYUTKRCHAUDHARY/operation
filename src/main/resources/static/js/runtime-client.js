(() => {
  const nativeFetch = window.fetch.bind(window);
  const defaultApiBase = window.location.hostname.endsWith('.vercel.app')
    ? 'https://yardflow-zzow.onrender.com'
    : '';
  const apiBase = String(window.YARDFLOW_API_BASE || defaultApiBase).replace(/\/$/, '');
  const tokenKey = 'yardflow_access_token';
  const cacheKey = 'yardflow-dashboard-cache-v2';

  const decodeToken = (token) => {
    if (!token) return null;
    try {
      const part = token.split('.')[1];
      const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(atob(normalized).split('').map(c => `%${(`00${c.charCodeAt(0).toString(16)}`).slice(-2)}`).join(''));
      const payload = JSON.parse(json);
      if (payload.exp && payload.exp * 1000 <= Date.now()) return null;
      return payload;
    } catch (_) {
      return null;
    }
  };

  const clearSession = () => {
    sessionStorage.removeItem(tokenKey);
    sessionStorage.removeItem(cacheKey);
  };

  const session = () => {
    const token = sessionStorage.getItem(tokenKey);
    const claims = decodeToken(token);
    if (!claims && token) clearSession();
    return claims ? {
      token,
      email: claims.sub || '',
      fullName: claims.name || '',
      role: claims.role || ''
    } : null;
  };

  const homeForRole = (role) => role === 'CUSTOMER' ? '/customer.html' : '/';

  window.yardFlowApiBase = apiBase;
  window.yardFlowClearSession = clearSession;
  window.yardFlowSession = session;
  window.yardFlowRole = () => session()?.role || '';
  window.yardFlowHomeForRole = homeForRole;

  const current = session();
  const path = window.location.pathname;
  const internalHome = path === '/' || path === '/index.html';
  if (current?.role === 'CUSTOMER' && internalHome) {
    window.stop();
    window.location.replace('/customer.html');
    return;
  }
  if (current?.role && current.role !== 'CUSTOMER' && path === '/customer.html') {
    window.stop();
    window.location.replace('/');
    return;
  }

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
    if (rawUrl === '/api/auth/logout' || (isBackendPath && response.status === 401)) clearSession();
    return response;
  };
})();