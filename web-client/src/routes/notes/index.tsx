import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/notes/')({ component: NotesPage });

function NotesPage() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Notes</h1>
      <p className="mt-4 text-lg text-muted-foreground">Notes and checklists will appear here.</p>
    </div>
  );
}
