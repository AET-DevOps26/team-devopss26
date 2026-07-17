import { createFileRoute } from '@tanstack/react-router';
import { RegisterPage } from 'src/pages/RegisterPage';

/**
 * Registration page route. On success, redirects to `/login` after a short delay.
 */
export const Route = createFileRoute('/_unauthenticated/register')({ component: RegisterPage });
