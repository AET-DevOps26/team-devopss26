import { createFileRoute } from '@tanstack/react-router';
import { RegisterPage } from 'src/pages/RegisterPage';

export const Route = createFileRoute('/_unauthenticated/register')({ component: RegisterPage });
