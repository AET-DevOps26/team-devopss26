import { useState, useMemo, useEffect, useRef } from 'react';
import { createFileRoute } from '@tanstack/react-router';
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

export const Route = createFileRoute('/_authenticated/calendar/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData(calendarQueries.all()),
  pendingComponent: CalendarSkeleton,
  errorComponent: CalendarError,
  component: CalendarPage,
});

const calendarKeys = {
  all: ['calendar'] as const,
  events: () => [...calendarKeys.all, 'events'] as const,
};

const calendarQueries = {
  all: () =>
    queryOptions({
      queryKey: calendarKeys.events(),
      queryFn: async () => {
        const response = await getEvents();
        return response.events ?? [];
      },
      staleTime: 30_000,
    }),
};

function localDateStr(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const today = new Date();
const todayStr = localDateStr(today);

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay(); // 0 = Sun
}

function getDateStr(isoString: string | undefined): string {
  if (!isoString) return '';
  return isoString.slice(0, 10);
}

function getTimeStr(isoString: string | undefined): string {
  if (!isoString) return '';
  return isoString.slice(11, 16);
}

function formatTime(isoString: string | undefined): string {
  if (!isoString) return '';
  return new Intl.DateTimeFormat('de-DE', {
    hour: '2-digit', minute: '2-digit', timeZone: 'UTC',
  }).format(new Date(isoString));
}

function formatDate(isoString: string | undefined): string {
  if (!isoString) return '';
  return new Intl.DateTimeFormat('de-DE', { timeZone: 'UTC' }).format(new Date(isoString));
}

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

function addHour(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number);
  const total = h * 60 + m + 60;
  return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

/** Convert YYYY-MM-DD to DD.MM.YYYY for localized display. */
function toDisplayDate(isoDate: string): string {
  const match = isoDate.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return isoDate;
  return `${match[3]}.${match[2]}.${match[1]}`;
}

/** Convert DD.MM.YYYY back to YYYY-MM-DD for internal state / API. */
function fromDisplayDate(dmy: string): string {
  const match = dmy.match(/^(\d{2})\.(\d{2})\.(\d{4})$/);
  if (!match) return '';
  return `${match[3]}-${match[2]}-${match[1]}`;
}

interface CalendarFormEvent {
  id?: number;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  description?: string;
}

