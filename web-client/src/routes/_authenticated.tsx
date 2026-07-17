import { createFileRoute, redirect } from '@tanstack/react-router';
import { useAuthStore } from 'src/stores/authStore';
import { AppShell } from 'src/components/layout/AppShell';

/**
 * Auth-gated layout. BeforeLoad reads auth store synchronously and throws
 * `redirect` to `/login` when unauthenticated (uses `throw redirect()`,
 * not `return redirect()`, for navigation abort). Check fires on every
 * navigation within the tree, not just initial page load.
 *
 * @throws {Redirect} Always redirects to `/login` when unauthenticated.
 */
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: () => {
    const { isAuthenticated } = useAuthStore.getState();
    if (!isAuthenticated) {
      throw redirect({ to: '/login' });
    }
  },
  component: AuthenticatedLayout,
});

/**
 * Delegates to AppShell for persistent navigation chrome and child route outlet.
 */
function AuthenticatedLayout() {
  return <AppShell />;
}
