import { createFileRoute } from '@tanstack/react-router';
import { LoginPage } from 'src/pages/LoginPage';

export const Route = createFileRoute('/_unauthenticated/login')({ component: LoginPage });
