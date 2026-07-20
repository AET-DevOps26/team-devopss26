import { useState, useRef, useEffect, useCallback } from 'react';
import { createFileRoute, useRouter } from '@tanstack/react-router';
import { useQueryErrorResetBoundary } from '@tanstack/react-query';
import { Button } from '#/components/ui/button.tsx';
import { Textarea } from '#/components/ui/textarea.tsx';
import { Spinner } from '#/components/ui/spinner.tsx';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { Badge } from '#/components/ui/badge.tsx';
import { Avatar, AvatarFallback } from '#/components/ui/avatar.tsx';
import {
  BotIcon,
  SendIcon,
  RefreshCwIcon,
  UserIcon,
  SparklesIcon,
  BookmarkIcon,
  Trash2Icon,
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useSendMessage, useDeleteConversation } from '#/lib/queries/chat.ts';
import { classifyChatError } from '#/lib/utils/chat.ts';
import { genId } from '#/lib/utils';
import { getConversation } from '#/services/genai/gen-a-i/gen-a-i';

// ── Types ──────────────────────────────────────────────────────

/**
 * Chat message with content and display state.
 * - `'sent'` — successfully delivered.
 * - `'error'` — failed to send (shows retry button).
 * - `'typing'` — placeholder skeleton while agent generates.
 *
 * `model` is the underlying LLM identifier returned by the server (see
 * `ChatResponse.model` in `api/genai-service.yaml`). It is only populated
 * for messages produced in the current session; restored history falls
 * back to {@link MODEL_FALLBACK_LABEL}.
 */
interface Message {
  id: string;
  role: 'user' | 'agent';
  content: string;
  state: 'sent' | 'error' | 'typing';
  model?: string;
}

// ── Constants ──────────────────────────────────────────────────

/** Quick-prompt suggestion chips shown in the welcome state. */
const suggestionChips = [
  'What tasks are due today?',
  'Summarize my recent notes',
  'Help me plan this sprint',
  'Explain the project architecture',
];

/**
 * Human-friendly display names for the model identifiers the genai-service
 * can return (see `_resolve_model_name` in `services/genai-service/main.py`).
 * Keys must stay in sync with the server-side resolver; unknown identifiers
 * fall through to the raw value via {@link formatModelLabel}.
 */
const MODEL_DISPLAY_NAMES: Record<string, string> = {
  'llama-3.1-8b-instant': 'Llama 3.1 8B (Groq)',
  'mistral-small-latest': 'Mistral Small',
  'command-r': 'Cohere Command R',
  'llama3.1': 'Llama 3.1 (Ollama)',
  'llama3.2:1b': 'Llama 3.2 1B (Ollama)',
  'gemini-3.1-flash-lite': 'Gemini 3.1 Flash Lite',
};

/** Fallback label for messages where the server didn't report a model (e.g. restored history). */
const MODEL_FALLBACK_LABEL = 'AI Assistant';

/**
 * Turn a server-side model identifier into a label suitable for the agent
 * message badge. Falls back to {@link MODEL_FALLBACK_LABEL} when no model
 * is known and to the raw identifier when it isn't in {@link MODEL_DISPLAY_NAMES}.
 */
function formatModelLabel(model?: string): string {
  if (!model) return MODEL_FALLBACK_LABEL;
  return MODEL_DISPLAY_NAMES[model] ?? model;
}

/** LocalStorage key for persisting the last active conversation ID across sessions. */
const LS_CONVERSATION_KEY = 'chat-last-conversation-id';

// ── Route ───────────────────────────────────────────────────────

/**
 * Error-state fallback for the chat route.
 * Resets the query error boundary and reloads on retry.
 */
function RouteErrorComponent() {
  const queryErrorReset = useQueryErrorResetBoundary();

  const handleRetry = () => {
    queryErrorReset.reset();
    window.location.reload();
  };

  return (
    <div className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center">
      <div className="mb-4 flex size-14 items-center justify-center rounded-2xl bg-destructive/10 ring-1 ring-destructive/20">
        <BotIcon className="size-7 text-destructive" />
      </div>
      <h2 className="text-xl font-bold tracking-tight">Something went wrong</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        The chat encountered an unexpected error. Please try again.
      </p>
      <Button variant="outline" className="mt-4" onClick={handleRetry}>
        <RefreshCwIcon data-icon="inline-start" />Try Again
      </Button>
    </div>
  );
}

