import { describe, it, expect } from 'vitest';
import { toApiEvent, fromApiEvent } from '#/lib/utils/calendar.ts';
import type { CalendarFormEvent } from '#/lib/utils/calendar.ts';
import type { IdentifiedCalendarEvent } from '#/types/calendar';

describe('toApiEvent', () => {
  it('converts date and time to ISO datetime strings', () => {
    const form: CalendarFormEvent = {
      title: 'Meeting',
      date: '2026-07-09',
      startTime: '10:00',
      endTime: '11:00',
    };

    const result = toApiEvent(form);

    expect(result.title).toBe('Meeting');
    expect(result.startTime).toBe('2026-07-09T10:00:00Z');
    expect(result.endTime).toBe('2026-07-09T11:00:00Z');
  });

  it('includes description when provided', () => {
    const form: CalendarFormEvent = {
      title: 'Meeting',
      date: '2026-07-09',
      startTime: '10:00',
      endTime: '11:00',
      description: 'Quarterly review',
    };

    const result = toApiEvent(form);

    expect(result.description).toBe('Quarterly review');
  });

  it('omits description when empty', () => {
    const form: CalendarFormEvent = {
      title: 'Meeting',
      date: '2026-07-09',
      startTime: '10:00',
      endTime: '11:00',
      description: '',
    };

    const result = toApiEvent(form);

    expect(result.description).toBeUndefined();
  });
});

describe('fromApiEvent', () => {
  it('extracts date and time from ISO datetime strings', () => {
    const apiEvent: IdentifiedCalendarEvent = {
      id: 1,
      title: 'Meeting',
      startTime: '2026-07-09T10:00:00Z',
      endTime: '2026-07-09T11:00:00Z',
      description: 'Quarterly review',
    };

    const result = fromApiEvent(apiEvent);

    expect(result.id).toBe(1);
    expect(result.title).toBe('Meeting');
    expect(result.date).toBe('2026-07-09');
    expect(result.startTime).toBe('10:00');
    expect(result.endTime).toBe('11:00');
    expect(result.description).toBe('Quarterly review');
  });

  it('handles missing optional fields', () => {
    const apiEvent: IdentifiedCalendarEvent = {
      id: 2,
      title: 'Test',
      startTime: '2026-07-09T14:00:00Z',
      endTime: '2026-07-09T15:00:00Z',
    };

    const result = fromApiEvent(apiEvent);

    expect(result.title).toBe('Test');
    expect(result.description).toBe('');
    expect(result.date).toBe('2026-07-09');
    expect(result.startTime).toBe('14:00');
    expect(result.endTime).toBe('15:00');
  });
});
