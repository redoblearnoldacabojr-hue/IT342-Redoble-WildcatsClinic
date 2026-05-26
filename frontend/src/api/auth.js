import { apiFetch, buildApiUrl } from './http';

const TOKEN_KEY = "wc_token";
const USER_KEY = "wc_user";

async function requestJson(path, payload) {
  const response = await apiFetch(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const message = data?.message || data?.error || "Request failed";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

export function registerUser(payload) {
  return requestJson("/api/auth/register", payload);
}

export function loginUser(payload) {
  return requestJson("/api/auth/login", payload);
}

export function persistAuth(authResponse) {
  if (!authResponse?.token) {
    return;
  }

  const user = {
    userId: authResponse.userId,
    email: authResponse.email,
    firstName: authResponse.firstName,
    lastName: authResponse.lastName,
    provider: authResponse.provider,
    role: authResponse.role ?? 1,
  };

  localStorage.setItem(TOKEN_KEY, authResponse.token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export async function getCurrentUser() {
  const token = getStoredToken();
  if (!token) {
    const error = new Error("Not authenticated");
    error.status = 401;
    throw error;
  }

  const response = await apiFetch("/api/auth/me", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const message = data?.message || data?.error || "Failed to load user";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

export async function logoutUser() {
  const token = getStoredToken();
  if (!token) {
    return;
  }

  try {
    await apiFetch('/api/auth/logout', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  } catch {
    // Best effort: local auth is still cleared below.
  }
}

export function startGoogleLogin() {
  clearAuth();
  window.location.assign(buildApiUrl('/oauth2/authorization/google'));
}
