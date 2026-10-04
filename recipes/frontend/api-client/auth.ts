import type { AuthenticatedAdminResponse } from "./types";

// The access token lives in memory only — never localStorage, never a
// JS-readable cookie — so it isn't a standing target for XSS token theft.
// It's lost on a full page reload by design; main.tsx re-derives it by
// calling /auth/refresh once at startup (the refresh token is in an
// httpOnly cookie the page can't read but the browser still sends).
let accessToken: string | null = null;
let currentAdmin: AuthenticatedAdminResponse | null = null;

type SessionListener = (admin: AuthenticatedAdminResponse | null) => void;
const listeners = new Set<SessionListener>();

interface DecodedAccessToken {
  sub: string;
  username: string;
  fullName: string;
  role: "SUPER_ADMIN" | "ADMIN";
}

/**
 * The access token is a signed JWT, not a secret container — decoding it
 * client-side (without verifying the signature) is safe for display purposes
 * only, because every real authorization decision happens server-side against
 * the verified token. This lets /auth/refresh (which returns just a token, no
 * admin object) still restore "who's logged in" after a page reload.
 */
function decodeAccessToken(token: string): AuthenticatedAdminResponse {
  const payloadSegment = token.split(".")[1];
  if (!payloadSegment) {
    throw new Error("Malformed access token");
  }
  const base64 = payloadSegment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
  const decoded = JSON.parse(new TextDecoder("utf-8").decode(bytes)) as DecodedAccessToken;
  return { id: decoded.sub, username: decoded.username, fullName: decoded.fullName, role: decoded.role };
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function setSession(token: string): AuthenticatedAdminResponse {
  accessToken = token;
  currentAdmin = decodeAccessToken(token);
  listeners.forEach((l) => l(currentAdmin));
  return currentAdmin;
}

export function clearSession(): void {
  accessToken = null;
  currentAdmin = null;
  listeners.forEach((l) => l(null));
}

export function getCurrentAdmin(): AuthenticatedAdminResponse | null {
  return currentAdmin;
}

export function onSessionChange(listener: SessionListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function readCsrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}
