const DEFAULT_API_BASE_URL = (import.meta.env.VITE_API_URL || '').trim().replace(/\/$/, '');

function joinUrl(baseUrl, path) {
  if (!path) {
    return baseUrl || '';
  }

  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  if (!baseUrl) {
    return path.startsWith('/') ? path : `/${path}`;
  }

  return `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
}

export function getApiBaseUrl() {
  return DEFAULT_API_BASE_URL;
}

export function buildApiUrl(path) {
  return joinUrl(DEFAULT_API_BASE_URL, path);
}

export function apiFetch(path, options) {
  return fetch(buildApiUrl(path), options);
}