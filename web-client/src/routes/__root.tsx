import { createRootRouteWithContext, Outlet, Link } from '@tanstack/react-router';
import { TanStackRouterDevtoolsPanel } from '@tanstack/react-router-devtools';
import { TanStackDevtools } from '@tanstack/react-devtools';
import { HomeIcon } from 'lucide-react';
import type { QueryClient } from '@tanstack/react-query';

import 'src/styles.css';
import { buttonVariants } from 'src/components/ui/button';
import { cn } from 'src/lib/utils';

interface RouterContext {
  queryClient: QueryClient;
}

/**
 * Root application shell. Wraps pages in Outlet with TanStack Devtools.
 * 404 handling via built‑in `notFoundComponent`. Exposes QueryClient
 * to child routes via `createRootRouteWithContext`.
 */
export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootComponent,
  notFoundComponent: NotFoundPage,
});

/**
 * Top-level Outlet with TanStack Devtools at bottom-right.
 */
function RootComponent() {
  return (
    <>
      <Outlet />
      <TanStackDevtools
        config={{
          position: 'bottom-right',
        }}
        plugins={[
          {
            name: 'TanStack Router',
            render: <TanStackRouterDevtoolsPanel />,
          },
        ]}
      />
    </>
  );
}

/**
 * Catch-all 404 page. Also handles cases where `beforeLoad` redirect does
 * not apply and the URL is genuinely unknown.
 */
function NotFoundPage() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
      <h1 className="text-4xl font-bold tracking-tight">Page not found</h1>
      <p className="max-w-sm text-muted-foreground">
        The page you are looking for does not exist or has been moved.
      </p>
      <Link
        to="/"
        className={cn(buttonVariants({ variant: 'default' }))}
      >
        <HomeIcon data-icon="inline-start" />
        Go Home
      </Link>
    </div>
  );
}
