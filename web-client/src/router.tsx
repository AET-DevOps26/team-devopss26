import {createRouter as createTanStackRouter} from '@tanstack/react-router';
import {routeTree} from './routeTree.gen';
import {queryClient} from './lib/queryClient';

let router: ReturnType<typeof createTanStackRouter> | null = null;

export function getRouter() {
  if (!router) {
    router = createTanStackRouter({
      routeTree,
      basepath: import.meta.env.VITE_BASE_PATH || '/',
      context: {queryClient},
      scrollRestoration: true,
      defaultPreload: 'intent',
      defaultPreloadStaleTime: 0,
    });
  }
  return router;
}

declare module '@tanstack/react-router' {
  interface Register {
    router: ReturnType<typeof getRouter>;
  }
}
