import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RegisterPage } from '../../pages/RegisterPage';
import { useAuthStore } from '#/stores/authStore.ts';

// Mock router hooks that RegisterPage depends on
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

describe('register page', () => {
  it('renders the register form with all fields', () => {
    render(<RegisterPage />);
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Username is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(screen.getByText('Please confirm your password')).toBeInTheDocument();
  });

  it('shows error when passwords do not match', async () => {
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.type(screen.getByLabelText('Username'), 'testuser');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm password'), 'different');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
  });

  it('shows link to login page', () => {
    render(<RegisterPage />);
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('renders the card title', () => {
    render(<RegisterPage />);
    expect(screen.getAllByText('Create account').length).toBeGreaterThanOrEqual(1);
  });
});
