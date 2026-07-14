import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';
import { useAuthStore } from 'src/stores/authStore';

export const Route = createFileRoute('/_unauthenticated')({
  beforeLoad: () => {
    const { isAuthenticated } = useAuthStore.getState();
    if (isAuthenticated) {
      throw redirect({ to: '/' });
    }
  },
  component: UnauthenticatedLayout,
});

function UnauthenticatedLayout() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-4">
      <Outlet />
    </div>
  );
}
