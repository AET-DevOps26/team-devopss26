import { useState, useMemo } from 'react';
import { createFileRoute } from '@tanstack/react-router';
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
} from 'lucide-react';

export const Route = createFileRoute('/calendar/')({ component: CalendarPage });

// ── Types ──────────────────────────────────────────────────────

interface CalendarEvent {
  id: string;
  title: string;
  date: string; // YYYY-MM-DD
  startTime: string;
  endTime: string;
  description: string;
}

// ── Mock data ──────────────────────────────────────────────────

const today = new Date();
const todayStr = today.toISOString().slice(0, 10);

const mockEvents: CalendarEvent[] = [
  { id: 'e1', title: 'Team standup', date: todayStr, startTime: '10:00', endTime: '10:30', description: 'Daily sync with the team.' },
  { id: 'e2', title: 'Design review', date: todayStr, startTime: '14:00', endTime: '15:00', description: 'Review the new mockups.' },
  { id: 'e3', title: 'Deploy window', date: todayStr, startTime: '16:30', endTime: '17:00', description: 'Deploy to staging.' },
  { id: 'e4', title: 'Sprint planning', date: '2026-06-22', startTime: '11:00', endTime: '12:00', description: 'Plan sprint 27.' },
  { id: 'e5', title: 'Client call', date: '2026-06-25', startTime: '15:00', endTime: '16:00', description: 'Quarterly review.' },
];

// ── Helpers ────────────────────────────────────────────────────

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay(); // 0 = Sun
}

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

// ── Sub-components ─────────────────────────────────────────────

function EventSheet({
  event,
  isOpen,
  onOpenChange,
  onSave,
  onDelete,
}: {
  event: CalendarEvent | null;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (e: CalendarEvent) => void;
  onDelete: (id: string) => void;
}) {
  const [title, setTitle] = useState(event?.title ?? '');
  const [date, setDate] = useState(event?.date ?? todayStr);
  const [startTime, setStartTime] = useState(event?.startTime ?? '09:00');
  const [endTime, setEndTime] = useState(event?.endTime ?? '10:00');
  const [description, setDescription] = useState(event?.description ?? '');

  // Reset form when sheet opens with new event
  const handleOpenChange = (open: boolean) => {
    if (open) {
      setTitle(event?.title ?? '');
      setDate(event?.date ?? todayStr);
      setStartTime(event?.startTime ?? '09:00');
      setEndTime(event?.endTime ?? '10:00');
      setDescription(event?.description ?? '');
    }
    onOpenChange(open);
  };

  const handleSave = () => {
    if (!title.trim()) return;
    onSave({
      id: event?.id ?? crypto.randomUUID(),
      title: title.trim(),
      date,
      startTime,
      endTime,
      description: description.trim(),
    });
    onOpenChange(false);
  };

  return (
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle>{event ? 'Edit Event' : 'New Event'}</SheetTitle>
          <SheetDescription>
            {event ? 'Update the event details.' : 'Fill in the event details.'}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 space-y-4 p-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Title</label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Event title" />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Date</label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Start</label>
              <Input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">End</label>
              <Input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Description (optional)</label>
            <Input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Add description..." />
          </div>
        </div>

        <SheetFooter className="flex-row gap-2 p-4">
          {event && (
            <Button variant="destructive" size="sm" onClick={() => { onDelete(event.id); onOpenChange(false); }} className="mr-auto">
              <Trash2Icon data-icon="inline-start" />Delete
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="sm" onClick={handleSave} disabled={!title.trim()}>{event ? 'Update' : 'Create'}</Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

// ── Main page component ────────────────────────────────────────

function CalendarPage() {
  const [currentDate, setCurrentDate] = useState(today);
  const [events, setEvents] = useState<CalendarEvent[]>(mockEvents);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<CalendarEvent | null>(null);

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

  // Events mapped by date string
  const eventsByDate = useMemo(() => {
    const map = new Map<string, CalendarEvent[]>();
    for (const ev of events) {
      const list = map.get(ev.date) ?? [];
      list.push(ev);
      map.set(ev.date, list);
    }
    return map;
  }, [events]);

  const todayEvents = useMemo(() => {
    return events.filter((ev) => ev.date === todayStr);
  }, [events]);

  const navigateMonth = (delta: number) => {
    setCurrentDate(new Date(year, month + delta, 1));
  };

  const openCreateSheet = () => {
    setEditingEvent(null);
    setSheetOpen(true);
  };

  const openEditSheet = (event: CalendarEvent) => {
    setEditingEvent(event);
    setSheetOpen(true);
  };

  const handleSave = (event: CalendarEvent) => {
    setEvents((prev) => {
      const idx = prev.findIndex((e) => e.id === event.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = event;
        return next;
      }
      return [...prev, event];
    });
  };

  const handleDelete = (id: string) => {
    setEvents((prev) => prev.filter((e) => e.id !== id));
  };

  const isToday = (day: number) => {
    const d = new Date(year, month, day);
    return d.toISOString().slice(0, 10) === todayStr;
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Calendar</h1>
        <Button onClick={openCreateSheet}>
          <PlusIcon data-icon="inline-start" />Event
        </Button>
      </div>

      {/* Month navigation */}
      <div className="flex items-center justify-between mb-4">
        <Button variant="ghost" size="icon-sm" onClick={() => navigateMonth(-1)}>
          <ChevronLeftIcon className="size-5" />
        </Button>
        <h2 className="text-lg font-semibold">
          {MONTHS[month]} {year}
        </h2>
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
              const dateStr = day ? `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}` : '';
              const dayEvents = dateStr ? eventsByDate.get(dateStr) ?? [] : [];
              const isCurrent = day !== null && isToday(day);

              return (
                <div
                  key={i}
                  className={`relative min-h-[60px] border-b border-r p-1.5 text-sm transition-colors sm:min-h-[80px] sm:p-2 ${
                    isCurrent
                      ? 'bg-primary/10 ring-1 ring-inset ring-primary'
                      : 'hover:bg-muted/50'
                  } ${!day ? 'bg-muted/20' : ''}`}
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

      {/* Today's events list */}
      <div className="mt-6">
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">Today's Events</h3>

        {todayEvents.length === 0 ? (
          <Empty>
            <EmptyMedia>
              <CalendarDaysIcon className="size-8 text-muted-foreground" />
            </EmptyMedia>
            <EmptyContent>
              <EmptyTitle>Nothing scheduled today</EmptyTitle>
              <EmptyDescription>Your day is clear. Enjoy the free time!</EmptyDescription>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="space-y-2">
            {todayEvents.map((ev) => (
              <div
                key={ev.id}
                className="flex cursor-pointer items-center gap-3 rounded-lg border p-3 transition-colors hover:border-ring/30"
                onClick={() => openEditSheet(ev)}
              >
                <div className="flex flex-col items-center text-xs">
                  <span className="font-medium text-primary">{ev.startTime}</span>
                  <span className="text-muted-foreground">-</span>
                  <span className="text-muted-foreground">{ev.endTime}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{ev.title}</p>
                  {ev.description && (
                    <p className="text-xs text-muted-foreground truncate">{ev.description}</p>
                  )}
                </div>
                <Button variant="ghost" size="icon-sm" onClick={(e: React.MouseEvent) => { e.stopPropagation(); handleDelete(ev.id); }}>
                  <Trash2Icon className="size-4 text-muted-foreground hover:text-destructive" />
                </Button>
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
        onSave={handleSave}
        onDelete={handleDelete}
      />
    </div>
  );
}
