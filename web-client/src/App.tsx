import { useEffect } from 'react';
import { RouterProvider } from '@tanstack/react-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { getRouter } from './router';
import { queryClient } from './lib/queryClient';
import { Toaster } from '#/components';
import { useAuthStore } from './stores/authStore';

const router = getRouter();

function App() {
  const validateToken = useAuthStore((s) => s.validateToken);

  useEffect(() => {
    validateToken();
  }, [validateToken]);

  return (
    <QueryClientProvider client={queryClient}>
      <Toaster />
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}

export default App;
