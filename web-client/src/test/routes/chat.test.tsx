import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { type ReactElement } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ChatPage } from '#/routes/_authenticated/chat';
import { createTestQueryClient } from '../test-utils';

// useRouter and useSearch require RouterProvider context; mock for isolated tests
const mockNavigate = vi.fn();
vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useRouter: () => ({ navigate: mockNavigate }),
    useSearch: () => ({}),
  };
});

beforeEach(() => {
  mockNavigate.mockReset();
  localStorage.clear();
  sessionStorage.clear();
});

function renderChatPage(queryClient: QueryClient, ui?: ReactElement) {
  return render(
    <QueryClientProvider client={queryClient}>
      {ui ?? <ChatPage />}
    </QueryClientProvider>,
  );
}

describe('chat page — welcome state', () => {
  it('renders welcome heading and suggestion chips', async () => {
    const qc = createTestQueryClient();
    renderChatPage(qc);

    expect(screen.getByText('How can I help you?')).toBeInTheDocument();
    expect(screen.getByText('What tasks are due today?')).toBeInTheDocument();
    expect(screen.getByText('Summarize my recent notes')).toBeInTheDocument();
    expect(screen.getByText('Help me plan this sprint')).toBeInTheDocument();
    expect(screen.getByText('Explain the project architecture')).toBeInTheDocument();
  });
});

describe('chat page — sending message', () => {
  it('sends message and shows response', async () => {
    const qc = createTestQueryClient();
    renderChatPage(qc);

    // Type a message and send
    const input = screen.getByPlaceholderText('Ask anything...');
    const user = userEvent.setup();
    await user.type(input, 'Hello');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    // User message should appear
    expect(await screen.findByText('Hello')).toBeInTheDocument();

    // Agent response should appear (from MSW handler: 'AI response text')
    expect(await screen.findByText('AI response text')).toBeInTheDocument();
  });

  it('suggestion chip click sends message', async () => {
    const qc = createTestQueryClient();
    renderChatPage(qc);

    const chip = screen.getByText('What tasks are due today?');
    const user = userEvent.setup();
    await user.click(chip);

    // Chip text should appear as user message
    expect(await screen.findByText('What tasks are due today?')).toBeInTheDocument();
  });
});

describe('chat page — error handling', () => {
  it('shows error message on API failure', async () => {
    const qc = createTestQueryClient();

    // Override the MSW handler to return 503
    const { http, HttpResponse } = await import('msw');
    const { server } = await import('../setup');
    server.use(
      http.post('*/api/v1/chat', () => HttpResponse.json(null, { status: 503 })),
    );

    renderChatPage(qc);

    const input = screen.getByPlaceholderText('Ask anything...');
    const user = userEvent.setup();
    await user.type(input, 'Will this fail?');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    // Should show the AI service unavailable message
    expect(await screen.findByText(/AI service is temporarily unavailable/i)).toBeInTheDocument();
  });
});

describe('chat page — markdown rendering', () => {
  it('renders markdown content from agent responses', async () => {
    const qc = createTestQueryClient();

    // Override MSW handler to return markdown content
    const { http, HttpResponse } = await import('msw');
    const { server } = await import('../setup');
    server.use(
      http.post('*/api/v1/chat', () =>
        HttpResponse.json({
          response: '**Bold text** and *italic* and `inline code`',
          conversation_id: 1,
        }),
      ),
    );

    renderChatPage(qc);

    const input = screen.getByPlaceholderText('Ask anything...');
    const user = userEvent.setup();
    await user.type(input, 'Show markdown');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    // Bold text should be rendered via react-markdown
    expect(await screen.findByText('Bold text')).toBeInTheDocument();
  });
});

describe('chat page — save as note', () => {
  it('shows "Save as note" button on agent messages and navigates', async () => {
    const qc = createTestQueryClient();
    renderChatPage(qc);

    const input = screen.getByPlaceholderText('Ask anything...');
    const user = userEvent.setup();
    await user.type(input, 'Save this response');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    // Wait for response, then check for "Save as note" button
    const saveButton = await screen.findByText('Save as note');
    expect(saveButton).toBeInTheDocument();

    await user.click(saveButton);

    // Should have stored content in sessionStorage
    expect(sessionStorage.getItem('chat-quick-note')).toBe('AI response text');

    // Should have navigated to notes
    expect(mockNavigate).toHaveBeenCalledWith({
      to: '/notes',
      search: { action: 'create', type: 'note' },
    });
  });
});
