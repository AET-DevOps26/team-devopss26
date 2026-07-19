import axios, { type AxiosRequestConfig, type AxiosInstance, type AxiosResponse } from 'axios';
import { useAuthStore } from 'src/stores/authStore';

/** Shared Axios instance. baseURL from VITE_API_URL env var. 30s timeout default. */
export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Request interceptor: inject Bearer token from auth store. Uses direct store
 * access (not a hook) — safe outside React components. Skips header if no token.
 */
api.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Global 401 handler: skips auth routes (prevents redirect loops), clears auth
 * and hard-redirects to `/login` for others. Hard navigation avoids render errors
 * from stale router state.
 */
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const requestUrl = error.config?.url ?? '';
      const isAuthRoute = requestUrl.includes('/users/auth/');
      if (!isAuthRoute) {
        useAuthStore.getState().clearAuth();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error instanceof Error ? error : new Error(String(error)));
  },
);

/** Orval-compatible request wrapper. Second `options` param enables per-request
 * overrides (e.g., `signal` for cancellation).
 *
 * Edge cases: 204 → `undefined`, empty body → `undefined`.
 *
 * @param config - Axios request config
 * @param options - Optional overrides merged into config
 * @returns Typed response body, or `undefined` for empty responses
 */
export const customInstance = <T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> => {
  return api({ ...config, ...options }).then((response: AxiosResponse<T>): T => {
    // No Content responses have no body — return undefined to match void return types
    if (response.status === 204) return undefined as T;
    if (response.data === '' || response.data === null) return undefined as T;
    return response.data;
  });
};