export const Route = createFileRoute('/_authenticated/chat/')({
  component: ChatPage,
  errorComponent: RouteErrorComponent,
});

// ── Streaming Markdown ─────────────────────────────────────────

/**
 * Renders a syntax-highlighted code block with a copy button.
 * Language is inferred from the className (e.g. `language-typescript`).
 * Uses `react-syntax-highlighter` with the `oneDark` theme.
 */
function CodeBlock({ className, children }: { className?: string; children?: React.ReactNode }) {
  const match = /language-(\w+)/.exec(className ?? '');
  const rawCode = typeof children === 'string' ? children : '';
  const code = rawCode.replace(/\n$/, '');
  const [showCopy, setShowCopy] = useState(false);

  return (
    <div
      className="relative my-2 rounded-lg border bg-muted/50 overflow-hidden"
      onMouseEnter={() => { setShowCopy(true); }}
      onMouseLeave={() => { setShowCopy(false); }}
    >
      {match && (
        <div className="flex items-center justify-between px-4 py-1.5 text-xs text-muted-foreground border-b bg-muted/30">
          <span>{match[1]}</span>
          {showCopy && (
            <button
              type="button"
              className="hover:text-foreground transition-colors"
              onClick={() => { void navigator.clipboard.writeText(code); }}
            >
              Copy
            </button>
          )}
        </div>
      )}
      <div className="overflow-x-auto">
        {match ? (
          <SyntaxHighlighter
            language={match[1]}
            style={oneDark}
            customStyle={{ background: 'transparent', padding: '0.75rem 1rem', fontSize: '0.8125rem', margin: 0 }}
            showLineNumbers={false}
          >
            {code}
          </SyntaxHighlighter>
        ) : (
          <pre className="p-3 text-sm overflow-x-auto"><code className={className}>{children}</code></pre>
        )}
      </div>
    </div>
  );
}

/**
 * Renders agent responses as formatted Markdown with GFM support.
 * Code blocks are syntax-highlighted via `CodeBlock`; inline code and
 * other elements use default `react-markdown` + `rehype-sanitize` rendering.
 */
function StreamingMarkdown({ content }: { content: string }) {
  return (
    <div className="prose prose-sm dark:prose-invert max-w-none [&_:is(pre,code)]:before:!content-none [&_:is(pre,code)]:after:!content-none">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSanitize]}
        components={{
          code({ className, children, ...props }) {
            const isBlock = className?.startsWith('language-');
            if (isBlock) {
              return <CodeBlock className={className}>{children}</CodeBlock>;
            }
            return <code className={className} {...props}>{children}</code>;
          },
          pre({ children }) {
            return <>{children}</>;
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────

/**
 * Initial empty-state view shown when no conversation is active.
 * Displays suggestion chips that kick off a new chat on click.
 */
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
            onClick={() => { onChipClick(chip); }}
          >
            <SparklesIcon className="size-3.5 mr-1.5 text-primary" />
            {chip}
          </Button>
        ))}
      </div>
    </div>
  );
}

/**
 * Renders a single chat message with role-based alignment and styling.
 * User messages are right-aligned in primary color; agent messages are
 * left-aligned in muted background with Markdown rendering.
 *
 * Supports additional actions:
 * - Error messages show a "Try Again" button.
 * - Agent messages show a "Save as note" button.
 * - Typing state renders skeleton placeholders.
 */
