import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginPage } from '../../pages/LoginPage';
import { useAuthStore } from '#/stores/authStore.ts';

// Mock router hooks that LoginPage depends on
const mockNavigate = vi.fn();
vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useRouter: () => ({ navigate: mockNavigate }),
    Link: ({ to, children, ...props }: { to: string; children: React.ReactNode; [key: string]: unknown }) => (
      <a href={to} {...props}>{children}</a>
    ),
  };
});

beforeEach(() => {
  useAuthStore.getState().clearAuth();
  mockNavigate.mockClear();
});

describe('login page', () => {
  it('renders the login form with username and password fields', () => {
    render(<LoginPage />);
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('Username is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
  });

  it('shows validation error for short username', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText('Username'), 'ab');
    await user.type(screen.getByLabelText('Password'), '123456');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('Username must be at least 3 characters')).toBeInTheDocument();
  });

  it('shows validation error for short password', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText('Username'), 'testuser');
    await user.type(screen.getByLabelText('Password'), '12345');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('Password must be at least 6 characters')).toBeInTheDocument();
  });

  it('shows link to register page', () => {
    render(<LoginPage />);
    expect(screen.getByRole('link', { name: 'Create one' })).toBeInTheDocument();
  });

  it('renders the card title', () => {
    render(<LoginPage />);
    expect(screen.getAllByText('Sign in').length).toBeGreaterThanOrEqual(1);
  });
});
