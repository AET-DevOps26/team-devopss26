import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/chat/')({ component: ChatPage });

function ChatPage() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Chat</h1>
      <p className="mt-4 text-lg text-muted-foreground">AI chat will appear here.</p>
    </div>
  );
}
