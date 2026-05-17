import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/calendar/')({ component: CalendarPage });

function CalendarPage() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Calendar</h1>
      <p className="mt-4 text-lg text-muted-foreground">Calendar events will appear here.</p>
    </div>
  );
}
