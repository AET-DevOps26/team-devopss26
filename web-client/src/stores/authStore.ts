import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { jwtDecode } from 'jwt-decode';
import { checkToken } from '../services/users/user-authentication/user-authentication';

interface AuthState {
  token: string | null;
  userId: number | null;
  username: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string) => void;
  clearAuth: () => void;
  validateToken: () => Promise<void>;
}

interface JwtPayload {
  sub?: string;
  name?: string;
  [key: string]: unknown;
}

/** Zustand store: authentication state backed by localStorage persistence.
 * Persisted: `token`, `userId`, `username`. `isAuthenticated` is re-derived on rehydrate.
 * Actions: setAuth (decode JWT), clearAuth (logout), validateToken (server-side check).
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      userId: null,
      username: null,
      isAuthenticated: false,

      /** Decode JWT and populate auth state from its claims. No signature verification
       * client-side — that is the server's responsibility.
       *
       * @param token - Raw JWT string from the login endpoint
       */
      setAuth: (token: string) => {
        const decoded = jwtDecode<JwtPayload>(token);
        const userId = decoded.sub ? Number(decoded.sub) : null;
        const username = decoded.name ?? null;
        set({ token, userId, username, isAuthenticated: true });
      },

      /** Clear all auth state (logout). Safe to call multiple times. */
      clearAuth: () => {
        set({ token: null, userId: null, username: null, isAuthenticated: false });
      },

      /** Verify current token with the server. Auto-clears auth on non-2xx.
       * No-ops if no token is set.
       */
      validateToken: async () => {
        const { token } = get();
        if (!token) return;
        try {
          await checkToken();
        } catch {
          get().clearAuth();
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        token: state.token,
        userId: state.userId,
        username: state.username,
      }),
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.isAuthenticated = !!state.token;
        }
      },
    },
  ),
);
