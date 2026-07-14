import { useMemo } from 'react';
import { createFileRoute, useRouter } from '@tanstack/react-router';
import { useSuspenseQuery, useQueryErrorResetBoundary } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '#/components/ui/card.tsx';
import { Button } from '#/components/ui/button.tsx';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { Badge } from '#/components/ui/badge.tsx';
import { Empty, EmptyTitle, EmptyDescription, EmptyMedia, EmptyContent } from '#/components/ui/empty.tsx';
import {
  StickyNoteIcon,
  CalendarCheck2Icon,
  ListChecksIcon,
  SquarePenIcon,
  PlusCircleIcon,
  CheckSquareIcon,
  BotIcon,
  AlertCircleIcon,
} from 'lucide-react';
import { notesQueries } from '#/lib/queries/notes.ts';
import { checklistQueries } from '#/lib/queries/checklists.ts';
import { calendarQueries } from '#/lib/queries/calendar.ts';

// ── Route config ───────────────────────────────────────────────

export const Route = createFileRoute('/_authenticated/')({
  loader: async ({ context: { queryClient } }) => {
    await Promise.all([
      queryClient.ensureQueryData(notesQueries.all()),
      queryClient.ensureQueryData(checklistQueries.all()),
      queryClient.ensureQueryData(calendarQueries.all()),
    ]);
  },
  pendingComponent: DashboardSkeleton,
  errorComponent: DashboardError,
  component: Home,
});

// ── Greeting ───────────────────────────────────────────────────

function GreetingSection() {
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const date = new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date());

  return (
    <div className="flex items-baseline justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{greeting}, team!</h1>
        <p className="mt-1 text-sm text-muted-foreground">{date}</p>
      </div>
    </div>
  );
}

// ── Error component ────────────────────────────────────────────

function DashboardError({ error, reset }: { error: Error; reset: () => void }) {
  const { reset: resetQuery } = useQueryErrorResetBoundary();

  const handleRetry = () => {
    resetQuery();
    reset();
  };

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center" role="alert">
      <AlertCircleIcon className="size-10 text-destructive" />
      <h2 className="text-xl font-bold tracking-tight">Failed to load dashboard</h2>
      <p className="max-w-sm text-sm text-muted-foreground">
        {error.message || 'Something went wrong while loading your dashboard.'}
      </p>
      <Button onClick={handleRetry}>Try Again</Button>
    </div>
  );
}

// ── Skeleton ───────────────────────────────────────────────────

function DashboardSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8" aria-busy="true" aria-label="Loading dashboard">
      <Skeleton className="h-9 w-48 mb-1" />
      <Skeleton className="h-4 w-64 mb-6" />

      <div className="grid gap-4 sm:grid-cols-3 mb-6">
        {[1, 2, 3].map((i) => (
          <Card key={i} size="sm">
            <CardHeader><Skeleton className="h-4 w-24" /></CardHeader>
            <CardContent><Skeleton className="h-8 w-16" /></CardContent>
          </Card>
        ))}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-8 w-24 rounded-lg" />
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {[1, 2].map((i) => (
          <Card key={i}>
            <CardHeader><Skeleton className="h-5 w-36" /></CardHeader>
            <CardContent className="space-y-3">
              {[1, 2, 3].map((j) => (
                <div key={j} className="space-y-1">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/2" />
                </div>
              ))}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

// ── Stat cards ─────────────────────────────────────────────────

function StatCards({
  noteCount,
  eventCount,
  taskPct,
}: {
  noteCount: number;
  eventCount: number;
  taskPct: number;
}) {
  const taskLabel = `${String(taskPct)}%`;

  const stats = [
    { icon: StickyNoteIcon, value: String(noteCount), label: 'Total Notes', color: 'text-primary' },
    { icon: CalendarCheck2Icon, value: String(eventCount), label: 'Upcoming Events', color: 'text-secondary' },
    { icon: ListChecksIcon, value: taskLabel, label: 'Tasks Complete', color: 'text-primary' },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {stats.map((stat) => (
        <Card key={stat.label} size="sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <stat.icon className={`size-4 ${stat.color}`} />
              {stat.label}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-2xl font-bold tracking-tight">{stat.value}</span>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

// ── Quick actions ──────────────────────────────────────────────

function QuickActions() {
  const router = useRouter();

  const handleNavigate = (to: string, search?: Record<string, unknown>) => {
    void router.navigate({ to, search });
  };

  return (
    <div className="flex flex-wrap gap-2">
      <Button variant="default" size="sm" onClick={() => { handleNavigate('/notes', { action: 'create', type: 'note' }); }}>
        <SquarePenIcon data-icon="inline-start" />New Note
      </Button>
      <Button variant="outline" size="sm" onClick={() => { handleNavigate('/calendar', { action: 'create' }); }}>
        <PlusCircleIcon data-icon="inline-start" />Add Event
      </Button>
      <Button variant="outline" size="sm" onClick={() => { handleNavigate('/notes', { action: 'create', type: 'checklist' }); }}>
        <CheckSquareIcon data-icon="inline-start" />New Task
      </Button>
      <Button variant="secondary" size="sm" onClick={() => { handleNavigate('/chat', {}); }}>
        <BotIcon data-icon="inline-start" />Ask AI
      </Button>
    </div>
  );
}

// ── Events Widget ──────────────────────────────────────────────

function EventsWidget({ events }: { events: { id: number; title: string; time: string; dateStr?: string }[] }) {
  const router = useRouter();
  return (
    <Card>
      <CardHeader><CardTitle>Upcoming Events</CardTitle></CardHeader>
      <CardContent>
        {events.length === 0 ? (
          <Empty>
            <EmptyMedia>
              <CalendarCheck2Icon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>No upcoming events</EmptyTitle>
              <EmptyDescription>Your schedule is clear. Add an event to get started.</EmptyDescription>
              <Button size="sm" className="mt-2" onClick={() => { void router.navigate({ to: '/calendar', search: { action: 'create' } }); }}>
                <PlusCircleIcon data-icon="inline-start" />Add Event
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="space-y-3">
            {events.map((event) => (
              <div key={event.id} className="flex cursor-pointer items-center gap-3 rounded-lg border border-transparent p-2 transition-colors hover:border-border hover:bg-muted/50 -mx-2"
                onClick={() => {
                  void router.navigate({ to: '/calendar', search: event.dateStr ? { date: event.dateStr } : {} });
                }}>
                <div className="size-2 shrink-0 rounded-full bg-primary" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{event.title}</p>
                  <p className="text-xs text-muted-foreground">{event.time}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

// ── Notes Widget ───────────────────────────────────────────────

interface RecentItem {
  id: number;
  title: string;
  snippet: string;
  type: 'note' | 'checklist';
  createdAt: string;
}

function NotesWidget({ items }: { items: RecentItem[] }) {
  const router = useRouter();

  const openDetail = (item: RecentItem) => {
    // Pass detailId as string to ensure TanStack Router serializes it correctly
    void router.navigate({
      to: '/notes',
      search: { detailId: String(item.id), detailType: item.type },
    });
  };

  return (
    <Card>
      <CardHeader><CardTitle>Recent Notes</CardTitle></CardHeader>
      <CardContent>
        {items.length === 0 ? (
          <Empty>
            <EmptyMedia>
              <StickyNoteIcon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>No notes yet</EmptyTitle>
              <EmptyDescription>Create your first note to start tracking ideas.</EmptyDescription>
              <Button size="sm" className="mt-2" onClick={() => { void router.navigate({ to: '/notes', search: { action: 'create' } }); }}>
                <SquarePenIcon data-icon="inline-start" />New Note
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="space-y-1">
            {items.map((item) => (
              <div key={`${item.type}-${String(item.id)}`}
                className="group cursor-pointer rounded-lg border border-transparent p-2 transition-colors hover:border-border hover:bg-muted/50 -mx-2"
                onClick={() => { openDetail(item); }}>
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm font-medium truncate">{item.title}</p>
                  <Badge variant={item.type === 'checklist' ? 'secondary' : 'default'} className="shrink-0 text-[10px] px-1.5 py-0">
                    {item.type === 'checklist' ? 'Checklist' : 'Note'}
                  </Badge>
                </div>
                <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{item.snippet}</p>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

// ── Helpers ────────────────────────────────────────────────────

function getTimeStr(isoString: string | undefined): string {
  if (!isoString) return '';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
  } catch {
    return '';
  }
}

function isUpcoming(isoString: string | undefined): boolean {
  if (!isoString) return false;
  try {
    const eventDate = new Date(isoString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return eventDate >= today;
  } catch {
    return false;
  }
}

// ── Page component ─────────────────────────────────────────────

export function Home() {
  const { data: apiNotes } = useSuspenseQuery(notesQueries.all());
  const { data: apiChecklists } = useSuspenseQuery(checklistQueries.all());
  const { data: apiEvents } = useSuspenseQuery(calendarQueries.all());

  // Stat cards
  const noteCount = apiNotes.length;
  const eventCount = useMemo(() => {
    return apiEvents.filter((ev) => isUpcoming(ev.startTime)).length;
  }, [apiEvents]);

  const taskPct = useMemo(() => {
    const items = apiChecklists.flatMap((cl) => cl.items ?? []);
    if (items.length === 0) return 0;
    const done = items.filter((item) => item.completed).length;
    return Math.round((done / items.length) * 100);
  }, [apiChecklists]);

  // Upcoming events for widget
  const upcomingEvents = useMemo(() => {
    return apiEvents
      .filter((ev) => isUpcoming(ev.startTime))
      .sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
      .slice(0, 5)
      .map((ev) => ({
        id: ev.id,
        title: ev.title ?? '',
        time: getTimeStr(ev.startTime),
        dateStr: ev.startTime ? ev.startTime.slice(0, 10) : undefined,
      }));
  }, [apiEvents]);

  // Recent items for notes widget
  const recentItems = useMemo(() => {
    const notes: RecentItem[] = apiNotes.map((n) => ({
      id: n.id,
      title: n.title,
      snippet: n.content,
      type: 'note' as const,
      createdAt: n.createdAt,
    }));
    const checklists: RecentItem[] = apiChecklists.map((cl) => ({
      id: cl.id ?? 0,
      title: cl.title ?? '',
       snippet: `${String((cl.items ?? []).filter((i) => i.completed).length)}/${String((cl.items ?? []).length)} tasks completed`,
      type: 'checklist' as const,
      createdAt: cl.createdAt ?? '',
    }));
    return [...notes, ...checklists]
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 5);
  }, [apiNotes, apiChecklists]);

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <GreetingSection />

      <div className="mt-6">
        <StatCards noteCount={noteCount} eventCount={eventCount} taskPct={taskPct} />
      </div>

      <div className="mt-6">
        <QuickActions />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <EventsWidget events={upcomingEvents} />
        <NotesWidget items={recentItems} />
      </div>
    </div>
  );
}
