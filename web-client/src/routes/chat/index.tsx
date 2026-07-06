import { useState, useRef, useEffect } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { Button } from '#/components/ui/button.tsx';
import { Textarea } from '#/components/ui/textarea.tsx';
import { Spinner } from '#/components/ui/spinner.tsx';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { Avatar, AvatarImage, AvatarFallback } from '#/components/ui/avatar.tsx';
import { BotIcon, SendIcon, RefreshCwIcon, UserIcon, SparklesIcon } from 'lucide-react';

export const Route = createFileRoute('/chat/')({ component: ChatPage });

// ── Types ──────────────────────────────────────────────────────

interface Message {
  id: string;
  role: 'user' | 'agent';
  content: string;
  state: 'sent' | 'error' | 'typing';
}

// ── Mock data ──────────────────────────────────────────────────

const suggestionChips = [
  'What tasks are due today?',
  'Summarize my recent notes',
  'Help me plan this sprint',
  'Explain the project architecture',
];

const mockAgentResponse = "I can help you with that! Here's what I found:\n\nYou have **3 upcoming events** today, including a team standup at 10 AM and a design review at 2 PM. Your sprint checklist is 2/4 tasks complete. Would you like me to:\n\n- Create a new note for this?\n- Add a task to your checklist?\n- Schedule a follow-up meeting?";

// ── Sub-components ─────────────────────────────────────────────

function WelcomeState({ onChipClick }: { onChipClick: (text: string) => void }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center">
      <div className="mb-4 flex size-14 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
        <BotIcon className="size-7 text-primary" />
      </div>
      <h2 className="text-xl font-bold tracking-tight">How can I help you?</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        I'm your AI assistant. Ask me about your notes, tasks, events, or anything you need help with.
      </p>
      <div className="mt-6 flex flex-wrap justify-center gap-2">
        {suggestionChips.map((chip) => (
          <Button
            key={chip}
            variant="outline"
            size="sm"
            className="rounded-full"
            onClick={() => onChipClick(chip)}
          >
            <SparklesIcon className="size-3.5 mr-1.5 text-primary" />
            {chip}
          </Button>
        ))}
      </div>
    </div>
  );
}

function MessageBubble({ message }: { message: Message }) {
  const isUser = message.role === 'user';

  if (message.state === 'typing') {
    return (
      <div className="flex items-start gap-3 px-6 py-2">
        <Avatar className="size-8 shrink-0">
          <AvatarFallback className="bg-primary/10 text-primary">
            <BotIcon className="size-4" />
          </AvatarFallback>
        </Avatar>
        <div className="flex flex-col gap-2">
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-4 w-32" />
        </div>
      </div>
    );
  }

  return (
    <div className={`flex items-start gap-3 px-6 py-2 ${isUser ? 'flex-row-reverse' : ''}`}>
      <Avatar className="size-8 shrink-0">
        {isUser ? (
          <AvatarFallback className="bg-muted">
            <UserIcon className="size-4" />
          </AvatarFallback>
        ) : (
          <>
            <AvatarImage src="" />
            <AvatarFallback className="bg-primary/10 text-primary">
              <BotIcon className="size-4" />
            </AvatarFallback>
          </>
        )}
      </Avatar>

      <div className={`max-w-[75%] ${isUser ? 'items-end' : 'items-start'} flex flex-col`}>
        <div
          className={`rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap ${
            isUser
              ? 'bg-primary text-primary-foreground rounded-tr-md'
              : 'bg-muted text-foreground rounded-tl-md'
          } ${message.state === 'error' ? 'bg-destructive/10 text-destructive ring-1 ring-destructive/20' : ''}`}
        >
          {message.content}
        </div>

        {message.state === 'error' && (
          <Button variant="ghost" size="xs" className="mt-1 text-destructive gap-1">
            <RefreshCwIcon className="size-3" />
            Try Again
          </Button>
        )}
      </div>
    </div>
  );
}

// ── Main page component ────────────────────────────────────────

function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showWelcome, setShowWelcome] = useState(true);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isLoading]);

  const sendMessage = (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || isLoading) return;

    setShowWelcome(false);

    const userMsg: Message = { id: crypto.randomUUID(), role: 'user', content: trimmed, state: 'sent' };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setIsLoading(true);

    // Simulate agent response after delay
    setTimeout(() => {
      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: 'agent', content: mockAgentResponse, state: 'sent' },
      ]);
      setIsLoading(false);
    }, 1500);
  };

  const handleChipClick = (text: string) => {
    sendMessage(text);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  };

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b px-6 py-3">
        <div className="flex items-center gap-3">
          <Avatar className="size-8">
            <AvatarFallback className="bg-primary/10 text-primary">
              <BotIcon className="size-4" />
            </AvatarFallback>
          </Avatar>
          <div>
            <h2 className="text-sm font-semibold">AI Assistant</h2>
            <p className="text-xs text-muted-foreground">{isLoading ? 'Typing...' : 'Online'}</p>
          </div>
        </div>
      </div>

      {/* Messages area */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto py-4">
        {showWelcome ? (
          <WelcomeState onChipClick={handleChipClick} />
        ) : (
          <>
            {messages.map((msg) => (
              <MessageBubble key={msg.id} message={msg} />
            ))}
            {isLoading && (
              <MessageBubble
                message={{ id: 'typing', role: 'agent', content: '', state: 'typing' }}
              />
            )}
          </>
        )}
      </div>

      {/* Input bar */}
      <div className="border-t bg-background px-4 py-3">
        <div className="flex items-end gap-2 max-w-4xl mx-auto">
          <Textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask anything..."
            disabled={isLoading}
            className="min-h-10 max-h-32 resize-none"
            rows={1}
          />
          <Button
            size="icon"
            onClick={() => sendMessage(input)}
            disabled={!input.trim() || isLoading}
            className="shrink-0 mb-0.5"
          >
            {isLoading ? <Spinner className="size-4" /> : <SendIcon className="size-4" />}
          </Button>
        </div>
      </div>
    </div>
  );
}
