import { useState } from 'react';
import { createFileRoute, useRouter } from '@tanstack/react-router';
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
  RefreshCwIcon,
} from 'lucide-react';

export const Route = createFileRoute('/_authenticated/')({ component: Home });

// ── Mock data ──────────────────────────────────────────────────

const mockNotes = [
  { id: '1', title: 'Project setup notes', snippet: 'Steps to initialize the monorepo with pnpm workspaces...', updatedAt: '2h ago', type: 'note' as const },
  { id: '2', title: 'Sprint review todos', snippet: 'Items to discuss: API rate limiting, caching strategy, error handling...', updatedAt: '5h ago', type: 'checklist' as const },
  { id: '3', title: 'Design system reference', snippet: 'Color tokens: oklch green palette, spacing scale, typography...', updatedAt: '1d ago', type: 'note' as const },
];

const mockEvents = [
  { id: '1', title: 'Team standup', time: '10:00 AM', type: 'meeting' as const },
  { id: '2', title: 'Design review', time: '2:00 PM', type: 'review' as const },
  { id: '3', title: 'Deploy window', time: '4:30 PM', type: 'deploy' as const },
];

type WidgetState = 'populated' | 'empty' | 'loading' | 'error';

// ── Sub-components ─────────────────────────────────────────────

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

