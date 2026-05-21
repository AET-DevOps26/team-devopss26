import { createFileRoute } from '@tanstack/react-router';
import { Button } from '#/components/ui/button.tsx';

export const Route = createFileRoute('/')({ component: Home });

function Home() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Welcome to our App!</h1>
      <p className="mt-4 text-lg">
          <Button> example button </Button>
      </p>
    </div>
  );
}
