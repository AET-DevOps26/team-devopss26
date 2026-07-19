import { useState, useMemo, useRef } from 'react';
import { createFileRoute, useSearch, useRouter } from '@tanstack/react-router';
import {
  useSuspenseQuery,
  useMutation,
  useQueryClient,
  useQueryErrorResetBoundary,
  queryOptions,
} from '@tanstack/react-query';
import { toast } from 'sonner';
import { Card, CardContent } from '#/components/ui/card.tsx';
import { Button } from '#/components/ui/button.tsx';
import { Input } from '#/components/ui/input.tsx';
import { Empty, EmptyTitle, EmptyDescription, EmptyMedia, EmptyContent } from '#/components/ui/empty.tsx';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '#/components/ui/sheet.tsx';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  PlusIcon,
  Trash2Icon,
  CalendarDaysIcon,
  AlertCircleIcon,
} from 'lucide-react';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { getEvents, createEvent, updateEvent, deleteEvent } from '#/services/calendar/calendar-events/calendar-events.ts';
import type { IdentifiedCalendarEvent, CreateCalendarEventRequest } from '#/types/calendar';

interface CalendarSearch {
  action?: 'create';
  date?: string;
}

export const Route = createFileRoute('/_authenticated/calendar/')({
  validateSearch: (input: Record<string, unknown>): CalendarSearch => ({
    action: input.action === 'create' ? 'create' : undefined,
    date: typeof input.date === 'string' ? input.date : undefined,
  }),
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(calendarQueries.all()),
  pendingComponent: CalendarSkeleton,
  errorComponent: CalendarError,
  component: CalendarPage,
});

/**
 * Query key factory for calendar data.
 *
 * - `calendarKeys.all` — root key for broad invalidation.
 * - `calendarKeys.events()` — scoped to events list; mutations invalidate this
 *   key so the grid refetches after create/update/delete.
 */
const calendarKeys = {
  all: ['calendar'] as const,
  events: () => [...calendarKeys.all, 'events'] as const,
};

/**
 * **staleTime: 30_000** — avoids refetching when navigating back within 30s, while
 * keeping the grid reasonably up-to-date without manual refresh.
 */
const calendarQueries = {
  all: () =>
    queryOptions({
      queryKey: calendarKeys.events(),
      queryFn: async () => {
        const response = await getEvents();
        return response.events;
      },
      staleTime: 30_000,
    }),
};

