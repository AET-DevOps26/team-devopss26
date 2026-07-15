import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';
import { useAuthStore } from 'src/stores/authStore';

/**
 * Guest-only layout. Redirects authenticated users to `/` via `throw redirect()`,
 * aborting child route rendering to avoid flash of guest UI.
 *
 * @throws {Redirect} Always redirects to `/` when already authenticated.
 */
export const Route = createFileRoute('/_unauthenticated')({
  beforeLoad: () => {
    const { isAuthenticated } = useAuthStore.getState();
    if (isAuthenticated) {
      throw redirect({ to: '/' });
    }
  },
  component: UnauthenticatedLayout,
});

/**
 * Centered flex container with Outlet for guest-facing pages.
 */
function UnauthenticatedLayout() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-4">
      <Outlet />
    </div>
  );
}