function StatCards() {
  const stats = [
    { icon: StickyNoteIcon, value: '12', label: 'Total Notes', color: 'text-primary' },
    { icon: CalendarCheck2Icon, value: '3', label: 'Upcoming Events', color: 'text-secondary' },
    { icon: ListChecksIcon, value: '67%', label: 'Tasks Complete', color: 'text-primary' },
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

function StatCardsSkeleton() {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {[1, 2, 3].map((i) => (
        <Card key={i} size="sm">
          <CardHeader>
            <Skeleton className="h-4 w-24" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-8 w-16" />
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

function QuickActions() {
  const router = useRouter();
  const actions = [
    { icon: SquarePenIcon, label: 'New Note', variant: 'default' as const, to: '/notes' as const, search: { action: 'create' as const, type: 'note' as const } },
    { icon: PlusCircleIcon, label: 'Add Event', variant: 'outline' as const, to: '/calendar' as const, search: { action: 'create' as const } },
    { icon: CheckSquareIcon, label: 'New Task', variant: 'outline' as const, to: '/notes' as const, search: { action: 'create' as const, type: 'checklist' as const } },
    { icon: BotIcon, label: 'Ask AI', variant: 'secondary' as const, to: '/chat' as const, search: {} },
  ];

  return (
    <div className="flex flex-wrap gap-2">
      {actions.map((action) => (
        <Button key={action.label} variant={action.variant} size="sm" onClick={() => { router.navigate({ to: action.to, search: action.search }); }}>
          <action.icon data-icon="inline-start" />
          {action.label}
        </Button>
      ))}
    </div>
  );
}

function EventsWidget({ state }: { state: WidgetState }) {
  if (state === 'loading') {
    return (
      <Card>
        <CardHeader><CardTitle>Upcoming Events</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center gap-3">
              <Skeleton className="size-2 rounded-full" />
              <div className="flex-1 space-y-1">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/4" />
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (state === 'empty') {
    return (
      <Card>
        <CardHeader><CardTitle>Upcoming Events</CardTitle></CardHeader>
        <CardContent>
          <Empty>
            <EmptyMedia>
              <CalendarCheck2Icon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>No upcoming events</EmptyTitle>
              <EmptyDescription>Your schedule is clear. Add an event to get started.</EmptyDescription>
              <Button size="sm" className="mt-2"><PlusCircleIcon data-icon="inline-start" />Add Event</Button>
            </EmptyContent>
          </Empty>
        </CardContent>
      </Card>
    );
  }

  if (state === 'error') {
    return (
      <Card>
        <CardHeader><CardTitle>Upcoming Events</CardTitle></CardHeader>
        <CardContent>
          <Empty>
            <EmptyMedia>
              <AlertCircleIcon className="size-8 text-destructive" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>Failed to load events</EmptyTitle>
              <EmptyDescription>Something went wrong. Please try again.</EmptyDescription>
              <Button size="sm" variant="outline" className="mt-2">
                <RefreshCwIcon data-icon="inline-start" />Try Again
              </Button>
            </EmptyContent>
          </Empty>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader><CardTitle>Upcoming Events</CardTitle></CardHeader>
      <CardContent>
        <div className="space-y-3">
          {mockEvents.map((event) => (
            <div key={event.id} className="flex items-center gap-3">
              <div className="size-2 rounded-full bg-primary" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{event.title}</p>
                <p className="text-xs text-muted-foreground">{event.time}</p>
              </div>
              <Badge variant="outline" className="shrink-0">{event.type}</Badge>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function NotesWidget({ state }: { state: WidgetState }) {
  if (state === 'loading') {
    return (
      <Card>
        <CardHeader><CardTitle>Recent Notes</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="space-y-1">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-3 w-1/3" />
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (state === 'empty') {
    return (
      <Card>
        <CardHeader><CardTitle>Recent Notes</CardTitle></CardHeader>
        <CardContent>
          <Empty>
            <EmptyMedia>
              <StickyNoteIcon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>No notes yet</EmptyTitle>
              <EmptyDescription>Create your first note to start tracking ideas.</EmptyDescription>
              <Button size="sm" className="mt-2"><SquarePenIcon data-icon="inline-start" />New Note</Button>
            </EmptyContent>
          </Empty>
        </CardContent>
      </Card>
    );
  }

  if (state === 'error') {
    return (
      <Card>
        <CardHeader><CardTitle>Recent Notes</CardTitle></CardHeader>
        <CardContent>
          <Empty>
            <EmptyMedia>
              <AlertCircleIcon className="size-8 text-destructive" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>Failed to load notes</EmptyTitle>
              <EmptyDescription>Something went wrong. Please try again.</EmptyDescription>
              <Button size="sm" variant="outline" className="mt-2">
                <RefreshCwIcon data-icon="inline-start" />Try Again
              </Button>
            </EmptyContent>
          </Empty>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader><CardTitle>Recent Notes</CardTitle></CardHeader>
      <CardContent>
        <div className="space-y-3">
          {mockNotes.map((note) => (
            <div key={note.id} className="group cursor-pointer rounded-lg border border-transparent p-2 transition-colors hover:border-border hover:bg-muted/50 -mx-2">
              <div className="flex items-start justify-between gap-2">
                <p className="text-sm font-medium truncate">{note.title}</p>
                <Badge variant={note.type === 'checklist' ? 'secondary' : 'default'} className="shrink-0 text-[10px] px-1.5 py-0">
                  {note.type === 'checklist' ? 'Checklist' : 'Note'}
                </Badge>
              </div>
              <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{note.snippet}</p>
              <p className="mt-0.5 text-[10px] text-muted-foreground/60">{note.updatedAt}</p>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

// ── State toggle demo ──────────────────────────────────────────

type DemoState = 'populated' | 'empty' | 'loading' | 'error';

// ── Page component ─────────────────────────────────────────────

export function Home() {
  const [statState] = useState<DemoState>('populated');
  const [eventsState, setEventsState] = useState<WidgetState>('populated');
  const [notesState, setNotesState] = useState<WidgetState>('populated');

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <GreetingSection />

      <div className="mt-6">
        {statState === 'loading' ? <StatCardsSkeleton /> : <StatCards />}
      </div>

      <div className="mt-6">
        <QuickActions />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <EventsWidget state={eventsState} />
        <NotesWidget state={notesState} />
      </div>

      {/* State toggle controls (remove when wiring real data) */}
      <div className="mt-8 flex flex-wrap items-center gap-2 border-t pt-4">
        <span className="text-xs text-muted-foreground">Widget state demo:</span>
        {(['populated', 'loading', 'empty', 'error'] as const).map((s) => (
          <Button key={s} size="xs" variant={eventsState === s ? 'default' : 'outline'} onClick={() => { setEventsState(s); setNotesState(s); }}>
            {s}
          </Button>
        ))}
      </div>
    </div>
  );
}
