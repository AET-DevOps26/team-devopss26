import {createRouter as createTanStackRouter} from '@tanstack/react-router';
import {routeTree} from './routeTree.gen';
import {queryClient} from './lib/queryClient';

/**
 * Lazily-initialised singleton router. Cached in module scope so `getRouter()`
 * always returns the same instance after the first call.
 */
let router: ReturnType<typeof createTanStackRouter> | null = null;

/**
 * Creates (once) and returns the TanStack Router instance.
 *
 * Configuration: auto-generated routeTree, basepath from VITE_BASE_PATH,
 * context passes queryClient (DI), scrollRestoration enabled,
 * defaultPreload='intent', defaultPreloadStaleTime=0.
 *
 * Type safety: Register module augmentation types all hooks with this
 * specific route tree and context.
 *
 * @returns The singleton router instance.
 */
export function getRouter() {
  router ??= createTanStackRouter({
    routeTree,
    basepath: import.meta.env.VITE_BASE_PATH ?? '/',
    context: {queryClient},
      scrollRestoration: true,
      defaultPreload: 'intent',
      defaultPreloadStaleTime: 0,
    });

  return router;
}

declare module '@tanstack/react-router' {
  interface Register {
    router: ReturnType<typeof getRouter>;
  }
}
