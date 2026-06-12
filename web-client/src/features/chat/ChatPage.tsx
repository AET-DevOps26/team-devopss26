import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { Button } from '#/components/ui/button.tsx';
import { cn } from '#/lib/utils';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { Send, Loader2 } from 'lucide-react';

interface Message {
  id: string;
  role: 'user' | 'agent';
  content: string;
}

const API_BASE = 'http://localhost:8006';
const USER_ID = 1;

export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-scroll to bottom on new messages or loading state change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  // Auto-resize textarea as content grows (max ~4 lines)
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${String(Math.min(textareaRef.current.scrollHeight, 96))}px`;
    }
  }, [inputValue]);

  async function sendMessage(text: string) {
    if (!text.trim() || isLoading) return;

    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: text.trim(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    setIsLoading(true);
    setError(null);

    try {
      const body: Record<string, unknown> = { message: text.trim(), user_id: USER_ID };
      if (conversationId !== null) {
        body.conversation_id = conversationId;
      }

      const response = await fetch(`${API_BASE}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${String(response.status)}`);
      }

      const data = (await response.json()) as { response: string; conversation_id: number };
      setConversationId(data.conversation_id);

      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: 'agent' as const, content: data.response },
      ]);
    } catch {
      setError('Failed to send message. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void sendMessage(inputValue);
    }
  }

  const showWelcome = messages.length === 0 && !error;

  return (
    <div className="flex h-full flex-col">
      {/* Scrollable messages area */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="mx-auto max-w-2xl">
          {showWelcome && (
            <div className="flex min-h-[calc(100vh-14rem)] items-center justify-center">
              <div className="text-center">
                <h2 className="mb-2 text-2xl font-semibold">Chat</h2>
                <p className="text-muted-foreground">Send a message to start chatting</p>
              </div>
            </div>
          )}

          {!showWelcome && (
            <div className="space-y-4">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={cn('flex', msg.role === 'user' ? 'justify-end' : 'justify-start')}
                >
                  <div
                    className={cn(
                      'max-w-[80%] rounded-lg px-4 py-2 whitespace-pre-wrap break-words',
                      msg.role === 'user'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted text-foreground',
                    )}
                  >
                    {msg.content}
                  </div>
                </div>
              ))}

              {/* Loading indicator - shows while waiting for API response */}
              {isLoading && (
                <div className="flex justify-start">
                  <div className="rounded-lg bg-muted px-4 py-3">
                    <Skeleton className="h-4 w-24" />
                  </div>
                </div>
              )}

              {/* Error banner */}
              {error && (
                <div className="flex justify-center">
                  <div className="rounded-lg bg-destructive/10 px-4 py-2 text-sm text-destructive">
                    {error}
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          )}
        </div>
      </div>

      {/* Pinned input area */}
      <div className="border-t bg-background p-4">
        <div className="mx-auto flex max-w-2xl gap-2">
          <textarea
            ref={textareaRef}
            value={inputValue}
            onChange={(e) => { setInputValue(e.target.value); }}
            onKeyDown={handleKeyDown}
            placeholder="Type a message... (Enter to send, Shift+Enter newline)"
            disabled={isLoading}
            rows={1}
            className="flex-1 resize-none rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            style={{ maxHeight: '6rem' }}
          />
          <Button
            size="icon"
            onClick={() => { void sendMessage(inputValue); }}
            disabled={isLoading || !inputValue.trim()}
            aria-label="Send message"
          >
            {isLoading ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Send className="size-4" />
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
