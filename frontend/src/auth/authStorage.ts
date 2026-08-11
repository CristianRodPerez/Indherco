import type { CurrentUser } from './authTypes';

const TOKEN_KEY = 'indherco_token';
const USER_KEY = 'indherco_user';

export function saveSession(token: string, user: CurrentUser) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): CurrentUser | null {
  const value = localStorage.getItem(USER_KEY);
  return value ? (JSON.parse(value) as CurrentUser) : null;
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
