import type {
  CreateCalendarEventRequest,
  IdentifiedCalendarEvent,
  CalendarEvent,
} from '#/types/calendar';

export interface CalendarFormEvent {
  id?: number;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  description?: string;
}

export function toApiEvent(form: CalendarFormEvent): CreateCalendarEventRequest {
  const apiEvent: CalendarEvent & Required<Pick<CalendarEvent, 'title' | 'startTime' | 'endTime'>> = {
    title: form.title,
    startTime: `${form.date}T${form.startTime}:00Z`,
    endTime: `${form.date}T${form.endTime}:00Z`,
    description: form.description || undefined,
  };

  return apiEvent as CreateCalendarEventRequest;
}

export function fromApiEvent(event: IdentifiedCalendarEvent): CalendarFormEvent {
  const startDate = new Date(event.startTime ?? '');
  const endDate = new Date(event.endTime ?? '');

  const date = event.startTime
    ? startDate.toISOString().slice(0, 10)
    : '';

  const startTime = event.startTime
    ? startDate.toISOString().slice(11, 16)
    : '';

  const endTime = event.endTime
    ? endDate.toISOString().slice(11, 16)
    : '';

  return {
    id: event.id,
    title: event.title ?? '',
    date,
    startTime,
    endTime,
    description: event.description ?? '',
  };
}
