import { useState } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';
import { Button } from '#/components/ui/button.tsx';
import { Spinner } from '#/components/ui/spinner.tsx';
import { ErrorBoundary } from '#/components/ErrorBoundary.tsx';
import { Empty, EmptyTitle, EmptyDescription, EmptyMedia, EmptyContent } from '#/components/ui/empty.tsx';
import { toast } from 'sonner';
import {
  StickyNoteIcon,
  BellIcon,
  CheckCircleIcon,
  InfoIcon,
  TriangleAlertIcon,
  BugIcon,
  HomeIcon,
  LayoutTemplateIcon,
} from 'lucide-react';

export const Route = createFileRoute('/_authenticated/demo/')({ component: DemoPage });

function Code({ children }: { children: string }) {
  return <code className="rounded bg-muted px-1 py-0.5 font-mono text-[10px]">{children}</code>;
}

function Section({ title, desc, children }: { title: string; desc?: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border bg-card p-5">
      <h2 className="text-base font-semibold tracking-tight mb-1">{title}</h2>
      {desc && <p className="text-sm text-muted-foreground mb-4">{desc}</p>}
      {children}
    </section>
  );
}

// ── Full-screen error fallback ─────────────────────────────────

function FullScreenFallback({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <div className="flex min-h-[calc(100vh-3.5rem)] flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
      <BugIcon className="size-12 text-destructive" />
      <h1 className="text-2xl font-bold tracking-tight">Something went wrong</h1>
      <p className="max-w-sm text-sm text-muted-foreground">
        {error.message}
      </p>
      <div className="flex gap-3">
        <Button onClick={retry}>Retry</Button>
        <Link to="/" className="inline-flex h-8 items-center justify-center rounded-lg border border-border bg-background px-3 text-sm font-medium transition-colors hover:bg-muted">
          <HomeIcon data-icon="inline-start" />Go Home
        </Link>
      </div>
      <p className="mt-4 max-w-md rounded-lg border bg-card p-3 text-xs text-muted-foreground text-left">
        <span className="font-medium text-foreground">ErrorBoundary</span> caught this. A class component using <Code>getDerivedStateFromError</Code> + <Code>componentDidCatch</Code>. Import from <Code>#/components/ErrorBoundary</Code>.
      </p>
    </div>
  );
}

// ── Demo content (wrapped in ErrorBoundary) ────────────────────

