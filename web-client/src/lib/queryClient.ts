import { QueryClient } from '@tanstack/react-query';

/** Global TanStack Query client.
 *
 * Queries: 30s staleTime, retry: 1, refetchOnWindowFocus: false.
 * Mutations: retry: 0 (retrying a mutation like duplicate POST could cause data issues).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
