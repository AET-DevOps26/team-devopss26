import { describe, it, expect, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import { api } from '#/lib/api/client.ts';
import { useAuthStore } from '#/stores/authStore.ts';

beforeEach(() => {
  useAuthStore.getState().clearAuth();
});

describe('request interceptor', () => {
  it('attaches Authorization header when token is present', async () => {
    let capturedHeaders: Record<string, string> = {};

    server.use(
      http.get('*/api/v1/notes', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json([]);
      }),
    );

    useAuthStore.getState().setAuth('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature');
    await api.get('/api/v1/notes');

    expect(capturedHeaders.authorization).toBe('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature');
  });

  it('does not attach Authorization header when no token', async () => {
    let capturedHeaders: Record<string, string> = {};

    server.use(
      http.get('*/api/v1/notes', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json([]);
      }),
    );

    await api.get('/api/v1/notes');

    expect(capturedHeaders.authorization).toBeUndefined();
  });

  it('does not attach Authorization header when token is cleared', async () => {
    let capturedHeaders: Record<string, string> = {};

    server.use(
      http.get('*/api/v1/notes', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json([]);
      }),
    );

    useAuthStore.getState().setAuth('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature');
    useAuthStore.getState().clearAuth();
    await api.get('/api/v1/notes');

    expect(capturedHeaders.authorization).toBeUndefined();
  });
});

describe('401 response interceptor', () => {
  it('clears auth state on 401 for non-auth routes', async () => {
    useAuthStore.getState().setAuth('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature');

    server.use(
      http.get('*/api/v1/notes', () => HttpResponse.json(null, { status: 401 })),
    );

    await expect(api.get('/api/v1/notes')).rejects.toBeDefined();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().token).toBeNull();
  });

  it('does not clear auth state on 401 for auth routes', async () => {
    useAuthStore.getState().setAuth('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature');

    server.use(
      http.post('*/api/v1/users/auth/login', () => HttpResponse.json(null, { status: 401 })),
    );

    await expect(api.post('/api/v1/users/auth/login')).rejects.toBeDefined();
    // Auth state should still be intact since it's an auth route
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
  });
});
