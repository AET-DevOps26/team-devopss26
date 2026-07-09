import { type ReactElement, Suspense } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

interface RenderWithClientOptions extends Omit<RenderOptions, 'wrapper'> {
  suspenseFallback?: ReactElement;
}

export function renderWithClient(
  ui: ReactElement,
  options?: RenderWithClientOptions,
) {
  const { suspenseFallback, ...renderOptions } = options ?? {};
  const queryClient = createTestQueryClient();

  let content = (
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );

  if (suspenseFallback) {
    content = (
      <QueryClientProvider client={queryClient}>
        <Suspense fallback={suspenseFallback}>
          {ui}
        </Suspense>
      </QueryClientProvider>
    );
  }

  const { rerender, ...result } = render(content, renderOptions);

  return {
    ...result,
    queryClient,
    rerender: (rerenderUi: ReactElement) => {
      let newContent = (
        <QueryClientProvider client={queryClient}>
          {rerenderUi}
        </QueryClientProvider>
      );
      if (suspenseFallback) {
        newContent = (
          <QueryClientProvider client={queryClient}>
            <Suspense fallback={suspenseFallback}>
              {rerenderUi}
            </Suspense>
          </QueryClientProvider>
        );
      }
      rerender(newContent);
    },
  };
}
