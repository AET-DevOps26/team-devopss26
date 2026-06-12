import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';

// =============================================================================
// Module-level mocks — hoisted by vitest before any imports
// =============================================================================

// Mock base-ui button (used by #/components/ui/button.tsx)
vi.mock('@base-ui/react/button', () => ({
  Button: ({ children, ...props }: Record<string, unknown>) => (
    <button data-slot="base-button" {...props}>
      {children as React.ReactNode}
    </button>
  ),
}));

// Mock utility function
vi.mock('#/lib/utils', () => ({
  cn: (...inputs: unknown[]) => inputs.filter(Boolean).join(' '),
}));

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  Send: () => <svg data-testid="send-icon" />,
  Loader2: () => <svg data-testid="loader-icon" />,
}));

// =============================================================================
// Imports (after all vi.mock calls)
// =============================================================================
import { ChatPage } from '../ChatPage';

// =============================================================================
// Tests
// =============================================================================

// jsdom does not implement scrollIntoView — mock it
beforeEach(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

describe('ChatPage — rendering', () => {
  beforeEach(() => {
    vi.spyOn(crypto, 'randomUUID').mockImplementation(() => 'test-uuid');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ---------------------------------------------------------------------------
  // 1. Welcome state: heading + prompt visible when no messages
  // ---------------------------------------------------------------------------
  it('renders welcome heading and prompt when no messages exist', () => {
    render(<ChatPage />);

    expect(
      screen.getByRole('heading', { name: 'Chat' }),
    ).toBeInTheDocument();

    expect(
      screen.getByText('Send a message to start chatting'),
    ).toBeInTheDocument();
  });

  // ---------------------------------------------------------------------------
  // 2. Input controls: textarea placeholder and send button aria-label
  // ---------------------------------------------------------------------------
  it('renders textarea with correct placeholder and send button with aria-label', () => {
    render(<ChatPage />);

    const textarea = screen.getByPlaceholderText(
      "Type a message... (Enter to send, Shift+Enter newline)",
    );
    expect(textarea).toBeInTheDocument();
    expect(textarea).toHaveAttribute('rows', '1');

    const sendButton = screen.getByRole('button', { name: /send message/i });
    expect(sendButton).toBeInTheDocument();
  });

  // ---------------------------------------------------------------------------
  // 3. Send button is disabled when input is empty
  // ---------------------------------------------------------------------------
  it('disables send button when input is empty and enables it when text is entered', async () => {
    const user = userEvent.setup();
    render(<ChatPage />);

    const sendButton = screen.getByRole('button', { name: /send message/i });
    expect(sendButton).toBeDisabled();

    const textarea = screen.getByPlaceholderText(
      "Type a message... (Enter to send, Shift+Enter newline)",
    );
    await user.type(textarea, 'Hello');

    expect(sendButton).toBeEnabled();
  });

  // ---------------------------------------------------------------------------
  // 4. Enter key sends, Shift+Enter does not send
  // ---------------------------------------------------------------------------
  it('does not call fetch on Shift+Enter but does call fetch on Enter', async () => {
    const fetchMock = vi.fn();
    globalThis.fetch = fetchMock;

    const user = userEvent.setup();
    render(<ChatPage />);

    const textarea = screen.getByPlaceholderText(
      "Type a message... (Enter to send, Shift+Enter newline)",
    );

    // Shift+Enter should NOT trigger a fetch call
    await user.type(textarea, 'Hello{Shift>}{Enter}{/Shift}');
    expect(fetchMock).not.toHaveBeenCalled();

    // Enter should trigger a fetch call
    await user.type(textarea, '{Enter}');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
