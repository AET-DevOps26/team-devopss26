import { createFileRoute } from '@tanstack/react-router';
import { LogInIcon } from 'lucide-react';

export const Route = createFileRoute('/login')({ component: LoginPage });

function LoginPage() {
  return (
    <div className="flex min-h-screen flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
      <LogInIcon className="size-12 text-muted-foreground" />
      <h1 className="text-4xl font-bold tracking-tight">Login</h1>
      <p className="max-w-sm text-muted-foreground">
        Login page coming soon. Authentication will be implemented in the next issue.
      </p>
    </div>
  );
}