function toApiEvent(form: CalendarFormEvent): CreateCalendarEventRequest {
  return {
    title: form.title,
    startTime: `${form.date}T${form.startTime}:00Z`,
    endTime: `${form.date}T${form.endTime}:00Z`,
    description: form.description || undefined,
  };
}

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

  const createMutation = useMutation({
    mutationFn: (form: CalendarFormEvent) => createEvent(toApiEvent(form)),
    onError: () => toast.error('Failed to create event'),
    onSuccess: () => {
      toast.success('Event created');
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, form }: { id: number; form: CalendarFormEvent }) =>
      updateEvent(id, toApiEvent(form)),
    onError: () => toast.error('Failed to update event'),
    onSuccess: () => {
      toast.success('Event updated');
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteEvent(id),
    onError: () => toast.error('Failed to delete event'),
    onSuccess: () => {
      toast.success('Event deleted');
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
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

  // Sync form when editing event changes while sheet is open
  useEffect(() => {
    if (isOpen && event) {
      setTitle(event.title);
      setDateDisplay(toDisplayDate(event.date));
      setStartTime(event.startTime);
      setEndTime(event.endTime);
      setDescription(event.description ?? '');
    }
  }, [isOpen, event]);

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

    if (event?.id && event.id > 0) {
      updateMutation.mutate({ id: event.id, form });
    } else {
      createMutation.mutate(form);
    }
    onOpenChange(false);
  };

  const handleDelete = () => {
    if (event?.id && event.id > 0 && !isPending) {
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
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Event title" />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Date</label>
            <div className="relative">
              <Input
                inputMode="numeric"
                placeholder="DD.MM.YYYY"
                value={dateDisplay}
                onChange={(e) => setDateDisplay(e.target.value)}
                onBlur={() => {
                  const parsed = fromDisplayDate(dateDisplay);
                  if (parsed) setDateDisplay(toDisplayDate(parsed));
                }}
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => datePickerRef.current?.showPicker()}
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
                onChange={(e) => setDateDisplay(toDisplayDate(e.target.value))}
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
                onChange={(e) => setStartTime(e.target.value)}
                onBlur={(e) => {
                  const v = e.target.value;
                  const m = v.match(/^(\d{1,2}):?(\d{0,2})$/);
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
                onChange={(e) => setEndTime(e.target.value)}
                onBlur={(e) => {
                  const v = e.target.value;
                  const m = v.match(/^(\d{1,2}):?(\d{0,2})$/);
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
            <Input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Add description..." />
          </div>
        </div>

        <SheetFooter className="flex-row gap-2 p-4">
          {event?.id && event.id > 0 && (
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
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)} disabled={isPending}>Cancel</Button>
          <Button size="sm" onClick={handleSave} disabled={!title.trim() || !parsedDate || !isTimeValid || isPending}>
            {isPending ? 'Saving...' : event?.id ? 'Update' : 'Create'}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

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

export function CalendarPage() {
  const [currentDate, setCurrentDate] = useState(today);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<CalendarFormEvent | null>(null);

  const { data: events, isFetching } = useSuspenseQuery(calendarQueries.all());

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const daysInMonth = getDaysInMonth(year, month);
  const firstDay = getFirstDayOfMonth(year, month);

  // Build grid: leading blanks + day numbers
  const gridDays = useMemo(() => {
    const cells: (number | null)[] = Array(firstDay).fill(null);
    for (let d = 1; d <= daysInMonth; d++) {
      cells.push(d);
    }
    return cells;
  }, [year, month, daysInMonth, firstDay]);

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

  // Currently selected day
  const [selectedDate, setSelectedDate] = useState(todayStr);

  // Events for the selected day
  const selectedDayEvents = useMemo(() => {
    return events.filter((ev) => getDateStr(ev.startTime) === selectedDate);
  }, [events, selectedDate]);

  const navigateMonth = (delta: number) => {
    setCurrentDate(new Date(year, month + delta, 1));
  };

  const goToToday = () => {
    const now = new Date();
    setCurrentDate(now);
    setSelectedDate(localDateStr(now));
  };

  const openCreateSheet = () => {
    setEditingEvent({
      title: '',
      date: selectedDate,
      startTime: '09:00',
      endTime: '10:00',
    });
    setSheetOpen(true);
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
        <Button variant="ghost" size="icon-sm" onClick={() => navigateMonth(-1)}>
          <ChevronLeftIcon className="size-5" />
        </Button>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={goToToday}>Today</Button>
          <h2 className="text-lg font-semibold">
            {MONTHS[month]} {year}
          </h2>
        </div>
        <Button variant="ghost" size="icon-sm" onClick={() => navigateMonth(1)}>
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
                ? `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
                : '';
              const dayEvents = dateStr ? eventsByDate.get(dateStr) ?? [] : [];
              const isCurrent = day !== null && isToday(day);

              const isSelected = day !== null && dateStr === selectedDate;

              return (
                <div
                  key={i}
                  onClick={() => day && handleDayClick(day)}
                  className={`relative min-h-[60px] border-b border-r p-1.5 text-sm transition-colors sm:min-h-[80px] sm:p-2 ${
                    isCurrent
                      ? 'bg-primary/10 ring-1 ring-inset ring-primary'
                      : isSelected
                        ? 'bg-muted ring-1 ring-inset ring-muted-foreground/30'
                        : 'hover:bg-muted/50'
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
                className="flex cursor-pointer items-center gap-3 rounded-lg border p-3 transition-colors hover:border-ring/30"
                onClick={() => openEditSheet(ev)}
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
        event={editingEvent}
        isOpen={sheetOpen}
        onOpenChange={setSheetOpen}
      />
    </div>
  );
}
