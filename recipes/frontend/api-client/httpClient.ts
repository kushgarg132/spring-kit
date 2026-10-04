import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { clearSession, getAccessToken, readCsrfCookie, setSession } from "./auth";
import type { ApiErrorResponse, RefreshResponse } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly validationErrors: Record<string, string> | null;

  constructor(body: ApiErrorResponse) {
    super(body.message);
    this.name = "ApiError";
    this.status = body.status;
    this.validationErrors = body.validationErrors;
  }
}

const AUTH_PATHS = ["/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout"];

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

/** Called once, from main.tsx, so the app shell can react (e.g. redirect to /login). */
let onSessionExpired: (() => void) | null = null;
export function setOnSessionExpired(handler: () => void): void {
  onSessionExpired = handler;
}

// Multiple requests can 401 at once (e.g. the dashboard fires several queries in
// parallel) — share one in-flight refresh instead of racing several.
let refreshPromise: Promise<string> | null = null;

function isAuthPath(url: string | undefined): boolean {
  return Boolean(url && AUTH_PATHS.some((p) => url.includes(p)));
}

async function performRefresh(client: AxiosInstance): Promise<string> {
  const csrfToken = readCsrfCookie();
  const { data } = await client.post<RefreshResponse>(
    "/api/v1/auth/refresh",
    {},
    { headers: csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {} }
  );
  setSession(data.accessToken);
  return data.accessToken;
}

export function createHttpClient(baseURL: string): AxiosInstance {
  const client = axios.create({
    baseURL,
    withCredentials: true,
    headers: { "Content-Type": "application/json" }
  });

  client.interceptors.request.use((config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.set("Authorization", `Bearer ${token}`);
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ApiErrorResponse>) => {
      const config = error.config as RetryableConfig | undefined;

      const isUnauthorized = error.response?.status === 401;
      const canRetry = config && !config._retried && !isAuthPath(config.url);

      if (isUnauthorized && canRetry) {
        config._retried = true;
        try {
          refreshPromise ??= performRefresh(client).finally(() => {
            refreshPromise = null;
          });
          const newToken = await refreshPromise;
          config.headers.set("Authorization", `Bearer ${newToken}`);
          return client.request(config);
        } catch {
          clearSession();
          onSessionExpired?.();
        }
      }

      if (error.response?.data?.message) {
        return Promise.reject(new ApiError(error.response.data));
      }
      return Promise.reject(error);
    }
  );

  return client;
}
