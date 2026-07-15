import type {CreateCalendarEventRequest, IdentifiedCalendarEvent,} from '#/types/calendar';

/** Calendar event form shape. Separates date/time into discrete fields for form binding,
 * intentionally different from the API's combined ISO 8601 format.
 */
export interface CalendarFormEvent {
  id?: number;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  description?: string;
}

/** Convert form data to API ISO 8601 format (`YYYY-MM-DDTHH:mm:00Z`).
 *
 * Timezone note: `Z` suffix marks these as UTC. Round-tripping preserves clock
 * values for UTC users but may shift for others — known simplification that
 * avoids a full timezone library.
 *
 * @param form - Form data with separated date/time fields
 * @returns API-ready event request with ISO 8601 UTC timestamps
 */
export function toApiEvent(form: CalendarFormEvent): CreateCalendarEventRequest {
  return {
    title: form.title,
    startTime: `${form.date}T${form.startTime}:00Z`,
    endTime: `${form.date}T${form.endTime}:00Z`,
    // eslint-disable-next-line @typescript-eslint/prefer-nullish-coalescing -- need to coerce '' to undefined
    description: form.description || undefined,
  };
}

/** Convert API event (ISO 8601) back to form-friendly date/time parts.
 * Both extraction and reverse use `.toISOString()` (UTC), so values are
 * timezone-locked to UTC. Falls back to empty strings for missing fields.
 *
 * @param event - Calendar event from the API (ISO 8601 timestamps)
 * @returns Form-friendly representation with separated date/time
 */
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