function MessageBubble({
  message,
  onRetry,
  onSaveAsNote,
}: {
  message: Message;
  onRetry?: () => void;
  onSaveAsNote?: (content: string) => void;
}) {
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
          <AvatarFallback className="bg-primary/10 text-primary">
            <BotIcon className="size-4" />
          </AvatarFallback>
        )}
      </Avatar>

      <div className={`max-w-[75%] ${isUser ? 'items-end' : 'items-start'} flex flex-col`}>
        {!isUser && (
          <div className="flex items-center gap-2 mb-1">
            <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 font-normal text-muted-foreground">
              {formatModelLabel(message.model)}
            </Badge>
          </div>
        )}
        <div
          className={`rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
            isUser
              ? 'bg-primary text-primary-foreground rounded-tr-md whitespace-pre-wrap'
              : 'bg-muted text-foreground rounded-tl-md'
          } ${message.state === 'error' ? 'bg-destructive/10 text-destructive ring-1 ring-destructive/20' : ''}`}
        >
          {isUser ? message.content : <StreamingMarkdown content={message.content} />}
        </div>

        {!isUser && message.state === 'sent' && onSaveAsNote && (
          <Button
            variant="ghost"
            size="xs"
            className="mt-1 gap-1 text-muted-foreground h-7"
            onClick={() => { onSaveAsNote(message.content); }}
          >
            <BookmarkIcon className="size-3" />
            Save as note
          </Button>
        )}

        {message.state === 'error' && (
          <Button variant="ghost" size="xs" className="mt-1 text-destructive gap-1" onClick={onRetry}>
            <RefreshCwIcon className="size-3" />
            Try Again
          </Button>
        )}
      </div>
    </div>
  );
}

// ── Main page component ────────────────────────────────────────

/**
 * Main chat page — real AI chat with the GenAI backend.
 *
 * **States:**
 * - Welcome screen with suggestion chips when no conversation exists.
 * - Live chat with message history, streaming Markdown rendering.
 * - Loading skeleton during agent response generation.
 * - Error state with retry on API failure.
 *
 * **Persistence:** The last conversation ID is saved to localStorage so
 * the chat history survives page reloads. A "New Chat" button deletes
 * the current conversation and starts fresh.
 *
 * **Data flow:**
 * 1. Send message → POST to GenAI API via `sendMutation`.
 * 2. RAG context (notes, events, checklists) is fetched server-side.
 * 3. Response rendered as Markdown via `StreamingMarkdown`.
 * 4. "Save as note" redirects to notes page with pre-filled content.
 */
