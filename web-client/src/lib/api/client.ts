import axios, { type AxiosRequestConfig, type AxiosInstance, type AxiosResponse } from 'axios';

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Response interceptor: redirect to /login on 401
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      window.location.href = '/login';
    }
    return Promise.reject(error instanceof Error ? error : new Error(String(error)));
  },
);

// Orval mutator: wraps the shared instance with the call signature Orval expects
export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
  return api({ ...config }).then((response: AxiosResponse<T>): T => {
    // No Content responses have no body — return undefined to match void return types
    if (response.status === 204) return undefined as T;
    if (response.data === '' || response.data === null) return undefined as T;
    return response.data;
  });
};
