import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '../../stores/authStore';

// Reset store between tests
beforeEach(() => {
  useAuthStore.getState().clearAuth();
  localStorage.removeItem('auth-storage');
});

describe('auth store', () => {
  it('initializes with empty state', () => {
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.userId).toBeNull();
    expect(state.username).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('setAuth stores token, userId, and username decoded from JWT', () => {
    // JWT with sub=42 and name=testuser
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature';
    useAuthStore.getState().setAuth(token);

    const state = useAuthStore.getState();
    expect(state.token).toBe(token);
    expect(state.userId).toBe(42);
    expect(state.username).toBe('testuser');
    expect(state.isAuthenticated).toBe(true);
  });

  it('clearAuth clears all state', () => {
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature';
    useAuthStore.getState().setAuth(token);
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    useAuthStore.getState().clearAuth();

    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.userId).toBeNull();
    expect(state.username).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('persists token to localStorage', () => {
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature';
    useAuthStore.getState().setAuth(token);

    const stored = JSON.parse(localStorage.getItem('auth-storage') ?? '{}');
    expect(stored.state.token).toBe(token);
    expect(stored.state.userId).toBe(42);
    expect(stored.state.username).toBe('testuser');
  });

  it('removes token from localStorage on clearAuth', () => {
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsIm5hbWUiOiJ0ZXN0dXNlciJ9.fake-signature';
    useAuthStore.getState().setAuth(token);
    useAuthStore.getState().clearAuth();

    const stored = JSON.parse(localStorage.getItem('auth-storage') ?? '{}');
    expect(stored.state.token).toBeNull();
    expect(stored.state.userId).toBeNull();
    expect(stored.state.username).toBeNull();
  });

  it('isAuthenticated returns false when token is null', () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });
});
