import { useState } from 'react';
import { Link, useRouter } from '@tanstack/react-router';
import { UserPlusIcon } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from 'src/components/ui/card';
import { Input } from 'src/components/ui/input';
import { Button } from 'src/components/ui/button';
import { Spinner } from 'src/components/ui/spinner';
import { registerUser } from 'src/services/users/user-authentication/user-authentication';
import type { RegisterUserRequest } from 'src/types/users';

/**
 * Registration form with client-side validation. 409 → "Username already taken".
 * On success, redirect to `/login` after 1.5s delay. All inputs disabled
 * during loading.
 */
export function RegisterPage() {
  const router = useRouter();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{
    username?: string;
    password?: string;
    confirmPassword?: string;
    form?: string;
    success?: string;
  }>({});
  const [loading, setLoading] = useState(false);

/**
 * Client-side validation: username ≥3 chars, password ≥6 chars, confirm must match.
 *
 * @returns `true` when all fields pass validation, `false` otherwise.
 */
  function validate(): boolean {
    const next: { username?: string; password?: string; confirmPassword?: string } = {};

    if (!username.trim()) next.username = 'Username is required';
    else if (username.trim().length < 3) next.username = 'Username must be at least 3 characters';

    if (!password) next.password = 'Password is required';
    else if (password.length < 6) next.password = 'Password must be at least 6 characters';

    if (!confirmPassword) next.confirmPassword = 'Please confirm your password';
    else if (password !== confirmPassword) next.confirmPassword = 'Passwords do not match';

    setErrors(next);
    return Object.keys(next).length === 0;
  }

/**
 * Validates and sends registration request. On success shows banner and
 * navigates to `/login` after 1500ms. On 409 → "Username already taken".
 *
 * **Known limitation:** 1.5s redirect delay does not cancel on unmount.
 *
 * @param e - The form submission event (prevents default behaviour).
 */
  async function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setErrors({});

    try {
      const payload: RegisterUserRequest = { username: username.trim(), password };
      await registerUser(payload);
      setErrors({ success: 'Account created successfully! You can now sign in.' });
      setTimeout(() => {
        void router.navigate({ to: '/login' });
      }, 1500);
    } catch (error) {
      if (error instanceof Error && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 409) {
          setErrors({ form: 'Username already taken' });
        } else {
          setErrors({ form: 'Registration failed. Please try again.' });
        }
      } else {
        setErrors({ form: 'Registration failed. Please try again.' });
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader className="items-center text-center">
        <div className="mb-2 flex size-10 items-center justify-center rounded-full bg-primary/10">
          <UserPlusIcon className="size-5 text-primary" />
        </div>
        <CardTitle className="text-xl">Create account</CardTitle>
        <CardDescription>Enter your details to get started</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={(e) => { void handleSubmit(e); }} className="space-y-4">
          {errors.form && (
            <div className="rounded-lg border border-destructive/20 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {errors.form}
            </div>
          )}

          {errors.success && (
            <div className="rounded-lg border border-primary/20 bg-primary/10 px-3 py-2 text-sm text-primary">
              {errors.success}
            </div>
          )}

          <div className="space-y-2">
            <label htmlFor="reg-username" className="text-sm font-medium">
              Username
            </label>
            <Input
              id="reg-username"
              type="text"
              placeholder="Choose a username"
              value={username}
              onChange={(e) => { setUsername(e.target.value); }}
              disabled={loading}
              aria-invalid={!!errors.username}
            />
            {errors.username && (
              <p className="text-xs text-destructive">{errors.username}</p>
            )}
          </div>

          <div className="space-y-2">
            <label htmlFor="reg-password" className="text-sm font-medium">
              Password
            </label>
            <Input
              id="reg-password"
              type="password"
              placeholder="Choose a password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); }}
              disabled={loading}
              aria-invalid={!!errors.password}
            />
            {errors.password && (
              <p className="text-xs text-destructive">{errors.password}</p>
            )}
          </div>

          <div className="space-y-2">
            <label htmlFor="reg-confirm" className="text-sm font-medium">
              Confirm password
            </label>
            <Input
              id="reg-confirm"
              type="password"
              placeholder="Confirm your password"
              value={confirmPassword}
              onChange={(e) => { setConfirmPassword(e.target.value); }}
              disabled={loading}
              aria-invalid={!!errors.confirmPassword}
            />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive">{errors.confirmPassword}</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={loading}>
            {loading && <Spinner className="size-4" />}
            {loading ? 'Creating account…' : 'Create account'}
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary hover:underline">
              Sign in
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  );
}