/** YYYY-MM-DD in local timezone — the API stores the date portion in local time. */
function localDateStr(date: Date): string {
  return `${String(date.getFullYear())}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const today = new Date();
const todayStr = localDateStr(today);

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/**
 * Number of days in a month. Uses JS date overflow trick.
 */
function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate();
}

/** Weekday index (0=Sun) of the first day. */
function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay(); // 0 = Sun
}

/**
 * Extract the date portion (YYYY-MM-DD) from an ISO 8601 datetime string.
 * Returns empty string for undefined/null values so callers can skip rendering.
 *
 * Used as the canonical date key for grouping and comparing events across the calendar grid.
 */
function getDateStr(isoString: string | undefined): string {
  if (!isoString) return '';
  return isoString.slice(0, 10);
}

/**
 * Extract the time portion (HH:MM) from an ISO 8601 datetime string.
 * Avoids timezone parsing overhead by simple string slicing — the API always
 * returns UTC timestamps, and the time portion is timezone-invariant.
 */
function getTimeStr(isoString: string | undefined): string {
  if (!isoString) return '';
  return isoString.slice(11, 16);
}

/**
 * Format an ISO 8601 datetime to a localized time string (HH:MM) for display.
 * Uses `de-DE` locale (24-hour format) and explicitly reads UTC hours so the
 * displayed time matches what was stored, regardless of the viewer's timezone.
 */
function formatTime(isoString: string | undefined): string {
  if (!isoString) return '';
  return new Intl.DateTimeFormat('de-DE', {
    hour: '2-digit', minute: '2-digit', timeZone: 'UTC',
  }).format(new Date(isoString));
}

/**
 * Format an ISO 8601 datetime to a localized date string (e.g. "15.07.2026").
 * Uses `de-DE` locale and forces UTC to avoid timezone drift from the stored value.
 */
function formatDate(isoString: string | undefined): string {
  if (!isoString) return '';
  return new Intl.DateTimeFormat('de-DE', { timeZone: 'UTC', day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(isoString));
}

/**
 * Convert an HH:MM string to total minutes since midnight. Used for time-range
 * validation (start must be before end) and for computing the auto-adjusted
 * end time in the event form.
 */
function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

/**
 * Add exactly one hour to an HH:MM string, wrapping at 24 hours.
 * Used when the user sets a start time past the current end time, triggering an
 * automatic end-time adjustment so the duration stays at least 1 hour.
 *
 * Example: `"14:30"` → `"15:30"`, `"23:00"` → `"00:00"`
 */
function addHour(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number);
  const total = h * 60 + m + 60;
  return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

/** Convert YYYY-MM-DD to DD.MM.YYYY for localized display. */
function toDisplayDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate);
  if (!match) return isoDate;
  return `${match[3]}.${match[2]}.${match[1]}`;
}

/** Convert DD.MM.YYYY back to YYYY-MM-DD for internal state / API. */
function fromDisplayDate(dmy: string): string {
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(dmy);
  if (!match) return '';
  return `${match[3]}-${match[2]}-${match[1]}`;
}

/**
 * Local form state for event create/edit inside the Sheet.
 *
 * - `id` — set for edits, undefined for new events.
 * - `date` — YYYY-MM-DD format (parsed from the user-facing DD.MM.YYYY display).
 * - `startTime` / `endTime` — HH:MM strings, kept in 24h format internally.
 *   The form auto-normalizes user input (e.g. `"9:5"` → `"09:05"`) on blur.
 */
interface CalendarFormEvent {
  id?: number;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  description?: string;
}

/**
 * Convert local form state into the API request shape.
 * Combines the separate `date` + `startTime` / `endTime` fields back into
 * full ISO 8601 UTC timestamps expected by the backend.
 */
function toApiEvent(form: CalendarFormEvent): CreateCalendarEventRequest {
  return {
    title: form.title,
    startTime: `${form.date}T${form.startTime}:00Z`,
    endTime: `${form.date}T${form.endTime}:00Z`,
    description: form.description ?? undefined,
  };
}

/**
 * Convert an API event into local form state.
 * Splits the ISO 8601 timestamp into separate date/time fields for the form controls,
 * and provides empty-string defaults so React controlled inputs never receive `null`.
 */
function fromApiEvent(event: IdentifiedCalendarEvent): CalendarFormEvent {
  return {
    id: event.id,
    title: event.title ?? '',
    date: getDateStr(event.startTime),
    startTime: getTimeStr(event.startTime),
    endTime: getTimeStr(event.endTime),
    description: event.description ?? '',
  };
}

/**
 * Error-state fallback for the calendar route.
 *
 * Displays the error message from the failed query and a "Try Again" button
 * that resets both the TanStack Query error boundary (`useQueryErrorResetBoundary`)
 * and the route-level error boundary (`reset()` from the `errorComponent` props).
 * This two-reset pattern is required because `errorComponent` wraps the route
 * element, so retrying needs to clear both layers.
 */
function CalendarError({ error, reset }: { error: Error; reset: () => void }) {
  const { reset: resetQuery } = useQueryErrorResetBoundary();

  const handleRetry = () => {
    resetQuery();
    reset();
  };

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center" role="alert">
      <AlertCircleIcon className="size-10 text-destructive" />
      <h2 className="text-xl font-bold tracking-tight">Failed to load events</h2>
      <p className="max-w-sm text-sm text-muted-foreground">
        {error.message || 'Something went wrong while loading your calendar.'}
      </p>
      <Button onClick={handleRetry}>Try Again</Button>
    </div>
  );
}

/**
 * Slide-over sheet for creating or editing a calendar event.
 *
 * Manages local form state (title, date, time range, description) and delegates
 * persistence to three mutations (create, update, delete). The form is
 * **key-remounted** by `CalendarPage` via `key={editingEvent?.id ?? 'create'}`,
 * so the form state resets naturally when switching between events — no manual
 * reset effect needed.
 *
 * **Validation rules:**
 * - Title must be non-empty (after trim).
 * - Date must parse to a valid YYYY-MM-DD string.
 * - End time must be strictly after start time (compared in minutes).
 * - All buttons are disabled while any mutation is pending.
 *
 * **Time input normalization:** On blur, bare hour values like `"9"` or `"9:5"`
 * are padded to `"09:00"` / `"09:05"`. If the adjusted start time equals or
 * exceeds the end time, the end time auto-increments by 1 hour.
 *
 * **Cache invalidation:** Every successful mutation invalidates `calendarKeys.events()`,
 * causing the calendar grid to refetch and re-render.
 */
function EventSheet({
  event,
  isOpen,
  onOpenChange,
}: {
  event: CalendarFormEvent | null;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [title, setTitle] = useState(event?.title ?? '');
  const [dateDisplay, setDateDisplay] = useState(toDisplayDate(event?.date ?? todayStr));
  const [startTime, setStartTime] = useState(event?.startTime ?? '09:00');
  const [endTime, setEndTime] = useState(event?.endTime ?? '10:00');
  const [description, setDescription] = useState(event?.description ?? '');
  const queryClient = useQueryClient();
  const datePickerRef = useRef<HTMLInputElement>(null);

  // Derive YYYY-MM-DD from display value; empty if invalid
  const parsedDate = fromDisplayDate(dateDisplay);
  const isTimeValid = startTime.length === 5 && endTime.length === 5 && toMinutes(startTime) < toMinutes(endTime);

  /**
   * Create mutation — POSTs a new event to the API.
   * On success: toast confirmation + invalidate event list cache.
   */
  const createMutation = useMutation({
    mutationFn: (form: CalendarFormEvent) => createEvent(toApiEvent(form)),
    onError: () => toast.error('Failed to create event'),
    onSuccess: () => {
      toast.success('Event created');
      void queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });

  /**
   * Update mutation — PUTs changes to an existing event by ID.
   * Shares the same cache invalidation as create. The `id` is the numeric
   * event identifier from the API (required to exist in the backend).
   */
  const updateMutation = useMutation({
    mutationFn: ({ id, form }: { id: number; form: CalendarFormEvent }) =>
      updateEvent(id, toApiEvent(form)),
    onError: () => toast.error('Failed to update event'),
    onSuccess: () => {
      toast.success('Event updated');
      void queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });

  /**
   * Delete mutation — DELETEs an event by its numeric ID.
   * Shows a confirmation toast on success. The delete button is only rendered
   * in edit mode (when the event has an `id`).
   */
  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteEvent(id),
    onError: () => toast.error('Failed to delete event'),
    onSuccess: () => {
      toast.success('Event deleted');
      void queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });

  const isPending = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  // Reset form when sheet opens with new event
  const handleOpenChange = (open: boolean) => {
    if (open) {
      setTitle(event?.title ?? '');
      setDateDisplay(toDisplayDate(event?.date ?? todayStr));
      setStartTime(event?.startTime ?? '09:00');
      setEndTime(event?.endTime ?? '10:00');
      setDescription(event?.description ?? '');
    }
    onOpenChange(open);
  };

  /* Form sync is handled by key-based remount of EventSheet in CalendarPage */

  const handleSave = () => {
    if (!title.trim() || !parsedDate || !isTimeValid || isPending) return;

    const form: CalendarFormEvent = {
      id: event?.id,
      title: title.trim(),
      date: parsedDate,
      startTime,
      endTime,
      description: description.trim() || undefined,
    };

    if (event?.id !== undefined) {
      updateMutation.mutate({ id: event.id, form });
    } else {
      createMutation.mutate(form);
    }
    onOpenChange(false);
  };

  const handleDelete = () => {
    if (event?.id && !isPending) {
      deleteMutation.mutate(event.id);
      onOpenChange(false);
    }
  };

  return (
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle>{event?.id ? 'Edit Event' : 'New Event'}</SheetTitle>
          <SheetDescription>
            {event?.id ? 'Update the event details.' : 'Fill in the event details.'}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 space-y-4 p-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Title</label>
            <Input value={title} onChange={(e) => { setTitle(e.target.value); }} placeholder="Event title" />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Date</label>
            <div className="relative">
              <Input
                inputMode="numeric"
                placeholder="DD.MM.YYYY"
                value={dateDisplay}
                onChange={(e) => { setDateDisplay(e.target.value); }}
                onBlur={() => {
                  const parsed = fromDisplayDate(dateDisplay);
                  if (parsed) setDateDisplay(toDisplayDate(parsed));
                }}
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => { datePickerRef.current?.showPicker(); }}
                className="absolute right-1 top-1/2 -translate-y-1/2 flex size-7 items-center justify-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
                tabIndex={-1}
              >
                <CalendarDaysIcon className="size-4" />
              </button>
              <input
                ref={datePickerRef}
                type="date"
                className="sr-only"
                value={parsedDate}
                tabIndex={-1}
                onChange={(e) => { setDateDisplay(toDisplayDate(e.target.value)); }}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Start</label>
              <Input
                inputMode="numeric"
                placeholder="HH:MM"
                value={startTime}
                onChange={(e) => { setStartTime(e.target.value); }}
                onBlur={(e) => {
                  const v = e.target.value;
                  const m = /^(\d{1,2}):?(\d{0,2})$/.exec(v);
                  if (m) {
                    const norm = `${m[1].padStart(2, '0')}:${(m[2] || '00').padStart(2, '0')}`;
                    setStartTime(norm);
                    if (toMinutes(norm) >= toMinutes(endTime)) {
                      setEndTime(addHour(norm));
                    }
                  }
                }}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">End</label>
              <Input
                inputMode="numeric"
                placeholder="HH:MM"
                value={endTime}
                onChange={(e) => { setEndTime(e.target.value); }}
                onBlur={(e) => {
                  const v = e.target.value;
                  const m = /^(\d{1,2}):?(\d{0,2})$/.exec(v);
                  if (m) {
                    setEndTime(`${m[1].padStart(2, '0')}:${(m[2] || '00').padStart(2, '0')}`);
                  }
                }}
              />
            </div>
          </div>
          {!isTimeValid && (
            <p className="text-xs text-destructive" role="alert">Start time must be before end time.</p>
          )}
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Description (optional)</label>
            <Input value={description} onChange={(e) => { setDescription(e.target.value); }} placeholder="Add description..." />
          </div>
        </div>

        <SheetFooter className="flex-row gap-2 p-4">
          {event?.id && (
            <Button
              variant="destructive"
              size="sm"
              onClick={handleDelete}
              disabled={isPending}
              className="mr-auto"
            >
              <Trash2Icon data-icon="inline-start" />Delete
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={() => { onOpenChange(false); }} disabled={isPending}>Cancel</Button>
          <Button size="sm" onClick={handleSave} disabled={!title.trim() || !parsedDate || !isTimeValid || isPending}>
            {isPending ? 'Saving...' : event?.id ? 'Update' : 'Create'}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

/**
 * Loading skeleton for the calendar route (rendered as `pendingComponent`).
 * Mirrors the layout of the real calendar grid — month nav, weekday headers,
 * and 35 cell placeholders — so the user sees a stable layout while the
 * events query loads. Each cell shows a circular skeleton placeholder for the
 * day number. Uses `aria-busy="true" to signal the loading state to assistive
 * technology.
 */
function CalendarSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8" aria-busy="true" aria-label="Loading calendar">
      <div className="flex items-center justify-between mb-6">
        <Skeleton className="h-9 w-24" />
        <Skeleton className="h-9 w-20" />
      </div>
      <div className="flex items-center justify-between mb-4">
        <Skeleton className="size-9 rounded-md" />
        <Skeleton className="h-7 w-40" />
        <Skeleton className="size-9 rounded-md" />
      </div>
      <Card>
        <CardContent className="p-0">
          <div className="grid grid-cols-7 border-b">
            {WEEKDAYS.map((d) => (
              <div key={d} className="p-2 text-center text-xs font-medium text-muted-foreground">{d}</div>
            ))}
          </div>
          <div className="grid grid-cols-7">
            {Array.from({ length: 35 }).map((_, i) => (
              <div key={i} className="min-h-[60px] border-b border-r p-1.5 sm:min-h-[80px] sm:p-2">
                <Skeleton className="size-6 rounded-full" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * Main calendar page component.
 *
 * Renders a monthly grid with navigation, an event list for the selected day,
 * and a slide-over sheet for create/edit operations.
 *
 * **State management:**
 * - `currentDate` — the month/year shown in the grid (day is always 1).
 * - `selectedDate` — the YYYY-MM-DD string of the currently highlighted day.
 * - `sheetOpen` / `editingEvent` — controls the event form sheet. The sheet is
 *   key-remounted when the editing event changes (or switches to create mode),
 *   so local form state resets automatically.
 *
 * **Data flow:**
 * - Events are fetched via `useSuspenseQuery` — guaranteed to have data at
 *   render time (the route's `loader` prefetches via `ensureQueryData`).
 * - `gridDays` is a memoized array of `(number | null)[]` where `null` cells
 *   are leading blanks aligning the first day to the correct weekday column.
 * - `eventsByDate` maps date strings to event lists for O(1) cell lookups.
 * - `selectedDayEvents` filters the full list for the selected date.
 *
 * **Empty states:**
 * - No events at all → "No events yet" with a create button.
 * - Events exist but none on the selected day → "Nothing scheduled" message.
 */
export function CalendarPage() {
  const routeSearch: CalendarSearch = useSearch({ from: '/_authenticated/calendar/' });
  const router = useRouter();
  const [currentDate, setCurrentDate] = useState(() => {
    if (routeSearch.date) {
      const d = new Date(routeSearch.date);
      return isNaN(d.getTime()) ? today : d;
    }
    return today;
  });
  const [sheetOpen, setSheetOpen] = useState(routeSearch.action === 'create');
  const [editingEvent, setEditingEvent] = useState<CalendarFormEvent | null>(null);
  const [selectedDate, setSelectedDate] = useState(routeSearch.date ?? todayStr);

  const { data: events, isFetching } = useSuspenseQuery(calendarQueries.all());

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // Build grid: leading blanks + day numbers
  const gridDays = useMemo(() => {
    const firstDay = getFirstDayOfMonth(year, month);
    const daysInMonth = getDaysInMonth(year, month);
    const cells: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) cells.push(d);
    return cells;
  }, [year, month]);

  // Events mapped by date string (derived from startTime ISO string)
  const eventsByDate = useMemo(() => {
    const map = new Map<string, IdentifiedCalendarEvent[]>();
    for (const ev of events) {
      const dateKey = getDateStr(ev.startTime);
      if (!dateKey) continue;
      const list = map.get(dateKey) ?? [];
      list.push(ev);
      map.set(dateKey, list);
    }
    return map;
  }, [events]);

  // Events for the selected day
  const selectedDayEvents = useMemo(() => {
    return events.filter((ev) => getDateStr(ev.startTime) === selectedDate);
  }, [events, selectedDate]);

  const navigateMonth = (delta: number) => {
    setCurrentDate(new Date(year, month + delta, 1));
  };

  const goToToday = () => {
    const now = new Date();
    const dateStr = localDateStr(now);
    setCurrentDate(now);
    setSelectedDate(dateStr);
    void router.navigate({
      to: '/calendar',
      search: { date: dateStr },
    });
  };

  const openCreateSheet = () => {
    setEditingEvent({
      title: '',
      date: selectedDate,
      startTime: '09:00',
      endTime: '10:00',
    });
    setSheetOpen(true);
    void router.navigate({
      to: '/calendar',
      search: { date: selectedDate, action: 'create' },
    });
  };

  const openEditSheet = (event: IdentifiedCalendarEvent) => {
    setEditingEvent(fromApiEvent(event));
    setSheetOpen(true);
  };

  const isToday = (day: number) => {
    const d = new Date(year, month, day);
    return localDateStr(d) === todayStr;
  };

  const handleDayClick = (day: number) => {
    const dateStr = localDateStr(new Date(year, month, day));
    setSelectedDate(dateStr);
    void router.navigate({
      to: '/calendar',
      search: { date: dateStr },
    });
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Calendar</h1>
        <div className="flex items-center gap-2">
          {isFetching && (
            <span className="text-xs text-muted-foreground">Updating...</span>
          )}
          <Button onClick={openCreateSheet}>
            <PlusIcon data-icon="inline-start" />Event
          </Button>
        </div>
      </div>

      {/* Month navigation */}
      <div className="flex items-center justify-between mb-4">
        <Button variant="ghost" size="icon-sm" onClick={() => { navigateMonth(-1); }}>
          <ChevronLeftIcon className="size-5" />
        </Button>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={goToToday}>Today</Button>
          <h2 className="text-lg font-semibold">
            {MONTHS[month]} {String(year)}
          </h2>
        </div>
        <Button variant="ghost" size="icon-sm" onClick={() => { navigateMonth(1); }}>
          <ChevronRightIcon className="size-5" />
        </Button>
      </div>

      {/* Calendar grid */}
      <Card>
        <CardContent className="p-0">
          {/* Weekday header */}
          <div className="grid grid-cols-7 border-b">
            {WEEKDAYS.map((d) => (
              <div key={d} className="p-2 text-center text-xs font-medium text-muted-foreground">
                {d}
              </div>
            ))}
          </div>
          {/* Day cells */}
          <div className="grid grid-cols-7">
            {gridDays.map((day, i) => {
              const dateStr = day
                ? `${String(year)}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
                : '';
              const dayEvents = dateStr ? eventsByDate.get(dateStr) ?? [] : [];
              const isCurrent = day !== null && isToday(day);

              const isSelected = day !== null && dateStr === selectedDate;

              return (
                <div
                  key={i}
                  onClick={() => { if (day) handleDayClick(day); }}
                  className={`relative min-h-[60px] border-b border-r p-1.5 text-sm transition-colors sm:min-h-[80px] sm:p-2 ${
                    isCurrent
                      ? 'bg-primary/10 ring-1 ring-inset ring-primary'
                      : isSelected
                        ? 'bg-muted ring-1 ring-inset ring-muted-foreground/30 hover:bg-accent'
                        : 'hover:bg-accent/50'
                  } ${!day ? 'bg-muted/20' : day ? 'cursor-pointer' : ''}`}
                >
                  {day && (
                    <>
                      <span className={`inline-flex size-6 items-center justify-center rounded-full text-xs ${
                        isCurrent ? 'bg-primary text-primary-foreground font-bold' : ''
                      }`}>
                        {day}
                      </span>
                      {dayEvents.length > 0 && (
                        <div className="mt-1 flex flex-wrap gap-0.5">
                          {dayEvents.slice(0, 3).map((ev) => (
                            <div key={ev.id} className="size-1.5 rounded-full bg-primary" />
                          ))}
                          {dayEvents.length > 3 && (
                            <span className="text-[10px] text-muted-foreground">+{dayEvents.length - 3}</span>
                          )}
                        </div>
                      )}
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Selected day's events list */}
      <div className="mt-6">
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          Events for {selectedDate === todayStr ? 'Today' : formatDate(selectedDate)}
        </h3>

        {events.length === 0 ? (
          <Empty>
            <EmptyMedia>
              <CalendarDaysIcon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>No events yet</EmptyTitle>
              <EmptyDescription>Create your first event to get started.</EmptyDescription>
              <Button className="mt-2" onClick={openCreateSheet}>
                <PlusIcon data-icon="inline-start" />Create Event
              </Button>
            </EmptyContent>
          </Empty>
        ) : selectedDayEvents.length === 0 ? (
          <Empty>
            <EmptyMedia>
              <CalendarDaysIcon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>Nothing scheduled{selectedDate === todayStr ? ' today' : ''}</EmptyTitle>
              <EmptyDescription>Click +Event to create one.</EmptyDescription>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="space-y-2">
            {selectedDayEvents.map((ev) => (
              <div
                key={ev.id}
                className="flex cursor-pointer items-center gap-3 rounded-lg border p-3 transition-colors hover:bg-accent/50 hover:border-ring/30"
                onClick={() => { openEditSheet(ev); }}
              >
                <div className="flex flex-col items-center text-xs">
                  <span className="font-medium text-primary">{formatTime(ev.startTime)}</span>
                  <span className="text-muted-foreground">-</span>
                  <span className="text-muted-foreground">{formatTime(ev.endTime)}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{ev.title}</p>
                  {ev.description && (
                    <p className="text-xs text-muted-foreground truncate">{ev.description}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Event create/edit sheet */}
      <EventSheet
        key={editingEvent?.id ?? 'create'}
        event={editingEvent}
        isOpen={sheetOpen}
        onOpenChange={(open) => {
          setSheetOpen(open);
          if (!open) {
            void router.navigate({
              to: '/calendar',
              search: { date: selectedDate, action: undefined },
            });
          }
        }}
      />
    </div>
  );
}
