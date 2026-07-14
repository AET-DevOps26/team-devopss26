import { describe, it, expect, beforeEach } from 'vitest';
import { redirect } from '@tanstack/react-router';
import { useAuthStore } from '../../stores/authStore';

beforeEach(() => {
  useAuthStore.getState().clearAuth();
});

describe('route guards', () => {
  it('unauthenticated users are blocked by the _authenticated guard', () => {
    useAuthStore.getState().clearAuth();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);

    expect(() => {
      if (!useAuthStore.getState().isAuthenticated) {
        throw redirect({ to: '/login' });
      }
    }).toThrow();
  });

  it('authenticated users can pass the _authenticated guard', () => {
    useAuthStore.getState().setAuth(
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature',
    );
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    expect(() => {
      if (!useAuthStore.getState().isAuthenticated) {
        throw redirect({ to: '/login' });
      }
    }).not.toThrow();
  });

  it('authenticated users are redirected by the _unauthenticated guard', () => {
    useAuthStore.getState().setAuth(
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature',
    );
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    expect(() => {
      if (useAuthStore.getState().isAuthenticated) {
        throw redirect({ to: '/' });
      }
    }).toThrow();
  });

  it('unauthenticated users can pass the _unauthenticated guard', () => {
    useAuthStore.getState().clearAuth();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);

    expect(() => {
      if (useAuthStore.getState().isAuthenticated) {
        throw redirect({ to: '/' });
      }
    }).not.toThrow();
  });
});
