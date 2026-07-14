import axios, { type AxiosRequestConfig, type AxiosInstance, type AxiosResponse } from 'axios';
import { useAuthStore } from 'src/stores/authStore';

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach Bearer token from auth store
api.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: clear auth state and redirect to /login on 401 (but not for auth endpoints)
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

// Orval mutator: wraps the shared instance with the call signature Orval expects.
// The second `options` parameter enables per-request overrides (e.g., cancellation signals).
export const customInstance = <T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> => {
  return api({ ...config, ...options }).then((response: AxiosResponse<T>): T => {
    // No Content responses have no body — return undefined to match void return types
    if (response.status === 204) return undefined as T;
    if (response.data === '' || response.data === null) return undefined as T;
    return response.data;
  });
};
