import { createFileRoute, redirect } from '@tanstack/react-router';
import { useAuthStore } from 'src/stores/authStore';
import { AppShell } from 'src/components/layout/AppShell';

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: () => {
    const { isAuthenticated } = useAuthStore.getState();
    if (!isAuthenticated) {
      throw redirect({ to: '/login' });
    }
  },
  component: AuthenticatedLayout,
});

function AuthenticatedLayout() {
  return <AppShell />;
}
