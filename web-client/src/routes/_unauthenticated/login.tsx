import { createFileRoute } from '@tanstack/react-router';
import { LoginPage } from 'src/pages/LoginPage';

/**
 * Login page route. Renders LoginPage which provides HTTP Basic Auth flow.
 */
export const Route = createFileRoute('/_unauthenticated/login')({ component: LoginPage });
