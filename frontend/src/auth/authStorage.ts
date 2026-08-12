import type { CurrentUser } from './authTypes';

const TOKEN_KEY = 'indherco_token';
const USER_KEY = 'indherco_user';

export function saveSession(token: string, user: CurrentUser, keepSignedIn: boolean) {
  clearSession();
  const storage = keepSignedIn ? localStorage : sessionStorage;
  storage.setItem(TOKEN_KEY, token);
  storage.setItem(USER_KEY, JSON.stringify(user));
}

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) ?? localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): CurrentUser | null {
  const value = sessionStorage.getItem(USER_KEY) ?? localStorage.getItem(USER_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value) as CurrentUser;
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}