function DemoContent({ onCrash }: { onCrash: () => void }) {
  return (
    <div className="p-4 sm:p-6 lg:p-8 space-y-6 max-w-2xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">This Branch: Changes Overview</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Everything added on this branch compared to <Code>main</Code>.
        </p>
      </div>

      {/* ── 1. ErrorBoundary ───────────────────────────────── */}
      <Section title="1. ErrorBoundary" desc="A React class component that catches render errors and shows a fallback UI with retry.">
        <div className="text-sm text-muted-foreground">
          <p>The entire demo page is wrapped in <Code>ErrorBoundary</Code>. Click below to trigger a render crash — the fallback takes over the full screen.</p>
          <Button size="sm" variant="destructive" className="mt-3" onClick={onCrash}>
            <BugIcon data-icon="inline-start" />Trigger Full-Screen Crash
          </Button>
          <div className="mt-3 text-xs text-muted-foreground/60">After crashing, click "Retry" to recover the page.</div>
        </div>
      </Section>

      {/* ── 4. 404 Page ────────────────────────────────────── */}
      <Section title="2. 404 Not Found Page" desc="A <Code>notFoundComponent</Code> on the root route that catches unmatched paths.">
        <div className="rounded-lg border p-3 text-center">
          <div className="flex flex-col items-center gap-3 py-8">
            <LayoutTemplateIcon className="size-10 text-muted-foreground" />
            <h3 className="text-lg font-bold tracking-tight">Page not found</h3>
            <p className="max-w-xs text-sm text-muted-foreground">The page you are looking for does not exist or has been moved.</p>
            <Link to="/" className="inline-flex h-8 items-center justify-center rounded-lg bg-primary px-3 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/80">
              Go Home
            </Link>
          </div>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          Defined in <Code>__root.tsx</Code> via <Code>notFoundComponent</Code>. Try <Code>/nonexistent</Code>.
        </p>
      </Section>

      {/* ── 5. Components ───────────────────────────────────── */}
      <Section title="3. New UI Primitives" desc="Three components supporting the loading/empty/error state pattern across all pages.">
        <div className="space-y-5">
          <div>
            <h3 className="text-xs font-medium text-foreground mb-2 flex items-center gap-1.5"><Spinner className="size-3" /> Spinner</h3>
            <div className="flex flex-wrap items-center gap-3">
              <Spinner className="size-3" />
              <Spinner className="size-4" />
              <Spinner className="size-5" />
              <Spinner className="size-6" />
              <Button disabled size="sm"><Spinner className="size-4" data-icon="inline-start" />Loading</Button>
              <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground"><Spinner className="size-3" />Saving...</span>
            </div>
          </div>

          <div>
            <h3 className="text-xs font-medium text-foreground mb-2 flex items-center gap-1.5"><StickyNoteIcon className="size-3.5" /> Empty (composable)</h3>
            <div className="grid gap-3 sm:grid-cols-2">
              <Empty className="rounded-lg border-dashed p-4">
                <EmptyMedia variant="icon"><StickyNoteIcon className="size-4" /></EmptyMedia>
                <EmptyContent>
                  <EmptyTitle>No notes yet</EmptyTitle>
                  <EmptyDescription>Create your first note.</EmptyDescription>
                </EmptyContent>
              </Empty>
              <Empty className="rounded-lg border-dashed p-4">
                <EmptyMedia variant="icon"><BellIcon className="size-4" /></EmptyMedia>
                <EmptyContent>
                  <EmptyTitle>No notifications</EmptyTitle>
                  <EmptyDescription>You're all caught up.</EmptyDescription>
                </EmptyContent>
              </Empty>
            </div>
          </div>

          <div>
            <h3 className="text-xs font-medium text-foreground mb-2 flex items-center gap-1.5"><BellIcon className="size-3.5" /> Toast (Sonner)</h3>
            <div className="flex flex-wrap gap-2">
              <Button size="sm" onClick={() => toast.success('Saved!', { description: 'Your changes have been saved.' })}>
                <CheckCircleIcon data-icon="inline-start" />Success
              </Button>
              <Button size="sm" variant="secondary" onClick={() => toast.info('Update available', { description: 'Version 2.1.0 ready.' })}>
                <InfoIcon data-icon="inline-start" />Info
              </Button>
              <Button size="sm" variant="outline" onClick={() => toast.warning('Session expiring', { description: '5 minutes remaining.' })}>
                <TriangleAlertIcon data-icon="inline-start" />Warning
              </Button>
              <Button size="sm" variant="destructive" onClick={() => toast.error('Connection lost', { description: 'Check internet and retry.' })}>
                <TriangleAlertIcon data-icon="inline-start" />Error
              </Button>
              <Button size="sm" variant="ghost" onClick={() => toast('Plain message')}>Plain</Button>
            </div>
            <p className="mt-3 text-xs text-muted-foreground">
              Toasts appear in the <span className="font-medium text-foreground">bottom-right</span> corner. <Code>Toaster</Code> is mounted in <Code>App.tsx</Code>.
            </p>
          </div>
        </div>
      </Section>
    </div>
  );
}

// ── Component that throws (must be inside ErrorBoundary) ──────

function ThrowError(): React.ReactNode {
  throw new Error('This is a simulated crash triggered from the demo page.');
}

// ── Page wrapper with crash state ──────────────────────────────

function DemoPage() {
  const [crash, setCrash] = useState(false);

  return (
    <ErrorBoundary
      fallback={(error, retry) => (
        <FullScreenFallback
          error={error}
          retry={() => { setCrash(false); retry(); }}
        />
      )}
    >
      {crash ? <ThrowError /> : <DemoContent onCrash={() => { setCrash(true); }} />}
    </ErrorBoundary>
  );
}
