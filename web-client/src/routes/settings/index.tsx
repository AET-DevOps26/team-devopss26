import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/settings/')({ component: SettingsPage });

function SettingsPage() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Settings</h1>
      <p className="mt-4 text-lg text-muted-foreground">Profile and settings will appear here.</p>
    </div>
  );
}