export function ChatPage() {
  const router = useRouter();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showWelcome, setShowWelcome] = useState(true);
  const [showRagStatus, setShowRagStatus] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const sendMutation = useSendMessage();
  const deleteMutation = useDeleteConversation();

  // Restore conversation from localStorage on mount
  useEffect(() => {
    const savedId = localStorage.getItem(LS_CONVERSATION_KEY);
    if (savedId) {
      const id = Number(savedId);
      if (!isNaN(id)) {
        getConversation(id)
          .then((conv) => {
            if (conv.messages && conv.messages.length > 0) {
              const restored: Message[] = conv.messages.map((m) => ({
                id: genId(),
                role: m.role === 'USER' ? 'user' : 'agent',
                content: m.content ?? '',
                state: 'sent' as const,
              }));
              setMessages(restored);
              setConversationId(id);
              setShowWelcome(false);
            }
          })
          .catch(() => {
            // 404 or error — conversation was deleted, start fresh
            localStorage.removeItem(LS_CONVERSATION_KEY);
          });
      }
    }
  }, []);

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isLoading, showRagStatus]);

  // Show RAG status briefly after sending
  useEffect(() => {
    if (showRagStatus) {
      const timer = setTimeout(() => { setShowRagStatus(false); }, 3000);
      return () => { clearTimeout(timer); };
    }
  }, [showRagStatus]);

  /**
   * Send a user message to the GenAI backend and append the response.
   * Creates a new conversation if none is active; saves conversation ID
   * to localStorage on success. On error, classifies the error type and
   * displays a user-friendly message.
   */
  const sendMessage = useCallback(
    (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || isLoading) return;

      setShowWelcome(false);

      const userMsg: Message = { id: genId(), role: 'user', content: trimmed, state: 'sent' };
      setMessages((prev) => [...prev, userMsg]);
      setInput('');
      setIsLoading(true);
      setShowRagStatus(true);

      sendMutation.mutate(
        { message: trimmed, conversationId: conversationId ?? undefined },
        {
          onSuccess: (data) => {
            const newConvId = data.conversation_id;
            if (newConvId) {
              setConversationId(newConvId);
              localStorage.setItem(LS_CONVERSATION_KEY, String(newConvId));
            }
            setMessages((prev) => [
              ...prev,
              {
                id: genId(),
                role: 'agent',
                content: data.response ?? '',
                state: 'sent',
                model: data.model ?? undefined,
              },
            ]);
            setIsLoading(false);
            setShowRagStatus(false);
          },
          onError: (error: unknown) => {
            const classified = classifyChatError(error);
            setMessages((prev) => {
              const next = [...prev];
              // Mark the last user message as errored
              const lastMsg = next[next.length - 1];
              if (lastMsg.role === 'user') {
                next[next.length - 1] = { ...lastMsg, state: 'error', content: `${lastMsg.content}\n\n${classified.message}` };
              }
              return next;
            });
            setIsLoading(false);
            setShowRagStatus(false);
          },
        },
      );
    },
    [isLoading, conversationId, sendMutation],
  );

  const handleChipClick = useCallback(
    (text: string) => { sendMessage(text); },
    [sendMessage],
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(input);
      }
    },
    [input, sendMessage],
  );

  const handleRetry = useCallback(
    (failedText: string) => { sendMessage(failedText); },
    [sendMessage],
  );

  const handleSaveAsNote = useCallback(
    (content: string) => {
      sessionStorage.setItem('chat-quick-note', content);
      void router.navigate({
        to: '/notes',
        search: { action: 'create' as const, type: 'note' as const },
      });
    },
    [router],
  );

  const handleNewChat = useCallback(() => {
    if (conversationId) {
      deleteMutation.mutate(conversationId, {
        onSuccess: () => {
          setMessages([]);
          setConversationId(null);
          setShowWelcome(true);
          localStorage.removeItem(LS_CONVERSATION_KEY);
        },
      });
    } else {
      setMessages([]);
      setConversationId(null);
      setShowWelcome(true);
    }
  }, [conversationId, deleteMutation]);

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Avatar className="size-8">
              <AvatarFallback className="bg-primary/10 text-primary">
                <BotIcon className="size-4" />
              </AvatarFallback>
            </Avatar>
            <div>
              <h2 className="text-sm font-semibold">AI Assistant</h2>
              <p className="text-xs text-muted-foreground">
                {isLoading ? 'Typing...' : showWelcome ? 'Ready' : 'Online'}
              </p>
            </div>
          </div>
          {!showWelcome && (
            <Button variant="ghost" size="sm" className="gap-1 text-muted-foreground" onClick={handleNewChat}>
              <Trash2Icon className="size-3.5" />
              New Chat
            </Button>
          )}
        </div>
      </div>

      {/* Messages area */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto py-4">
        {showWelcome ? (
          <WelcomeState onChipClick={handleChipClick} />
        ) : (
          <>
            {messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                onRetry={
                  msg.state === 'error'
                    ? () => {
                        // Find the original text (before error message was appended)
                        const originalText = msg.content.split('\n\n')[0];
                        handleRetry(originalText);
                      }
                    : undefined
                }
                onSaveAsNote={msg.role === 'agent' && msg.state === 'sent' ? handleSaveAsNote : undefined}
              />
            ))}

            {showRagStatus && (
              <div className="flex items-center gap-2 px-6 py-1 text-xs text-muted-foreground animate-pulse">
                <Spinner className="size-3" />
                Searching your notes and calendar…
              </div>
            )}

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
            onChange={(e) => { setInput(e.target.value); }}
            onKeyDown={handleKeyDown}
            placeholder="Ask anything..."
            disabled={isLoading}
            className="min-h-10 max-h-32 resize-none"
            rows={1}
          />
          <Button
            size="icon"
            aria-label="Send message"
            onClick={() => { sendMessage(input); }}
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
